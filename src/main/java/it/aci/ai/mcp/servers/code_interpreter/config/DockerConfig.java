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
    /**
     * Password for keystore and key material; should be set in configuration when TLS is enabled.
     */
    private String keyStorePassword = "";

    public DockerConfig(String host, boolean tls, String caCert, String clientCert, String clientKey) {
        this.host = host;
        this.tls = tls;
        this.caCert = caCert;
        this.clientCert = clientCert;
        this.clientKey = clientKey;
        // keyStorePassword can be set via setter or default to empty
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
    public String getKeyStorePassword() {
        return keyStorePassword;
    }
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getHost() {
        return host;
    }

}
