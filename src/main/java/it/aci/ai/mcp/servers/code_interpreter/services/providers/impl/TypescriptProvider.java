package it.aci.ai.mcp.servers.code_interpreter.services.providers.impl;

import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Service;

import it.aci.ai.mcp.servers.code_interpreter.services.providers.LanguageProvider;

@Service
public class TypescriptProvider extends LanguageProvider {

    @Override
    public String getFromImage() {
        return "node:23";
    }

    @Override
    public String getSourceFileName() {
        return "source.ts";
    }

    @Override
    public List<String> getPrepareExecutionCommands(Path workspace) {
        return List.of();
    }

    @Override
    public List<String> getExecutionCommands(Path workspace) {
        return List.of("node --no-warnings " + getSourceFileName());
    }

}
