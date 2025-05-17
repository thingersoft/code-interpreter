package it.aci.ai.mcp.servers.code_interpreter.exception;

/**
 * Exception thrown by DockerService when an error occurs during Docker operations.
 */
public class DockerServiceException extends RuntimeException {
    public DockerServiceException(String message) {
        super(message);
    }

    public DockerServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}