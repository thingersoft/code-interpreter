package it.aci.ai.mcp.servers.code_interpreter.exception;

/**
 * Exception thrown when code execution fails.
 */
public class CodeExecutionException extends RuntimeException {
    public CodeExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
    public CodeExecutionException(Throwable cause) {
        super(cause);
    }
}