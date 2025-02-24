package it.aci.ai.mcp.servers.code_interpreter.dto;

import java.util.List;

import it.aci.ai.mcp.servers.code_interpreter.models.StoredFile;

public record ExecuteCodeResult(String stdOut, String stdErr, String sessionId, List<StoredFile> outputFiles) {

}
