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
        if (!(o instanceof ExecuteCodeRequest)) return false;
        ExecuteCodeRequest other = (ExecuteCodeRequest) o;
        return language == other.language
                && Objects.equals(code, other.code)
                && Objects.equals(sessionId, other.sessionId)
                && Arrays.equals(dependencies, other.dependencies);
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
