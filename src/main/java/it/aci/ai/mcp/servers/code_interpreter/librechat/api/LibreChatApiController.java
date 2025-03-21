package it.aci.ai.mcp.servers.code_interpreter.librechat.api;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeRequest;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeResult;
import it.aci.ai.mcp.servers.code_interpreter.dto.UploadedFile;
import it.aci.ai.mcp.servers.code_interpreter.enums.Language;
import it.aci.ai.mcp.servers.code_interpreter.librechat.model.ExecuteRequest;
import it.aci.ai.mcp.servers.code_interpreter.librechat.model.ExecuteResponse;
import it.aci.ai.mcp.servers.code_interpreter.librechat.model.FileObject;
import it.aci.ai.mcp.servers.code_interpreter.librechat.model.FileRef;
import it.aci.ai.mcp.servers.code_interpreter.librechat.model.UploadResponse;
import it.aci.ai.mcp.servers.code_interpreter.models.StoredFile;
import it.aci.ai.mcp.servers.code_interpreter.services.CodeService;
import it.aci.ai.mcp.servers.code_interpreter.services.FileService;
import jakarta.validation.Valid;

@RestController
@RequestMapping(LibreChatApiController.LIBRECHAT_API_PATH)
public class LibreChatApiController {

    public static final String LIBRECHAT_API_PATH = "/librechat";

    private final CodeService codeService;
    private final FileService fileService;

    public LibreChatApiController(CodeService codeService, FileService fileService) {
        this.codeService = codeService;
        this.fileService = fileService;
    }

    @DeleteMapping(value = "/files/{session_id}/{fileId}")
    public void filesSessionIdFileIdDelete(@PathVariable("session_id") String sessionId,
            @PathVariable("fileId") String fileId) {
        fileService.deleteFile(fileId);
    }

    @GetMapping(value = "/files/{session_id}")
    public List<FileObject> filesSessionIdGet(@PathVariable("session_id") String sessionId,
            @Valid @RequestParam(value = "detail", required = false, defaultValue = "simple") String detail) {

        List<StoredFile> storedFiles = fileService.findSessionFiles(sessionId);

        return storedFiles.stream()
                .map(LibreChatApiController::toFileObject)
                .toList();

    }

    @GetMapping(value = "/download/{sessionId}/{fileId}", produces = { "application/octet-stream" })
    public byte[] downloadSessionIdFileIdGet(@PathVariable String sessionId, @PathVariable String fileId) {
        return fileService.downloadFile(fileId);
    }

    @PostMapping(value = "/upload", consumes = { "multipart/form-data" })
    public UploadResponse uploadPost(@RequestPart(value = "entity_id", required = false) String entityId,
            @RequestPart(value = "file", required = true) List<MultipartFile> files) {

        List<StoredFile> storedFiles = fileService.uploadFile(
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
                .map(LibreChatApiController::toFileObject)
                .toList();

        UploadResponse uploadResponse = new UploadResponse();
        uploadResponse.setMessage("success");
        uploadResponse.setSessionId(fileObjects.get(0).getSessionId());
        uploadResponse.setFiles(fileObjects);
        return uploadResponse;

    }

    @PostMapping("/exec")
    public ExecuteResponse executeCode(@RequestBody ExecuteRequest executeRequest) {

        Language language = switch (executeRequest.getLang()) {
            case PY:
                yield Language.PYTHON;
            case JAVA:
                yield Language.JAVA;
            case JS:
            case TS:
                yield Language.TYPESCRIPT;
            default:
                throw new IllegalArgumentException("Unsupported language: " + executeRequest.getLang());
        };

        String sessionId = null;

        if (executeRequest.getFiles() != null && !executeRequest.getFiles().isEmpty()) {
            sessionId = executeRequest.getFiles().get(0).getSessionId();
        }

        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest(language, executeRequest.getCode(), sessionId);
        ExecuteCodeResult result = codeService.executeCode(executeCodeRequest);

        ExecuteResponse executeResponse = new ExecuteResponse();
        executeResponse.setSessionId(result.sessionId());
        executeResponse.setStdout(result.stdOut());
        executeResponse.setStderr(result.stdErr());

        List<FileRef> outputFiles = result.outputFiles().stream()
                .map(outputFile -> {
                    FileRef fileRef = new FileRef();
                    fileRef.setName(outputFile.relativePath());
                    fileRef.setId(outputFile.id());
                    fileRef.setPath("xxx");
                    return fileRef;
                })
                .toList();

        executeResponse.setFiles(outputFiles);

        return executeResponse;
    }

    private static FileObject toFileObject(StoredFile storedFile) {
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
