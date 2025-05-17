package it.aci.ai.mcp.servers.code_interpreter.exception;

/**
 * Exception thrown when CodeService fails to initialize or execute.
 */
public class CodeServiceException extends RuntimeException {
    public CodeServiceException(String message) {
        super(message);
    }

    public CodeServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}