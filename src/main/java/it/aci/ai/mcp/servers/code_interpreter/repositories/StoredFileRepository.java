package it.aci.ai.mcp.servers.code_interpreter.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.aci.ai.mcp.servers.code_interpreter.models.StoredFile;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFileType;

public interface StoredFileRepository extends MongoRepository<StoredFile, String> {

    List<StoredFile> findBySessionId(String sessionId);

    List<StoredFile> findBySessionIdAndType(String sessionId, StoredFileType type);

}
