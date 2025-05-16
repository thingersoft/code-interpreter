package it.aci.ai.mcp.servers.code_interpreter.dto;

public record UploadedFile(String name, byte[] content) {
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UploadedFile other)) return false;
        return Objects.equals(name, other.name)
                && Arrays.equals(content, other.content);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }

    @Override
    public String toString() {
        return "UploadedFile[name=" + name + ", contentLength=" + (content != null ? content.length : 0) + "]";
    }
}
