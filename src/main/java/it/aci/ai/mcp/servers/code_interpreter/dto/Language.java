package it.aci.ai.mcp.servers.code_interpreter.dto;

public enum Language {

    PYTHON("registry.gitlab.informatica.aci.it/ccsc/images/release/python:3.11", ".py"),
    TYPESCRIPT("registry.gitlab.informatica.aci.it/ccsc/images/release/node:18.12", ".ts"),
    JAVA("eclipse-temurin:21-jdk", ".java");

    private Language(String image, String sourceFileExtension) {
        this.image = image;
        this.sourceFileExtension = sourceFileExtension;
    }

    private String image;
    private String sourceFileExtension;

    public String getSourceFileExtension() {
        return sourceFileExtension;
    }

    public String getImage() {
        return image;
    }

}
