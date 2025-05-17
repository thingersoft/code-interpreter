package it.aci.ai.mcp.servers.code_interpreter.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.stereotype.Service;
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

import it.aci.ai.mcp.servers.code_interpreter.config.DockerConfig;
import jakarta.annotation.PostConstruct;

@Service
public class DockerService {

    private static final Logger LOG = LoggerFactory.getLogger(DockerService.class);

    private final DockerConfig dockerConfig;

    private DockerClient dockerClient;

    public DockerService(DockerConfig dockerConfig) {
        this.dockerConfig = dockerConfig;
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
                throw new it.aci.ai.mcp.servers.code_interpreter.exception.CodeExecutionException(
                        "Failed to initialize Docker TLS context", e);
            }
        }
        dockerClient = DockerClientImpl.getInstance(config, builder.build());
    }

    public void cleanContainer(String containerId) {
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

    public List<Container> findContainersByName(String name) {
        return dockerClient
                .listContainersCmd()
                .withShowAll(true)
                .withNameFilter(List.of(name))
                .exec();
    }

    public boolean imageExists(String imageName) {
        try {
            dockerClient
                    .inspectImageCmd(imageName)
                    .exec();
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    public String buildImage(File dockerfile, String imageName, Map<String, String> buildArgs) {
        BuildImageCmd buildImageCmd = dockerClient
                .buildImageCmd(dockerfile)
                .withTags(Set.of(imageName));
        for (Entry<String, String> buildArg : buildArgs.entrySet()) {
            buildImageCmd.withBuildArg(buildArg.getKey(), buildArg.getValue());
        }
        BuildSandboxImageResultCallback buildImageResultCallback = new BuildSandboxImageResultCallback();
        buildImageCmd.exec(buildImageResultCallback);
        String imageId = buildImageResultCallback.awaitImageId();
        LOG.trace(
                "Image " + imageId + " build log: " + System.lineSeparator()
                        + String.join(System.lineSeparator(),
                                buildImageResultCallback.getBuildImageSteps()));

        return imageId;
    }

    public String createContainer(String imageName, String containerName, String user) {
        CreateContainerResponse createContainerResponse = dockerClient
                .createContainerCmd(imageName)
                .withName(containerName)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withUser(user)
                .exec();
        return createContainerResponse.getId();
    }

    public LoggingResultCallback startContainer(String containerId, String logTag) {
        LoggingResultCallback resultCallback = new LoggingResultCallback(true, logTag);

        dockerClient
                .startContainerCmd(containerId)
                .exec();

        // log stdout and stderr
        dockerClient
                .logContainerCmd(containerId)
                .withStdErr(true)
                .withStdOut(true)
                .withFollowStream(true)
                .exec(resultCallback);

        return resultCallback;
    }

    public LoggingResultCallback runInContainer(String containerId, List<String> commands, boolean log, String logTag)
            throws InterruptedException {

        LoggingResultCallback resultCallback = new LoggingResultCallback(log, logTag);

        // prepend init env command
        commands.add(0, "export PATH=\"$HOME/.local/bin:$PATH\"");
        ExecCreateCmdResponse createPrepareCommandResponse = dockerClient
                .execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("sh", "-l", "-c", String.join(" && ", commands))
                .exec();

        // run commands
        dockerClient
                .execStartCmd(createPrepareCommandResponse.getId())
                .exec(resultCallback)
                .awaitCompletion();

        return resultCallback;
    }

    public void copyToContainer(String containerId, Path localPath, String remotePath) throws IOException {
        dockerClient
                .copyArchiveToContainerCmd(containerId)
                .withHostResource(localPath.toString())
                .withRemotePath(remotePath)
                .withCopyUIDGID(true)
                .exec();
    }

    public TarArchiveInputStream copyArchiveFromContainer(String containerId, String remotePath) {
        InputStream is = dockerClient
                .copyArchiveFromContainerCmd(containerId, remotePath)
                .exec();
        return new TarArchiveInputStream(is);
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
        // Use no default hardcoded password
        keyStore.setKeyEntry("client", privateKey, new char[0], new Certificate[] { clientCertificate });

        // Create an SSLContext
        // Use no default hardcoded password
        return SSLContextBuilder.create()
                .loadTrustMaterial(keyStore, null)
                .loadKeyMaterial(keyStore, new char[0])
                .build();
    }

    private static byte[] decodePemPrivateKey(String pem) {
        String key = pem.replaceAll("-----.*?-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(key);
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
    public class LoggingResultCallback extends ResultCallback.Adapter<Frame> {

        private List<String> outputFrames = new ArrayList<>();
        private List<String> errorFrames = new ArrayList<>();
        private boolean log;
        private String logPrefix = "";

        public LoggingResultCallback(boolean log, String logTag) {
            this.log = log;
            if (StringUtils.hasText(logTag)) {
                this.logPrefix = "[" + logTag + "] ";
            }
        }

        @Override
        public void onNext(Frame frame) {

            Level logLevel = switch (frame.getStreamType()) {
                case STDOUT:
                case RAW:
                    outputFrames.add(frameToString(frame));
                    yield Level.TRACE;
                case STDERR:
                    errorFrames.add(frameToString(frame));
                    yield Level.ERROR;
                default:
                    throw new RuntimeException("unknown stream type:" + frame.getStreamType());
            };

            if (log) {
                LOG.atLevel(logLevel).log(logPrefix + frameToString(frame));
            }
        }

        private String frameToString(Frame frame) {
            return new String(frame.getPayload(), StandardCharsets.UTF_8);
        };

        public String getStdOut() {
            return String.join("", outputFrames);
        }

        public String getStdErr() {
            return String.join("", errorFrames);
        }

    }

}
