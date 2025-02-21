package it.aci.ai.mcp.servers.code_interpreter.dto;

public enum Language {

    PYTHON("registry.gitlab.informatica.aci.it/ccsc/images/release/python:3.11", ".py", "python"),
    TYPESCRIPT("registry.gitlab.informatica.aci.it/ccsc/images/release/node:18.12", ".ts", "node"),
    JAVA("eclipse-temurin:21-jdk", ".java", "root");

    private Language(String image, String sourceFileExtension, String user) {
        this.image = image;
        this.sourceFileExtension = sourceFileExtension;
        this.user = user;
    }

    private String image;
    private String sourceFileExtension;
    private String user;

    public String getSourceFileExtension() {
        return sourceFileExtension;
    }

    public String getImage() {
        return image;
    }

    public String getUser() {
        return user;
    }

}
