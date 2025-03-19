package it.aci.ai.mcp.servers.code_interpreter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties("docker")
@Validated
public class DockerConfig {

    @NotBlank
    private String host;
    private boolean tls;
    private String caCert;
    private String clientCert;
    private String clientKey;

    public DockerConfig(String host, boolean tls, String caCert, String clientCert, String clientKey) {
        this.host = host;
        this.tls = tls;
        this.caCert = caCert;
        this.clientCert = clientCert;
        this.clientKey = clientKey;
    }

    public boolean isTls() {
        return tls;
    }

    public String getCaCert() {
        return caCert;
    }

    public String getClientCert() {
        return clientCert;
    }

    public String getClientKey() {
        return clientKey;
    }

    public String getHost() {
        return host;
    }

}
