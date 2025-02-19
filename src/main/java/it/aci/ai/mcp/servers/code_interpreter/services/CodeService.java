package it.aci.ai.mcp.servers.code_interpreter.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
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

@Service
public class CodeService {

    private DockerClient dockerClient;

    private final DockerConfig dockerConfig;

    public CodeService(DockerConfig dockerConfig) {
        this.dockerConfig = dockerConfig;
    }

    @PostConstruct
    private void init() throws InterruptedException {

        // init docker client
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerConfig.getHost())
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();
        dockerClient = DockerClientImpl.getInstance(config, httpClient);

        // pull images for each supported language
        for (Language language : Language.values()) {
            dockerClient
                    .pullImageCmd(language.getImage())
                    .exec(new ResultCallback.Adapter<PullResponseItem>())
                    .awaitCompletion();
        }

    }

    public ExecuteCodeResult executeCode(ExecuteCodeRequest request) {

        Language language = request.language();

        try {

            String OUTPUT_PATH = "/output/";
            String INPUT_PATH = "/input/" + language;
            String SOURCE_FILENAME = "source" + language.getSourceFileExtension();

            // build run command
            List<Dependency> dependencies = Arrays.asList(request.dependencies());
            String command = "export PATH=\"$HOME/.local/bin:$PATH\" && ";
            switch (language) {
                case PYTHON:
                    command += "pip install pipreqs && ";
                    command += "pipreqs . && ";
                    command += "pip install -r requirements.txt && ";
                    command += "python " + SOURCE_FILENAME;
                    break;
                case TYPESCRIPT:
                    if (!dependencies.isEmpty()) {
                        command = "npm install ";
                        command += dependencies.stream()
                                .map(dep -> dep.id() + (dep.version() != null ? "@" + dep.version() : ""))
                                .reduce("", (partial, current) -> partial + " " + current);
                        command += "&& ";
                    }
                    command += "node " + SOURCE_FILENAME;
                    break;
                case JAVA:
                    command += "java " + SOURCE_FILENAME;
                    break;
            }

            command = "cd " + INPUT_PATH + " && " + command;

            // create I/O directories
            Files.createDirectories(Path.of(OUTPUT_PATH));
            Files.createDirectories(Path.of(INPUT_PATH));

            // write source code to input file
            Files.writeString(Path.of(INPUT_PATH, SOURCE_FILENAME), request.code(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            // I/O volumes bindings
            HostConfig hostConfig = new HostConfig();
            Bind inputBind = new Bind("/c" + INPUT_PATH, new Volume(INPUT_PATH));
            Bind outputBind = new Bind("/c" + OUTPUT_PATH, new Volume(OUTPUT_PATH));
            hostConfig.setBinds(inputBind, outputBind);

            // container creation
            CreateContainerResponse createContainerResponse = dockerClient
                    .createContainerCmd(language.getImage())
                    .withCmd("/bin/bash", "-c", command)
                    .withHostConfig(hostConfig)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();
            String containerId = createContainerResponse.getId();

            // container start
            dockerClient
                    .startContainerCmd(containerId)
                    .exec();

            // follow stdout and stderr
            List<String> outputFrames = new ArrayList<>();
            dockerClient
                    .logContainerCmd(containerId)
                    .withStdOut(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        public void onNext(Frame frame) {
                            outputFrames.add(new String(frame.getPayload(), StandardCharsets.UTF_8));
                        };
                    })
                    .awaitCompletion();

            List<String> errorFrames = new ArrayList<>();
            dockerClient
                    .logContainerCmd(containerId)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        public void onNext(Frame frame) {
                            errorFrames.add(new String(frame.getPayload(), StandardCharsets.UTF_8));
                        };
                    })
                    .awaitCompletion();

            return new ExecuteCodeResult(String.join("", outputFrames), String.join("", errorFrames));

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

    }

}
