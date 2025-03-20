package it.aci.ai.mcp.servers.code_interpreter.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties("app")
@Validated
public class AppConfig {

    @NotNull
    private final Path storagePath;
    @NotBlank
    private final String remoteIoPath;
    @NotBlank
    private final String apiKey;

    public AppConfig(Path storagePath, String remoteIoPath, String apiKey) {
        this.storagePath = storagePath;
        this.remoteIoPath = remoteIoPath;
        this.apiKey = apiKey;
    }

    public Path getStoragePath() {
        return storagePath;
    }

    public String getRemoteIoPath() {
        return remoteIoPath;
    }

    public String getApiKey() {
        return apiKey;
    }

}
