package it.aci.ai.mcp.servers.code_interpreter.services;

import java.io.IOException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import it.aci.ai.mcp.servers.code_interpreter.config.DockerConfig;
import it.aci.ai.mcp.servers.code_interpreter.dto.Dependency;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeRequest;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeResult;
import it.aci.ai.mcp.servers.code_interpreter.dto.Language;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class CodeService {

    Logger LOG = LoggerFactory.getLogger(CodeService.class);

    private static final String OUTPUT_PATH = "/output/";

    private DockerClient dockerClient;
    private Map<Language, String> languageContainerIdMap = new HashMap<>();

    private final DockerConfig dockerConfig;

    public CodeService(DockerConfig dockerConfig) {
        this.dockerConfig = dockerConfig;
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
            final String INPUT_PATH = getInputPath(language);

            try {

                // create I/O directories
                Files.createDirectories(Path.of(OUTPUT_PATH));
                Files.createDirectories(Path.of(INPUT_PATH));

                // pull image
                dockerClient
                        .pullImageCmd(language.getImage())
                        .exec(new ResultCallback.Adapter<PullResponseItem>())
                        .awaitCompletion();

                // I/O volumes bindings
                HostConfig hostConfig = new HostConfig();
                Bind inputBind = new Bind("/c" + INPUT_PATH, new Volume(INPUT_PATH));
                Bind outputBind = new Bind("/c" + OUTPUT_PATH, new Volume(OUTPUT_PATH));
                hostConfig.setBinds(inputBind, outputBind);

                // stop and remove container if already exists
                List<Container> matchingContainers = dockerClient
                        .listContainersCmd()
                        .withNameFilter(List.of(getContainerName(language)))
                        .exec();
                for (Container container : matchingContainers) {
                    cleanContainer(container.getId());
                }

                // create container with prepair and never ending command
                String prepareCommand = "cd " + INPUT_PATH + " && export PATH=\"$HOME/.local/bin:$PATH\" && ";
                switch (language) {
                    case PYTHON:
                        prepareCommand += "pip install pipreqs && ";
                        break;
                    case TYPESCRIPT:
                        break;
                    case JAVA:
                        break;
                }
                CreateContainerResponse createContainerResponse = dockerClient
                        .createContainerCmd(language.getImage())
                        .withName(getContainerName(language))
                        .withCmd("sh", "-c", prepareCommand + "while true; do sleep 1; done")
                        .withHostConfig(hostConfig)
                        .withAttachStdout(true)
                        .withAttachStderr(true)
                        .exec();
                String containerId = createContainerResponse.getId();

                // populate language-container map
                languageContainerIdMap.put(language, containerId);

                // container start
                dockerClient
                        .startContainerCmd(containerId)
                        .exec();

                // log stdout and stderr
                dockerClient
                        .logContainerCmd(containerId)
                        .withStdErr(true)
                        .withStdOut(true)
                        .withFollowStream(true)
                        .exec(new LoggingResultCallback());

            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @PreDestroy
    private void clean() {
        parallellyExecuteForEachLanguage(language -> {
            String containerId = languageContainerIdMap.get(language);
            cleanContainer(containerId);
        });
    }

    public ExecuteCodeResult executeCode(ExecuteCodeRequest request) {

        Language language = request.language();

        try {

            final String INPUT_PATH = getInputPath(language);
            final String SOURCE_FILENAME = "source" + language.getSourceFileExtension();
            String containerId = languageContainerIdMap.get(language);

            // write source code to input file
            Files.writeString(Path.of(INPUT_PATH, SOURCE_FILENAME), request.code(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            // build prepare command
            List<Dependency> dependencies = Arrays.asList(request.dependencies());
            String prepareCommand = "cd " + INPUT_PATH + " && export PATH=\"$HOME/.local/bin:$PATH\"";
            switch (language) {
                case PYTHON:
                    prepareCommand += " && pipreqs --force .";
                    prepareCommand += " && pip install -r requirements.txt";
                    break;
                case TYPESCRIPT:
                    if (!dependencies.isEmpty()) {
                        prepareCommand = "npm install ";
                        prepareCommand += dependencies.stream()
                                .map(dep -> dep.id() + (dep.version() != null ? "@" + dep.version() : ""))
                                .reduce("", (partial, current) -> partial + " " + current);
                    }
                    break;
                case JAVA:
                    break;
            }

            // create prepare command
            ExecCreateCmdResponse createPrepareCommandResponse = dockerClient
                    .execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd("sh", "-c", prepareCommand)
                    .exec();

            // run prepare command
            dockerClient
                    .execStartCmd(createPrepareCommandResponse.getId())
                    .exec(new LoggingResultCallback())
                    .awaitCompletion();

            // build command
            String command = "cd " + INPUT_PATH + " && export PATH=\"$HOME/.local/bin:$PATH\" && ";
            switch (language) {
                case PYTHON:
                    command += "python " + SOURCE_FILENAME;
                    break;
                case TYPESCRIPT:
                    command += "node " + SOURCE_FILENAME;
                    break;
                case JAVA:
                    command += "java " + SOURCE_FILENAME;
                    break;
            }

            // create command
            ExecCreateCmdResponse createcommandResponse = dockerClient
                    .execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd("sh", "-c", command)
                    .exec();

            // run command and collect output
            List<String> outputFrames = new ArrayList<>();
            List<String> errorFrames = new ArrayList<>();
            dockerClient
                    .execStartCmd(createcommandResponse.getId())
                    .exec(new ResultCallback.Adapter<Frame>() {
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

                    })
                    .awaitCompletion();

            return new ExecuteCodeResult(String.join("", outputFrames), String.join("", errorFrames));

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Callback to log container stdout and stderr
     */
    private class LoggingResultCallback extends ResultCallback.Adapter<Frame> {

        @Override
        public void onNext(Frame frame) {

            Level logLevel = switch (frame.getStreamType()) {
                case STDOUT:
                case RAW:
                    yield Level.INFO;
                case STDERR:
                    yield Level.ERROR;
                default:
                    throw new RuntimeException("unknown stream type:" + frame.getStreamType());
            };
            LOG.atLevel(logLevel).log(frameToString(frame));
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

    private String getInputPath(Language language) {
        return "/input/" + language;
    }

    private String getContainerName(Language language) {
        return "code_interpreter_sandbox___" + language.name().toLowerCase();
    }

    private String frameToString(Frame frame) {
        return new String(frame.getPayload(), StandardCharsets.UTF_8);
    };

    private void cleanContainer(String containerId) {
        dockerClient.stopContainerCmd(containerId).exec();
        dockerClient.removeContainerCmd(containerId).exec();
    }

}
