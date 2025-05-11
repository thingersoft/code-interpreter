package it.aci.ai.mcp.servers.code_interpreter.dto;

import it.aci.ai.mcp.servers.code_interpreter.enums.Language;

public record ExecuteCodeRequest(Language language, String code, String sessionId, Dependency... dependencies) {

    // Override equals, hashCode, and toString to account for array content in dependencies
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExecuteCodeRequest other)) return false;
        return language.equals(other.language)
                && code.equals(other.code)
                && sessionId.equals(other.sessionId)
                && java.util.Arrays.equals(dependencies, other.dependencies);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(language, code, sessionId);
        result = 31 * result + java.util.Arrays.hashCode(dependencies);
        return result;
    }

    @Override
    public String toString() {
        return "ExecuteCodeRequest[language=" + language
                + ", code=" + code
                + ", sessionId=" + sessionId
                + ", dependencies=" + java.util.Arrays.toString(dependencies)
                + "]";
    }
}
