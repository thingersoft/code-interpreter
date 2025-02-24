package it.aci.ai.mcp.servers.code_interpreter.librechat.util;

import it.aci.ai.mcp.servers.code_interpreter.librechat.model.FileObject;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFile;

public final class LibreChatUtils {

    private LibreChatUtils() {
    }

    public static FileObject toFileObject(StoredFile storedFile) {
        FileObject fileObject = new FileObject();
        fileObject.setFileId(storedFile.id());
        fileObject.setId(storedFile.id());
        fileObject.setSessionId(storedFile.sessionId());
        fileObject.setContentType(storedFile.contentType());
        fileObject.setLastModified(storedFile.lastModified());
        fileObject.setName(storedFile.sessionId() + "/" + storedFile.id());
        fileObject.setSize(storedFile.size());
        return fileObject;
    }

}
