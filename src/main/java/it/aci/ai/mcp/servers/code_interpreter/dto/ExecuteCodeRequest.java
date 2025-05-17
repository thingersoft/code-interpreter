package it.aci.ai.mcp.servers.code_interpreter.dto;

import it.aci.ai.mcp.servers.code_interpreter.enums.Language;

public record ExecuteCodeRequest(Language language, String code, String sessionId, Dependency... dependencies) {

}
