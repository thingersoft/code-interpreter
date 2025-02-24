package it.aci.ai.mcp.servers.code_interpreter.librechat.api;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeRequest;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeResult;
import it.aci.ai.mcp.servers.code_interpreter.dto.Language;
import it.aci.ai.mcp.servers.code_interpreter.librechat.model.ExecuteRequest;
import it.aci.ai.mcp.servers.code_interpreter.librechat.model.ExecuteResponse;
import it.aci.ai.mcp.servers.code_interpreter.librechat.model.FileRef;
import it.aci.ai.mcp.servers.code_interpreter.services.CodeService;

@RestController
public class ExecApiController {

    private CodeService codeService;

    public ExecApiController(CodeService codeService) {
        this.codeService = codeService;
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
                    fileRef.setName(outputFile.filename());
                    fileRef.setId(outputFile.id());
                    fileRef.setPath("xxx");
                    return fileRef;
                })
                .toList();

        executeResponse.setFiles(outputFiles);

        return executeResponse;
    }

}
