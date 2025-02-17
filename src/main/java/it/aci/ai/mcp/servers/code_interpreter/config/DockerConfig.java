package it.aci.ai.mcp.servers.code_interpreter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties("docker")
@Validated
public class DockerConfig {

    @NotBlank
    private String host;

    public DockerConfig(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

}
