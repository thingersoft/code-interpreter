package it.aci.ai.mcp.servers.code_interpreter.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import it.aci.ai.mcp.servers.code_interpreter.exception.CodeInterpreterException;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import com.github.dockerjava.api.model.Container;

import it.aci.ai.mcp.servers.code_interpreter.config.AppConfig;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeRequest;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeResult;
import it.aci.ai.mcp.servers.code_interpreter.enums.Language;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFile;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFileType;
import it.aci.ai.mcp.servers.code_interpreter.services.DockerService.LoggingResultCallback;
import it.aci.ai.mcp.servers.code_interpreter.services.providers.LanguageProvider;
import it.aci.ai.mcp.servers.code_interpreter.utils.AppUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class CodeService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeService.class);

    private static final String INPUT_FOLDER_NAME = "input";

    private final Path localInputPath;

    private final String remoteIoPath;
    private final String remoteInputPath;
    private final String remoteOutputPath;

    private final String cdToInputCommand;

    private Map<Language, String> languageContainerIdMap = new EnumMap<>(Language.class);

    private final DockerService dockerService;
    private final FileService fileService;
    private final BuildProperties buildProperties;
    private final ApplicationContext applicationContext;

    public CodeService(AppConfig appConfig, FileService fileService, DockerService dockerService,
            BuildProperties buildProperties, ApplicationContext applicationContext) {
        this.fileService = fileService;
        this.dockerService = dockerService;
        this.buildProperties = buildProperties;
        this.applicationContext = applicationContext;

        this.remoteIoPath = appConfig.getRemoteIoPath();
        // Construct I/O paths for container execution
        this.remoteInputPath = this.remoteIoPath + "/" + INPUT_FOLDER_NAME;
        this.remoteOutputPath = this.remoteIoPath + "/output";
        this.localInputPath = Path.of(System.getProperty("java.io.tmpdir")).resolve("code-interpreter");
        this.cdToInputCommand = "cd " + this.remoteInputPath;
    }

    private LanguageProvider getLanguageProvider(Language language) {
        return applicationContext.getBean(language.getProvider());
    }

    @PostConstruct
    private void init() {

        // init containers for each supported language in parallel
        parallellyExecuteForEachLanguage(language -> {

            LanguageProvider languageProvider = getLanguageProvider(language);

            String imageName = language.name().toLowerCase() + "-sandbox:" + buildProperties.getVersion();

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
                        buildArgs.put("FROM_IMAGE", languageProvider.getFromImage());
                        buildArgs.put("USER", LanguageProvider.IMAGE_USER);
                        buildArgs.put("INPUT_PATH", remoteInputPath);
                        buildArgs.put("OUTPUT_PATH", remoteOutputPath);
                        buildArgs.put("INIT_COMMAND", String.join(" && ", languageProvider.getImageInitCommands()));

                        String imageId = dockerService.buildImage(tmpFile, imageName, buildArgs);
                        logInfo(language, "Built image with id " + imageId);
                    }
                }

                // create container from sandbox image
                logInfo(language, "Creating container");
                String containerId = dockerService.createContainer(imageName, getContainerName(language),
                        LanguageProvider.IMAGE_USER);

                // populate language-container map
                languageContainerIdMap.put(language, containerId);

                // container start
                logInfo(language, "Starting container");
                dockerService.startContainer(containerId, language.name());
                logInfo(language, "Container ready");

            } catch (IOException e) {
                throw new CodeInterpreterException("Failed to initialize container for language " + language, e);
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
        String sourceCode = request.code();
        Path workspaceFolder = localInputPath.resolve(UUID.randomUUID().toString()).resolve(INPUT_FOLDER_NAME);
        LanguageProvider languageProvider = getLanguageProvider(language);

        String sessionId = request.sessionId() == null ? AppUtils.generateSessionId() : request.sessionId();

        logInfo(language, "Session " + sessionId + " - executing code");

        try {

            String containerId = languageContainerIdMap.get(language);

            // clean local input folder
            FileSystemUtils.deleteRecursively(workspaceFolder);
            Files.createDirectories(workspaceFolder);

            // write source code and attachments to local input folder
            List<StoredFile> attachments = fileService.findUploadedFiles(sessionId);
            for (StoredFile storedFile : attachments) {
                Files.copy(fileService.getFilePath(storedFile), workspaceFolder.resolve(storedFile.relativePath()));
            }

            // prepare workspace
            languageProvider.prepareWorkspace(workspaceFolder, sourceCode);

            // copy local input folder to container
            dockerService.copyToContainer(containerId, workspaceFolder, remoteIoPath);

            // prepare execution environment
            List<String> prepareCommands = new ArrayList<>();
            prepareCommands.add(cdToInputCommand);
            prepareCommands.add("rm -rf " + remoteOutputPath + "/*");
            prepareCommands.addAll(languageProvider.getPrepareExecutionCommands(workspaceFolder));
            LoggingResultCallback prepareResult = dockerService.runInContainer(containerId, prepareCommands, true,
                    language.name());
            // return early for errors raised by prepare phase
            if (StringUtils.hasText(prepareResult.getStdErr())) {
                return new ExecuteCodeResult("", prepareResult.getStdErr(), sessionId,
                        List.of());
            }

            // execute code and collect output
            List<String> commands = new ArrayList<>();
            commands.add(cdToInputCommand);
            commands.addAll(languageProvider.getExecutionCommands(workspaceFolder));
            LoggingResultCallback result = dockerService.runInContainer(containerId, commands, false, null);

            // collect and store produced files
            List<StoredFile> outputFiles = saveOutput(sessionId, containerId);

            logInfo(language, "Session " + sessionId + " - code executed");

            return new ExecuteCodeResult(result.getStdOut(), result.getStdErr(), sessionId, outputFiles);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CodeInterpreterException("Code execution interrupted for request " + request, e);
        } catch (IOException e) {
            throw new CodeInterpreterException("Failed to execute code for request " + request, e);
        }

    }

    private void parallellyExecuteForEachLanguage(Consumer<Language> consumer) {
        List<Language> languages = Arrays.asList(Language.values());
        try (ForkJoinPool threadPool = new ForkJoinPool(languages.size())) {
            threadPool.submit(() -> languages.parallelStream().forEach(consumer)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CodeInterpreterException("Parallel execution interrupted", e);
        } catch (ExecutionException e) {
            throw new CodeInterpreterException("Parallel execution failed", e);
        }
    }

    private String getContainerName(Language language) {
        return "code_interpreter_sandbox___" + language.name().toLowerCase();
    }

    private List<StoredFile> saveOutput(String sessionId, String containerId) throws IOException {

        List<StoredFile> storedFiles = new ArrayList<>();

        // untar files preserving directory structure
        try (TarArchiveInputStream tais = dockerService.copyArchiveFromContainer(containerId, remoteOutputPath)) {
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

    private static void logInfo(Language language, String message) {
        logAtLevel(language, message, Level.INFO);
    }

    private static void logAtLevel(Language language, String message, Level level) {
        LOG.atLevel(level).log("[" + language + "] " + message);
    }

}
