package it.aci.ai.mcp.servers.code_interpreter.services.providers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class LanguageProvider {

    public static final String IMAGE_USER = "intepreter";

    @Autowired
    protected ChatModel chatModel;

    public abstract String getFromImage();

    public abstract String getSourceFileName();

    public List<String> getImageInitCommands() {
        return List.of();
    }

    public void prepareWorkspace(Path workspace, String sourceCode) throws IOException {
        Files.writeString(workspace.resolve(getSourceFileName()), sourceCode, StandardOpenOption.CREATE);
    }

    public abstract List<String> getPrepareExecutionCommands(Path workspace);

    public abstract List<String> getExecutionCommands(Path workspace);

    protected List<String> getSetEnvVariablesCommands(Map<String, String> envVariables) {
        List<String> setEnvVariablesCommands = envVariables.entrySet().stream()
                .map(entry -> "echo 'export " + entry.getKey() + "=\"" + entry.getValue() + "\"' >> ~/.profile")
                .collect(Collectors.toCollection(ArrayList::new));
        setEnvVariablesCommands.add(". ~/.profile");
        return setEnvVariablesCommands;
    }

}
