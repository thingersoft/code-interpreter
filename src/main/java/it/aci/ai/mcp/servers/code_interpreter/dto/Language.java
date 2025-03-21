package it.aci.ai.mcp.servers.code_interpreter.dto;

public enum Language {

    PYTHON("python:3.12", ".py"),
    TYPESCRIPT("node:23", ".ts"),
    JAVA("maven:3.9.9-eclipse-temurin-21-jammy", ".java");

    private Language(String baseImage, String sourceFileExtension) {
        this.baseImage = baseImage;
        this.sourceFileExtension = sourceFileExtension;
    }

    private String baseImage;
    private String sourceFileExtension;

    public String getSourceFileExtension() {
        return sourceFileExtension;
    }

    public String getBaseImage() {
        return baseImage;
    }

}
