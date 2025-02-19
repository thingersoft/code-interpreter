package it.aci.ai.mcp.servers.code_interpreter.librechat.api;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.aci.ai.mcp.servers.code_interpreter.librechat.model.FileObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class FilesApiController {

    private static final Logger log = LoggerFactory.getLogger(FilesApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    public FilesApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @RequestMapping(value = "/files/{session_id}/{fileId}", produces = {
            "application/json" }, method = RequestMethod.DELETE)
    public ResponseEntity<Void> filesSessionIdFileIdDelete(@PathVariable("session_id") String sessionId,
            @PathVariable("fileId") String fileId) {
        return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/files/{session_id}", produces = { "application/json" }, method = RequestMethod.GET)
    public ResponseEntity<List<FileObject>> filesSessionIdGet(
            @PathVariable("session_id") String sessionId,
            @Valid @RequestParam(value = "detail", required = false, defaultValue = "simple") String detail) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<FileObject>>(objectMapper.readValue(
                        "[ {\n  \"metadata\" : {\n    \"content-type\" : \"content-type\",\n    \"original-filename\" : \"original-filename\"\n  },\n  \"size\" : 0.8008281904610115,\n  \"name\" : \"name\",\n  \"session_id\" : \"session_id\",\n  \"etag\" : \"etag\",\n  \"id\" : \"id\",\n  \"lastModified\" : \"lastModified\",\n  \"contentType\" : \"contentType\",\n  \"content\" : \"content\"\n}, {\n  \"metadata\" : {\n    \"content-type\" : \"content-type\",\n    \"original-filename\" : \"original-filename\"\n  },\n  \"size\" : 0.8008281904610115,\n  \"name\" : \"name\",\n  \"session_id\" : \"session_id\",\n  \"etag\" : \"etag\",\n  \"id\" : \"id\",\n  \"lastModified\" : \"lastModified\",\n  \"contentType\" : \"contentType\",\n  \"content\" : \"content\"\n} ]",
                        List.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<List<FileObject>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<List<FileObject>>(HttpStatus.NOT_IMPLEMENTED);
    }

}
