package it.aci.ai.mcp.servers.code_interpreter.exception;

/**
 * Exception thrown for Docker service related errors.
 */
public class DockerServiceException extends CodeInterpreterException {

    /**
     * Constructs a new DockerServiceException with the specified detail message.
     *
     * @param message the detail message
     */
    public DockerServiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new DockerServiceException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public DockerServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new DockerServiceException with the specified cause.
     *
     * @param cause the cause
     */
    public DockerServiceException(Throwable cause) {
        super(cause);
    }
}