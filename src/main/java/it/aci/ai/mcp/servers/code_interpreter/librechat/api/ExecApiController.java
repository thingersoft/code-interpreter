package it.aci.ai.mcp.servers.code_interpreter.librechat.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeRequest;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeResult;
import it.aci.ai.mcp.servers.code_interpreter.dto.Language;
import it.aci.ai.mcp.servers.code_interpreter.librechat.model.ExecuteRequest;
import it.aci.ai.mcp.servers.code_interpreter.librechat.model.ExecuteResponse;
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
            case TS:
                yield Language.TYPESCRIPT;
            default:
                throw new IllegalArgumentException("Unsupported language: " + executeRequest.getLang());
        };

        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest(language, executeRequest.getCode());
        ExecuteCodeResult result = codeService.executeCode(executeCodeRequest);

        ExecuteResponse executeResponse = new ExecuteResponse();
        executeResponse.setStdout(result.stdOut());
        executeResponse.setStderr(result.stdErr());
        return executeResponse;
    }

}
