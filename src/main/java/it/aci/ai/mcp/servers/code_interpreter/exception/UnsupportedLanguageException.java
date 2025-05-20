package it.aci.ai.mcp.servers.code_interpreter.exception;

/**
 * Exception thrown when an unsupported language is requested.
 */
public class UnsupportedLanguageException extends RuntimeException {
    public UnsupportedLanguageException(String message) {
        super(message);
    }
}