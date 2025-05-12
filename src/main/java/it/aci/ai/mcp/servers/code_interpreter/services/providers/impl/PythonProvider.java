package it.aci.ai.mcp.servers.code_interpreter.services.providers.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import org.springframework.ai.chat.model.ChatModel;
import it.aci.ai.mcp.servers.code_interpreter.services.providers.LanguageProvider;

@Service
public class PythonProvider extends LanguageProvider {
    /**
     * Constructs a PythonProvider with injected ChatModel.
     *
     * @param chatModel the chat model for dependency inference
     */
    public PythonProvider(ChatModel chatModel) {
        super(chatModel);
    }

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
        return getSetEnvVariablesCommands(Map.of("PIP_DISABLE_PIP_VERSION_CHECK", "1"));
    }

    @Override
    public void prepareWorkspace(Path workspace, String sourceCode) throws IOException {
        // infer external dependencies
        Set<String> dependencies = inferDependencies(sourceCode);

        Files.writeString(workspace.resolve("requirements.txt"), String.join("\n", dependencies),
                StandardOpenOption.CREATE);
        Files.writeString(workspace.resolve(getSourceFileName()), sourceCode, StandardOpenOption.CREATE);
    }

    @Override
    public List<String> getPrepareExecutionCommands(Path workspace) {
        List<String> prepareCommands = new ArrayList<>();
        prepareCommands.add("pip install -r requirements.txt");
        return prepareCommands;
    }

    @Override
    public List<String> getExecutionCommands(Path workspace) {
        return List.of("python " + getSourceFileName());
    }

    protected Set<String> inferDependencies(String sourceCode) {

        return ChatClient.create(chatModel)
                .prompt()
                .user(u -> u
                        .text("```\n{sourceCode}\n```\nInfer the dependencies wich are not part of the Python Standard Library needed to run the provided code.\nProvide them in a format suitable for a requirements.txt file.")
                        .param("sourceCode", sourceCode))
                .options(AzureOpenAiChatOptions.builder()
                        .temperature(0.0)
                        .build())
                .call()
                .entity(new ParameterizedTypeReference<Set<String>>() {
                });

    }

}
