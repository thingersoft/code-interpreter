package it.aci.ai.mcp.servers.code_interpreter.exception;

/**
 * Exception thrown when file service operations fail.
 */
public class FileServiceException extends RuntimeException {
    public FileServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}