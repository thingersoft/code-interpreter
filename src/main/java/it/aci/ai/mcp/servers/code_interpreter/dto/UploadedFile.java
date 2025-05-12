package it.aci.ai.mcp.servers.code_interpreter.dto;

import java.util.Arrays;
import java.util.Objects;

public record UploadedFile(String name, byte[] content) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Use record pattern to destructure the compared object
        if (!(o instanceof UploadedFile(var otherName, var otherContent))) return false;
        return Objects.equals(name, otherName)
                && Arrays.equals(content, otherContent);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }

    @Override
    public String toString() {
        return "UploadedFile[name=" + name
                + ", contentLength=" + (content != null ? content.length : 0)
                + "]";
    }
}
