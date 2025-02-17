package it.aci.ai.mcp.servers.code_interpreter.dto;

public record ExecuteCodeRequest(Language language, String code, Dependency... dependencies) {

}
