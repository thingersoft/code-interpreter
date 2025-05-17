package it.aci.ai.mcp.servers.code_interpreter.services.providers.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import it.aci.ai.mcp.servers.code_interpreter.services.providers.LanguageProvider;

@Service
public class TypescriptProvider extends LanguageProvider {
    /**
     * Constructor injection for ChatModel
     */
    public TypescriptProvider(org.springframework.ai.chat.model.ChatModel chatModel) {
        super(chatModel);
    }

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
        List<String> prepareExecutionCommands = new ArrayList<>();
        try {
            String sourceCode = Files.readString(workspace.resolve(getSourceFileName()));
            Set<String> dependencies = inferDependencies(sourceCode);
            if (!dependencies.isEmpty()) {
                prepareExecutionCommands.add("npm install --no-warnings " + String.join(" ", dependencies));
            }
        } catch (IOException e) {
            throw new it.aci.ai.mcp.servers.code_interpreter.exception.CodeExecutionException(
                    "Failed to prepare dependencies for Typescript code", e);
        }
        return prepareExecutionCommands;
    }

    @Override
    public List<String> getExecutionCommands(Path workspace) {
        return List.of("node --no-warnings " + getSourceFileName());
    }

    protected Set<String> inferDependencies(String sourceCode) {

        return ChatClient.create(chatModel)
                .prompt()
                .user(u -> u
                        .text("```\n{sourceCode}\n```\nInfer the dependencies wich are not part of the standard Node.js library needed to run the provided code.\nProvide them in a format suitable to be appended to a `npm install ` command.")
                        .param("sourceCode", sourceCode))
                .options(AzureOpenAiChatOptions.builder()
                        .temperature(0.0)
                        .build())
                .call()
                .entity(new ParameterizedTypeReference<Set<String>>() {
                });

    }

}
