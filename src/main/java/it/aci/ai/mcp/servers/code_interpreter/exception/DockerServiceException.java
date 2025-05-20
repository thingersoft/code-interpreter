package it.aci.ai.mcp.servers.code_interpreter.exception;

/**
 * Exception thrown when Docker service operations fail.
 */
public class DockerServiceException extends RuntimeException {
    public DockerServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}