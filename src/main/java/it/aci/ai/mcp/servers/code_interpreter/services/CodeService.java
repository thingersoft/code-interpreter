package it.aci.ai.mcp.servers.code_interpreter.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.SSLConfig;

import it.aci.ai.mcp.servers.code_interpreter.config.AppConfig;
import it.aci.ai.mcp.servers.code_interpreter.config.DockerConfig;
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

    private DockerClient dockerClient;
    private Map<Language, String> languageContainerIdMap = new HashMap<>();

    private final DockerConfig dockerConfig;
    private final FileService fileService;
    private final BuildProperties buildProperties;

    public CodeService(DockerConfig dockerConfig, AppConfig appConfig, FileService fileService,
            BuildProperties buildProperties) {
        this.fileService = fileService;
        this.dockerConfig = dockerConfig;
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

        // init docker client
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerConfig.getHost())
                .build();
        ApacheDockerHttpClient.Builder builder = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost());
        if (dockerConfig.isTls()) {
            try {
                SSLContext sslContext = createSSLContext(dockerConfig.getCaCert(), dockerConfig.getClientCert(),
                        dockerConfig.getClientKey());
                SSLConfig sslConfig = new SSLConfig() {
                    @Override
                    public SSLContext getSSLContext() {
                        return sslContext;
                    }
                };
                builder = builder.sslConfig(sslConfig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        dockerClient = DockerClientImpl.getInstance(config, builder.build());

        // init containers for each supported language in parallel
        parallellyExecuteForEachLanguage(language -> {

            String imageName = language.name().toLowerCase() + "-sandbox:" + buildProperties.getVersion();
            String imageUser = "intepreter";

            try {

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

                try {
                    // check if a sandbox image produced by the current software version exists
                    dockerClient
                            .inspectImageCmd(imageName)
                            .exec();
                    logInfo(language, "Image found");
                } catch (NotFoundException e) {
                    // sandbox image doesn't exist, build it
                    logInfo(language, "Image not found, building");
                    try (InputStream is = this.getClass().getResourceAsStream("/DockerFile.sandbox")) {
                        Path tmpDir = Files.createTempDirectory("DockerFile-" + language);
                        File tmpFile = tmpDir.resolve("DockerFile").toFile();
                        try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                            is.transferTo(fos);
                        }

                        BuildImageCmd buildImageCmd = dockerClient
                                .buildImageCmd(tmpFile)
                                .withTags(Set.of(imageName))
                                .withBuildArg("FROM_IMAGE", language.getBaseImage())
                                .withBuildArg("USER", imageUser)
                                .withBuildArg("INPUT_PATH", REMOTE_INPUT_PATH)
                                .withBuildArg("OUTPUT_PATH", REMOTE_OUTPUT_PATH);

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

                        buildImageCmd.withBuildArg("INIT_COMMAND", String.join(" && ", initCommands));

                        BuildSandboxImageResultCallback buildImageResultCallback = new BuildSandboxImageResultCallback();
                        buildImageCmd.exec(buildImageResultCallback);
                        String imageId = buildImageResultCallback.awaitImageId();
                        logInfo(language, "Built image with id " + imageId);
                        logAtLevel(language,
                                "Image build log: " + System.lineSeparator()
                                        + String.join(System.lineSeparator(),
                                                buildImageResultCallback.getBuildImageSteps()),
                                Level.TRACE);
                    }
                }

                // create container from sandbox image
                logInfo(language, "Creating container");
                CreateContainerResponse createContainerResponse = dockerClient
                        .createContainerCmd(imageName)
                        .withName(getContainerName(language))
                        .withAttachStdout(true)
                        .withAttachStderr(true)
                        .withUser(imageUser)
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
            cleanContainer(containerId);
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
                    commands.add("node --no-warnings " + sourceFilename);
                    break;
                case JAVA:
                    commands.add("java " + sourceFilename);
                    break;
            }
            CollectingResultCallback collectingResultCallback = new CollectingResultCallback();
            runInContainer(containerId, collectingResultCallback, commands);

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
     * Callback to collect image build log
     */
    private class BuildSandboxImageResultCallback extends BuildImageResultCallback {

        private List<String> buildImageSteps = new ArrayList<>();

        public List<String> getBuildImageSteps() {
            return buildImageSteps;
        }

        @Override
        public void onNext(BuildResponseItem item) {
            super.onNext(item);
            String itemStream = item.getStream();
            if (StringUtils.hasText(itemStream)) {
                itemStream = itemStream.replaceAll("[\r\n]+$", ""); // Remove trailing newlines
                buildImageSteps.add(itemStream);
            }
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

    private List<StoredFile> saveOutput(String sessionId, String containerId) throws IOException {

        List<StoredFile> storedFiles = new ArrayList<>();

        InputStream is = dockerClient
                .copyArchiveFromContainerCmd(containerId, REMOTE_OUTPUT_PATH)
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

    private static SSLContext createSSLContext(String caCert, String clientCert, String clientKey) throws Exception {
        // Convert PEM strings to byte arrays
        byte[] caBytes = caCert.getBytes(StandardCharsets.UTF_8);
        byte[] certBytes = clientCert.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = decodePemPrivateKey(clientKey);

        // Load CA Certificate
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        Certificate caCertificate = certFactory.generateCertificate(new ByteArrayInputStream(caBytes));

        // Load Client Certificate
        Certificate clientCertificate = certFactory.generateCertificate(new ByteArrayInputStream(certBytes));

        // Load Private Key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        // Create a KeyStore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", caCertificate);
        keyStore.setKeyEntry("client", privateKey, "changeit".toCharArray(), new Certificate[] { clientCertificate });

        // Create an SSLContext
        return SSLContextBuilder.create()
                .loadTrustMaterial(keyStore, null)
                .loadKeyMaterial(keyStore, "changeit".toCharArray())
                .build();
    }

    private static byte[] decodePemPrivateKey(String pem) {
        String key = pem.replaceAll("-----.*?-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(key);
    }

}
