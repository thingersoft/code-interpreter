package it.aci.ai.mcp.servers.code_interpreter.exception;

/**
 * Exception thrown when file storage operations fail.
 */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}