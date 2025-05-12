package it.aci.ai.mcp.servers.code_interpreter.exception;

/**
 * Generic exception for code interpreter errors.
 */
public class CodeInterpreterException extends RuntimeException {
    /**
     * Constructs a new CodeInterpreterException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public CodeInterpreterException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new CodeInterpreterException with the specified cause.
     *
     * @param cause the cause
     */
    public CodeInterpreterException(Throwable cause) {
        super(cause);
    }
}