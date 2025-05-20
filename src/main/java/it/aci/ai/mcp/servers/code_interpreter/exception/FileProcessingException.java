package it.aci.ai.mcp.servers.code_interpreter.exception;

/**
 * Exception thrown when processing uploaded files fails.
 */
public class FileProcessingException extends RuntimeException {
    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}