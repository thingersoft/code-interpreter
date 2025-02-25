package it.aci.ai.mcp.servers.code_interpreter.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import it.aci.ai.mcp.servers.code_interpreter.config.AppConfig;
import it.aci.ai.mcp.servers.code_interpreter.config.DockerConfig;
import it.aci.ai.mcp.servers.code_interpreter.dto.Dependency;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeRequest;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeResult;
import it.aci.ai.mcp.servers.code_interpreter.dto.Language;
import it.aci.ai.mcp.servers.code_interpreter.dto.UploadedFile;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFile;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFileType;
import it.aci.ai.mcp.servers.code_interpreter.repositories.StoredFileRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class CodeService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeService.class);

    // Allow uppercase/lowercase letters, digits, dashes and unserscores
    private static final char[] UID_ALPHABET = Stream.of(
            IntStream.rangeClosed('A', 'Z'),
            IntStream.rangeClosed('a', 'z'),
            IntStream.rangeClosed('0', '9'))
            .flatMapToInt(s -> s)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .append('-')
            .append('_')
            .toString().toCharArray();
    private static final int UID_LENGTH = 21;
    private static final SecureRandom UID_RANDOM = new SecureRandom();

    private static final String INPUT_FOLDER_NAME = "input";

    private static final String STORAGE_FOLDER_NAME = "storage";
    private static final String UPLOAD_FOLDER_NAME = "upload";
    private static final String OUTPUT_FOLDER_NAME = "output";

    private static Path LOCAL_IO_PATH;
    private static Path LOCAL_INPUT_PATH;
    private static Path LOCAL_STORAGE_PATH;

    private static String REMOTE_IO_PATH;
    private static String REMOTE_INPUT_PATH;
    private static String REMOTE_OUTPUT_PATH;

    private static String CD_TO_INPUT_COMMAND;

    private DockerClient dockerClient;
    private Map<Language, String> languageContainerIdMap = new HashMap<>();

    private final DockerConfig dockerConfig;
    private final StoredFileRepository storedFileRepository;

    public CodeService(DockerConfig dockerConfig, AppConfig appConfig, StoredFileRepository storedFileRepository) {
        this.storedFileRepository = storedFileRepository;
        this.dockerConfig = dockerConfig;

        LOCAL_IO_PATH = appConfig.getLocalIoPath();
        LOCAL_INPUT_PATH = LOCAL_IO_PATH.resolve(INPUT_FOLDER_NAME);
        LOCAL_STORAGE_PATH = LOCAL_IO_PATH.resolve(STORAGE_FOLDER_NAME);

        REMOTE_IO_PATH = appConfig.getRemoteIoPath();
        REMOTE_INPUT_PATH = REMOTE_IO_PATH + "/" + INPUT_FOLDER_NAME;
        REMOTE_OUTPUT_PATH = REMOTE_IO_PATH + "/" + OUTPUT_FOLDER_NAME;

        CD_TO_INPUT_COMMAND = "cd " + REMOTE_INPUT_PATH;
    }

    @PostConstruct
    private void init() throws InterruptedException, IOException {

        // init docker client
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerConfig.getHost())
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();
        dockerClient = DockerClientImpl.getInstance(config, httpClient);

        // pull images and init containers for each supported language in parallel
        parallellyExecuteForEachLanguage(language -> {

            try {

                // create I/O directories
                Files.createDirectories(LOCAL_INPUT_PATH);

                // pull image
                logInfo(language, "Pulling image");
                dockerClient
                        .pullImageCmd(language.getImage())
                        .exec(new ResultCallback.Adapter<PullResponseItem>())
                        .awaitCompletion();

                // stop and remove container if already exists
                logInfo(language, "Stopping and removing existing container");
                List<Container> matchingContainers = dockerClient
                        .listContainersCmd()
                        .withShowAll(true)
                        .withNameFilter(List.of(getContainerName(language)))
                        .exec();
                for (Container container : matchingContainers) {
                    cleanContainer(container.getId());
                }

                // create container with never ending command
                logInfo(language, "Creating container");
                CreateContainerResponse createContainerResponse = dockerClient
                        .createContainerCmd(language.getImage())
                        .withName(getContainerName(language))
                        .withCmd("sh", "-c", "while true; do sleep 1; done")
                        .withAttachStdout(true)
                        .withAttachStderr(true)
                        .withUser(language.getUser())
                        .exec();
                String containerId = createContainerResponse.getId();

                // populate language-container map
                languageContainerIdMap.put(language, containerId);

                // container start
                logInfo(language, "Starting container");
                dockerClient
                        .startContainerCmd(containerId)
                        .exec();

                // log stdout and stderr
                dockerClient
                        .logContainerCmd(containerId)
                        .withStdErr(true)
                        .withStdOut(true)
                        .withFollowStream(true)
                        .exec(new LoggingResultCallback(language));

                // init execution environment
                logInfo(language, "Init execution environment");
                List<String> initEnvCommands = new ArrayList<>();
                initEnvCommands.addAll(getSetEnvVariablesCommands(Map.of("PIP_DISABLE_PIP_VERSION_CHECK", "1")));
                initEnvCommands.add("mkdir -p " + REMOTE_INPUT_PATH + " " + REMOTE_OUTPUT_PATH);
                initEnvCommands.add(CD_TO_INPUT_COMMAND);
                switch (language) {
                    case PYTHON:
                        initEnvCommands.add("pip install pipreqs");
                        break;
                    case TYPESCRIPT:
                        break;
                    case JAVA:
                        break;
                }
                runInContainer(containerId, new LoggingResultCallback(language), initEnvCommands);

                logInfo(language, "Container ready");

            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @PreDestroy
    private void clean() {
        parallellyExecuteForEachLanguage(language -> {
            logInfo(language, "Stopping and removing container");
            String containerId = languageContainerIdMap.get(language);
            cleanContainer(containerId);
        });
    }

    @Transactional
    public synchronized ExecuteCodeResult executeCode(ExecuteCodeRequest request) {

        Language language = request.language();
        String sessionId = request.sessionId() == null ? generateSessionId() : request.sessionId();

        logInfo(language, "Session " + sessionId + " - executing code");

        try {

            String sourceFilename = "source" + language.getSourceFileExtension();
            String containerId = languageContainerIdMap.get(language);

            // prepare I/O folders
            Path sessionOutputPath = getSessionOutputStoragePath(sessionId);
            FileSystemUtils.deleteRecursively(LOCAL_INPUT_PATH);
            Files.createDirectories(LOCAL_INPUT_PATH);
            Files.createDirectories(sessionOutputPath);

            // write source code to input file and copy attachments
            Files.writeString(LOCAL_INPUT_PATH.resolve(sourceFilename), request.code(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            List<StoredFile> attachments = storedFileRepository.findBySessionIdAndType(sessionId, StoredFileType.INPUT);
            for (StoredFile storedFile : attachments) {
                Files.copy(getFilePath(storedFile), LOCAL_INPUT_PATH.resolve(storedFile.filename()));
            }
            copyToContainer(containerId, LOCAL_INPUT_PATH, REMOTE_IO_PATH);

            // prepare execution environment
            List<Dependency> dependencies = Arrays.asList(request.dependencies());
            List<String> prepareCommands = new ArrayList<>();
            prepareCommands.add(CD_TO_INPUT_COMMAND);
            prepareCommands.add("rm -rf " + REMOTE_OUTPUT_PATH + "/*");
            switch (language) {
                case PYTHON:
                    prepareCommands.add("pipreqs --scan-notebooks --force .");
                    prepareCommands.add("pip install -r requirements.txt");
                    break;
                case TYPESCRIPT:
                    if (!dependencies.isEmpty()) {
                        prepareCommands.add("npm install " + dependencies.stream()
                                .map(dep -> dep.id() + (dep.version() != null ? "@" + dep.version() : ""))
                                .reduce("", (partial, current) -> partial + " " + current));
                    }
                    break;
                case JAVA:
                    break;
            }
            runInContainer(containerId, new LoggingResultCallback(language), prepareCommands);

            // execute code and collect output
            List<String> commands = new ArrayList<>();
            commands.add(CD_TO_INPUT_COMMAND);
            switch (language) {
                case PYTHON:
                    commands.add("python " + sourceFilename);
                    break;
                case TYPESCRIPT:
                    commands.add("node " + sourceFilename);
                    break;
                case JAVA:
                    commands.add("java " + sourceFilename);
                    break;
            }
            CollectingResultCallback collectingResultCallback = new CollectingResultCallback();
            runInContainer(containerId, collectingResultCallback, commands);

            // collect and store produced files
            List<StoredFile> outputFiles = copyFromContainer(sessionId, containerId, REMOTE_OUTPUT_PATH,
                    sessionOutputPath);

            logInfo(language, "Session " + sessionId + " - code executed");

            try (Stream<Path> stream = Files.list(sessionOutputPath)) {
                return new ExecuteCodeResult(collectingResultCallback.getStdOut(),
                        collectingResultCallback.getStdErr(), sessionId, outputFiles);
            }

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    public List<StoredFile> findUploadedFiles(String sessionId) {
        return storedFileRepository.findBySessionId(sessionId);
    }

    @Transactional
    public void deleteFile(String fileId) {
        StoredFile storedFile = storedFileRepository.findById(fileId).orElseThrow();
        try {
            Files.delete(getFilePath(storedFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        storedFileRepository.delete(storedFile);
        LOG.info("Deleted file - session id: " + storedFile.sessionId() + " - file id: " + fileId);
    }

    @Transactional
    public List<StoredFile> uploadFile(List<UploadedFile> uploadedFiles) {
        String sessionId = generateSessionId();
        Path uploadStoragePath = getUploadStoragePath(sessionId);
        List<StoredFile> storedFiles = new ArrayList<>();
        for (UploadedFile uploadedFile : uploadedFiles) {
            String filename = uploadedFile.name();
            byte[] fileContent = uploadedFile.content();
            try {
                Files.createDirectories(uploadStoragePath);
                Path filePath = Files.write(uploadStoragePath.resolve(filename), fileContent);
                StoredFile storedFile = new StoredFile(generateFileId(), sessionId, filename, filename,
                        Instant.now(), fileContent.length, Files.probeContentType(filePath), StoredFileType.INPUT);
                storedFiles.add(storedFileRepository.save(storedFile));
                LOG.info("Uploaded file - session id: " + storedFile.sessionId() + " - file id: " + storedFile.id());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return storedFiles;
    }

    public byte[] downloadFile(String fileId) {
        StoredFile storedFile = storedFileRepository.findById(fileId).orElseThrow();
        Path filePath = getFilePath(storedFile);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getFilePath(StoredFile storedFile) {
        String sessionId = storedFile.sessionId();
        Path basePath = StoredFileType.OUTPUT.equals(storedFile.type()) ? getSessionOutputStoragePath(sessionId)
                : getUploadStoragePath(storedFile.sessionId());
        Path filePath = basePath.resolve(storedFile.relativePath());
        return filePath;
    }

    /**
     * Callback to log container stdout and stderr
     */
    private class LoggingResultCallback extends ResultCallback.Adapter<Frame> {

        private Language language;

        public LoggingResultCallback(Language language) {
            this.language = language;
        }

        @Override
        public void onNext(Frame frame) {

            Level logLevel = switch (frame.getStreamType()) {
                case STDOUT:
                case RAW:
                    yield Level.TRACE;
                case STDERR:
                    yield Level.ERROR;
                default:
                    throw new RuntimeException("unknown stream type:" + frame.getStreamType());
            };
            LOG.atLevel(logLevel).log("[" + language + "] " + frameToString(frame));
        }

    }

    /**
     * Callback to collect stdout and stderr
     */
    private class CollectingResultCallback extends ResultCallback.Adapter<Frame> {

        private List<String> outputFrames = new ArrayList<>();
        private List<String> errorFrames = new ArrayList<>();

        public void onNext(Frame frame) {

            switch (frame.getStreamType()) {
                case STDOUT:
                    outputFrames.add(frameToString(frame));
                    break;
                case STDERR:
                    errorFrames.add(frameToString(frame));
                    break;
                default:
                    break;
            }

        }

        public String getStdOut() {
            return String.join("", outputFrames);
        }

        public String getStdErr() {
            return String.join("", errorFrames);
        }

    }

    private void parallellyExecuteForEachLanguage(Consumer<Language> consumer) {
        List<Language> languages = Arrays.asList(Language.values());
        ForkJoinPool threadPool = new ForkJoinPool(languages.size());
        try {
            threadPool.submit(() -> {
                languages.parallelStream().forEach(consumer);
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    }

    private String getContainerName(Language language) {
        return "code_interpreter_sandbox___" + language.name().toLowerCase();
    }

    private String frameToString(Frame frame) {
        return new String(frame.getPayload(), StandardCharsets.UTF_8);
    };

    private void cleanContainer(String containerId) {
        Container container = dockerClient
                .listContainersCmd()
                .withShowAll(true)
                .withIdFilter(List.of(containerId))
                .exec()
                .get(0);
        if (container.getState().equalsIgnoreCase("running")) {
            dockerClient.stopContainerCmd(containerId).exec();
        }
        dockerClient.removeContainerCmd(containerId).exec();

    }

    private void runInContainer(String containerId, ResultCallback.Adapter<Frame> resultCallback, List<String> commands)
            throws InterruptedException {
        // prepend init and build command
        commands.add(0, "export PATH=\"$HOME/.local/bin:$PATH\"");
        ExecCreateCmdResponse createPrepareCommandResponse = dockerClient
                .execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("sh", "-l", "-c", String.join(" && ", commands))
                .exec();

        // run command
        dockerClient
                .execStartCmd(createPrepareCommandResponse.getId())
                .exec(resultCallback)
                .awaitCompletion();
    }

    private void copyToContainer(String containerId, Path localPath, String remotePath) throws IOException {
        dockerClient
                .copyArchiveToContainerCmd(containerId)
                .withHostResource(localPath.toString())
                .withRemotePath(remotePath)
                .withCopyUIDGID(true)
                .exec();
    }

    private List<StoredFile> copyFromContainer(String sessionId, String containerId, String remotePath, Path localPath)
            throws IOException {

        List<StoredFile> storedFiles = new ArrayList<>();

        File destDir = localPath.toFile();
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        InputStream is = dockerClient
                .copyArchiveFromContainerCmd(containerId, remotePath)
                .exec();

        // untar files preserving directory structure
        try (TarArchiveInputStream tais = new TarArchiveInputStream(is)) {
            TarArchiveEntry entry;
            String rootDirName = null;

            while ((entry = tais.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    if (rootDirName == null) {
                        rootDirName = entry.getName().split("/")[0]; // Capture the root directory name
                    }
                    continue;
                }

                String entryName = entry.getName();
                if (rootDirName != null && entryName.startsWith(rootDirName + "/")) {
                    entryName = entryName.substring(rootDirName.length() + 1); // Remove root dir prefix
                }

                File outputFile = new File(destDir, entryName);
                outputFile.getParentFile().mkdirs(); // Ensure parent directories exist

                // write to file system and database
                try (OutputStream fos = new FileOutputStream(outputFile)) {
                    int fileSize = IOUtils.copy(tais, fos);
                    StoredFile storedFile = new StoredFile(
                            generateFileSystemUid(null),
                            sessionId,
                            outputFile.getName(),
                            entryName,
                            Instant.now(),
                            fileSize,
                            Files.probeContentType(outputFile.toPath()),
                            StoredFileType.OUTPUT);

                    storedFiles.add(storedFileRepository.save(storedFile));
                }

            }
        }

        return storedFiles;
    }

    private List<String> getSetEnvVariablesCommands(Map<String, String> envVariables) {
        List<String> setEnvVariablesCommands = envVariables.entrySet().stream()
                .map(entry -> "echo 'export " + entry.getKey() + "=\"" + entry.getValue() + "\"' >> ~/.profile")
                .collect(Collectors.toCollection(ArrayList::new));
        setEnvVariablesCommands.add(". ~/.profile");
        return setEnvVariablesCommands;
    }

    private Path getUploadStoragePath(String sessionId) {
        return getSessionStoragePath(sessionId).resolve(UPLOAD_FOLDER_NAME);
    }

    private Path getSessionOutputStoragePath(String sessionId) {
        return getSessionStoragePath(sessionId).resolve(OUTPUT_FOLDER_NAME);
    }

    private Path getSessionStoragePath(String sessionId) {
        return LOCAL_STORAGE_PATH.resolve(sessionId);
    }

    private String generateSessionId() {
        return generateFileSystemUid("sess-");
    }

    private String generateFileId() {
        return generateFileSystemUid(null);
    }

    private String generateFileSystemUid(String prefix) {
        int uidLength = UID_LENGTH;
        if (prefix == null) {
            prefix = "";
        }
        uidLength = uidLength - prefix.length();
        return prefix + NanoIdUtils.randomNanoId(UID_RANDOM, UID_ALPHABET, uidLength);
    }

    private static void logInfo(Language language, String message) {
        LOG.info("[" + language + "] " + message);
    }

}
