package it.aci.ai.mcp.servers.code_interpreter.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@ConfigurationProperties("app")
@Validated
public class AppConfig {

    @NotNull
    private Path localIoPath;
    @NotNull
    private String remoteIoPath;

    public AppConfig(Path localIoPath, String remoteIoPath) {
        this.localIoPath = localIoPath;
        this.remoteIoPath = remoteIoPath;
    }

    public Path getLocalIoPath() {
        return localIoPath;
    }

    public String getRemoteIoPath() {
        return remoteIoPath;
    }

}
