package it.aci.ai.mcp.servers.code_interpreter.librechat.api;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.aci.ai.mcp.servers.code_interpreter.librechat.model.FileObject;
import it.aci.ai.mcp.servers.code_interpreter.librechat.util.LibreChatUtils;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFile;
import it.aci.ai.mcp.servers.code_interpreter.services.CodeService;
import jakarta.validation.Valid;

@RestController
public class FilesApiController {

    private CodeService codeService;

    public FilesApiController(CodeService codeService) {
        this.codeService = codeService;
    }

    @DeleteMapping(value = "/files/{session_id}/{fileId}")
    public void filesSessionIdFileIdDelete(@PathVariable("session_id") String sessionId,
            @PathVariable("fileId") String fileId) {
        codeService.deleteFile(fileId);
    }

    @GetMapping(value = "/files/{session_id}")
    public List<FileObject> filesSessionIdGet(@PathVariable("session_id") String sessionId,
            @Valid @RequestParam(value = "detail", required = false, defaultValue = "simple") String detail) {

        List<StoredFile> storedFiles = codeService.findUploadedFiles(sessionId);

        return storedFiles.stream()
                .map(LibreChatUtils::toFileObject)
                .toList();

    }

}
