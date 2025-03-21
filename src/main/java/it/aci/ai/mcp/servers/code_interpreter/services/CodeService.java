package it.aci.ai.mcp.servers.code_interpreter.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;

import it.aci.ai.mcp.servers.code_interpreter.config.AppConfig;
import it.aci.ai.mcp.servers.code_interpreter.dto.Dependency;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeRequest;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeResult;
import it.aci.ai.mcp.servers.code_interpreter.dto.Language;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFile;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFileType;
import it.aci.ai.mcp.servers.code_interpreter.utils.AppUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class CodeService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeService.class);

    private static final String INPUT_FOLDER_NAME = "input";

    private static Path LOCAL_INPUT_PATH;

    private static String REMOTE_IO_PATH;
    private static String REMOTE_INPUT_PATH;
    private static String REMOTE_OUTPUT_PATH;

    private static String CD_TO_INPUT_COMMAND;

    private Map<Language, String> languageContainerIdMap = new HashMap<>();

    private final DockerService dockerService;
    private final FileService fileService;
    private final BuildProperties buildProperties;

    public CodeService(AppConfig appConfig, FileService fileService, DockerService dockerService,
            BuildProperties buildProperties) {
        this.fileService = fileService;
        this.dockerService = dockerService;
        this.buildProperties = buildProperties;

        REMOTE_IO_PATH = appConfig.getRemoteIoPath();
        REMOTE_INPUT_PATH = REMOTE_IO_PATH + "/" + INPUT_FOLDER_NAME;
        LOCAL_INPUT_PATH = Path.of(System.getProperty("java.io.tmpdir")).resolve("code-interpreter")
                .resolve(INPUT_FOLDER_NAME);
        REMOTE_OUTPUT_PATH = REMOTE_IO_PATH + "/output";

        CD_TO_INPUT_COMMAND = "cd " + REMOTE_INPUT_PATH;
    }

    @PostConstruct
    private void init() throws InterruptedException, IOException {

        // init containers for each supported language in parallel
        parallellyExecuteForEachLanguage(language -> {

            String imageName = language.name().toLowerCase() + "-sandbox:" + buildProperties.getVersion();
            String imageUser = "intepreter";

            try {

                // stop and remove container if already exists
                logInfo(language, "Stopping and removing existing container");
                List<Container> matchingContainers = dockerService.findContainersByName(getContainerName(language));
                for (Container container : matchingContainers) {
                    dockerService.cleanContainer(container.getId());
                }

                // check if a sandbox image produced by the current software version exists
                if (dockerService.imageExists(imageName)) {
                    logInfo(language, "Image found");
                } else {
                    // sandbox image doesn't exist, build it
                    logInfo(language, "Image not found, building");
                    try (InputStream is = this.getClass().getResourceAsStream("/DockerFile.sandbox")) {
                        Path tmpDir = Files.createTempDirectory("DockerFile-" + language);
                        File tmpFile = tmpDir.resolve("DockerFile").toFile();
                        try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                            is.transferTo(fos);
                        }

                        Map<String, String> buildArgs = new HashMap<>();
                        buildArgs.put("FROM_IMAGE", language.getBaseImage());
                        buildArgs.put("USER", imageUser);
                        buildArgs.put("INPUT_PATH", REMOTE_INPUT_PATH);
                        buildArgs.put("OUTPUT_PATH", REMOTE_OUTPUT_PATH);

                        List<String> initCommands = new ArrayList<>();
                        switch (language) {
                            case PYTHON:
                                initCommands.addAll(
                                        getSetEnvVariablesCommands(Map.of("PIP_DISABLE_PIP_VERSION_CHECK", "1")));
                                initCommands.add("pip install pipreqs");
                                break;

                            default:
                                break;
                        }
                        buildArgs.put("INIT_COMMAND", String.join(" && ", initCommands));

                        String imageId = dockerService.buildImage(tmpFile, imageName, buildArgs);
                        logInfo(language, "Built image with id " + imageId);
                    }
                }

                // create container from sandbox image
                logInfo(language, "Creating container");
                String containerId = dockerService.createContainer(imageName, getContainerName(language), imageUser);

                // populate language-container map
                languageContainerIdMap.put(language, containerId);

                // container start
                logInfo(language, "Starting container");
                dockerService.startContainer(containerId, new LoggingResultCallback(language));
                logInfo(language, "Container ready");

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @PreDestroy
    private void clean() {
        parallellyExecuteForEachLanguage(language -> {
            logInfo(language, "Stopping and removing container");
            String containerId = languageContainerIdMap.get(language);
            dockerService.cleanContainer(containerId);
        });
    }

    @Transactional
    public synchronized ExecuteCodeResult executeCode(ExecuteCodeRequest request) {

        Language language = request.language();
        String sessionId = request.sessionId() == null ? AppUtils.generateSessionId() : request.sessionId();

        logInfo(language, "Session " + sessionId + " - executing code");

        try {

            String sourceFilename = "source" + language.getSourceFileExtension();
            String containerId = languageContainerIdMap.get(language);

            // prepare temp input folder
            FileSystemUtils.deleteRecursively(LOCAL_INPUT_PATH);
            Files.createDirectories(LOCAL_INPUT_PATH);

            // write source code to input file and copy attachments
            Files.writeString(LOCAL_INPUT_PATH.resolve(sourceFilename), request.code(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            List<StoredFile> attachments = fileService.findUploadedFiles(sessionId);
            for (StoredFile storedFile : attachments) {
                Files.copy(fileService.getFilePath(storedFile), LOCAL_INPUT_PATH.resolve(storedFile.relativePath()));
            }
            dockerService.copyToContainer(containerId, LOCAL_INPUT_PATH, REMOTE_IO_PATH);

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
            dockerService.runInContainer(containerId, new LoggingResultCallback(language), prepareCommands);

            // execute code and collect output
            List<String> commands = new ArrayList<>();
            commands.add(CD_TO_INPUT_COMMAND);
            switch (language) {
                case PYTHON:
                    commands.add("python " + sourceFilename);
                    break;
                case TYPESCRIPT:
                    commands.add("node --no-warnings " + sourceFilename);
                    break;
                case JAVA:
                    commands.add("java " + sourceFilename);
                    break;
            }
            CollectingResultCallback collectingResultCallback = new CollectingResultCallback();
            dockerService.runInContainer(containerId, collectingResultCallback, commands);

            // collect and store produced files
            List<StoredFile> outputFiles = saveOutput(sessionId, containerId);

            logInfo(language, "Session " + sessionId + " - code executed");

            return new ExecuteCodeResult(collectingResultCallback.getStdOut(), collectingResultCallback.getStdErr(),
                    sessionId, outputFiles);

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

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
        try (ForkJoinPool threadPool = new ForkJoinPool(languages.size())) {
            threadPool.submit(() -> {
                languages.parallelStream().forEach(consumer);
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private String getContainerName(Language language) {
        return "code_interpreter_sandbox___" + language.name().toLowerCase();
    }

    private String frameToString(Frame frame) {
        return new String(frame.getPayload(), StandardCharsets.UTF_8);
    };

    private List<StoredFile> saveOutput(String sessionId, String containerId) throws IOException {

        List<StoredFile> storedFiles = new ArrayList<>();

        // untar files preserving directory structure
        try (TarArchiveInputStream tais = dockerService.copyArchiveFromContainer(containerId, REMOTE_OUTPUT_PATH)) {
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

                // write file to file system and database
                byte[] fileContent = tais.readNBytes((int) entry.getSize());
                StoredFile storedFile = fileService.storeFile(entryName, fileContent, sessionId, StoredFileType.OUTPUT);
                storedFiles.add(storedFile);

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

    private static void logInfo(Language language, String message) {
        logAtLevel(language, message, Level.INFO);
    }

    private static void logAtLevel(Language language, String message, Level level) {
        LOG.atLevel(level).log("[" + language + "] " + message);
    }

}
