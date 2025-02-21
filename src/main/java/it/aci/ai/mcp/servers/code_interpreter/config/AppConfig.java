package it.aci.ai.mcp.servers.code_interpreter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties("app")
@Validated
public class AppConfig {

    @NotBlank
    private String localIoPath;
    @NotBlank
    private String remoteIoPath;

    public AppConfig(@NotBlank String localIoPath, @NotBlank String remoteIoPath) {
        this.localIoPath = localIoPath;
        this.remoteIoPath = remoteIoPath;
    }

    public String getLocalIoPath() {
        return localIoPath;
    }

    public String getRemoteIoPath() {
        return remoteIoPath;
    }

}
