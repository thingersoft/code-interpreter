package it.aci.ai.mcp.servers.code_interpreter.dto;

import it.aci.ai.mcp.servers.code_interpreter.enums.Language;

import java.util.Arrays;
import java.util.Objects;

public record ExecuteCodeRequest(Language language,
                                 String code,
                                 String sessionId,
                                 Dependency... dependencies) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Use record pattern to destructure the compared object
        if (!(o instanceof ExecuteCodeRequest(var otherLanguage, var otherCode, var otherSessionId, var otherDependencies))) return false;
        return language == otherLanguage
                && Objects.equals(code, otherCode)
                && Objects.equals(sessionId, otherSessionId)
                && Arrays.equals(dependencies, otherDependencies);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(language, code, sessionId);
        result = 31 * result + Arrays.hashCode(dependencies);
        return result;
    }

    @Override
    public String toString() {
        return "ExecuteCodeRequest[language=" + language
                + ", code=" + code
                + ", sessionId=" + sessionId
                + ", dependencies=" + Arrays.toString(dependencies)
                + "]";
    }
}
