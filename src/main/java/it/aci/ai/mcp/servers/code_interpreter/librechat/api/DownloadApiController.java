package it.aci.ai.mcp.servers.code_interpreter.librechat.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import it.aci.ai.mcp.servers.code_interpreter.services.CodeService;

@RestController
public class DownloadApiController {

    private CodeService codeService;

    public DownloadApiController(CodeService codeService) {
        this.codeService = codeService;
    }

    @GetMapping(value = "/download/{sessionId}/{fileId}", produces = { "application/octet-stream" })
    public byte[] downloadSessionIdFileIdGet(@PathVariable String sessionId, @PathVariable String fileId) {
        return codeService.downloadFile(fileId);
    }

}
