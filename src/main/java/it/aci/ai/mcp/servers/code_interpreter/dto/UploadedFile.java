package it.aci.ai.mcp.servers.code_interpreter.dto;

public record UploadedFile(String name, byte[] content) {

    // Override equals, hashCode, and toString to account for array content in content
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UploadedFile other)) return false;
        return name.equals(other.name)
                && java.util.Arrays.equals(content, other.content);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(name);
        result = 31 * result + java.util.Arrays.hashCode(content);
        return result;
    }

    @Override
    public String toString() {
        return "UploadedFile[name=" + name
                + ", content=" + java.util.Arrays.toString(content)
                + "]";
    }
}
