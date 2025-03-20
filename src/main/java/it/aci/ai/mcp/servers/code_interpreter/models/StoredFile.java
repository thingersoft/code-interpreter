package it.aci.ai.mcp.servers.code_interpreter.models;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "codeinterpreter_files")
public record StoredFile(
                @Id String id,
                String sessionId,
                String relativePath,
                Instant lastModified,
                Integer size,
                String contentType,
                StoredFileType type) {

}
