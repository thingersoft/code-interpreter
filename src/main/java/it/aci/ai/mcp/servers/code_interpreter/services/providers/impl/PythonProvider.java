package it.aci.ai.mcp.servers.code_interpreter.services.providers.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import it.aci.ai.mcp.servers.code_interpreter.services.providers.LanguageProvider;

@Service
public class PythonProvider extends LanguageProvider {

    @Override
    public String getFromImage() {
        return "python:3.12";
    }

    @Override
    public String getSourceFileName() {
        return "source.py";
    }

    @Override
    public List<String> getImageInitCommands() {
        List<String> initCommands = new ArrayList<>();
        initCommands.addAll(getSetEnvVariablesCommands(Map.of("PIP_DISABLE_PIP_VERSION_CHECK", "1")));
        initCommands.add("pip install pipreqs");
        return initCommands;
    }

    @Override
    public List<String> getPrepareExecutionCommands(Path workspace) {
        List<String> prepareCommands = new ArrayList<>();
        prepareCommands.add("pipreqs --scan-notebooks --force .");
        prepareCommands.add("pip install -r requirements.txt");
        return prepareCommands;
    }

    @Override
    public List<String> getExecutionCommands(Path workspace) {
        return List.of("python " + getSourceFileName());
    }

}
