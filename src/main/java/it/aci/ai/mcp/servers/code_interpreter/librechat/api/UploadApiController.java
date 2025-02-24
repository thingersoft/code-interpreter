package it.aci.ai.mcp.servers.code_interpreter.librechat.api;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import it.aci.ai.mcp.servers.code_interpreter.dto.UploadedFile;
import it.aci.ai.mcp.servers.code_interpreter.librechat.model.FileObject;
import it.aci.ai.mcp.servers.code_interpreter.librechat.model.UploadResponse;
import it.aci.ai.mcp.servers.code_interpreter.librechat.util.LibreChatUtils;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFile;
import it.aci.ai.mcp.servers.code_interpreter.services.CodeService;

@RestController
public class UploadApiController {

    private CodeService codeService;

    public UploadApiController(CodeService codeService) {
        this.codeService = codeService;
    }

    @PostMapping(value = "/upload", consumes = { "multipart/form-data" })
    public UploadResponse uploadPost(@RequestPart(value = "entity_id", required = false) String entityId,
            @RequestPart(value = "file", required = true) List<MultipartFile> files) {

        List<StoredFile> storedFiles = codeService.uploadFile(
                files.stream()
                        .map(file -> {
                            try {
                                return new UploadedFile(file.getOriginalFilename(), file.getBytes());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .toList());

        List<FileObject> fileObjects = storedFiles.stream()
                .map(LibreChatUtils::toFileObject)
                .toList();

        UploadResponse uploadResponse = new UploadResponse();
        uploadResponse.setMessage("success");
        uploadResponse.setSessionId(fileObjects.get(0).getSessionId());
        uploadResponse.setFiles(fileObjects);
        return uploadResponse;

    }

}
