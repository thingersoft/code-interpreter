package it.aci.ai.mcp.servers.code_interpreter.librechat.api;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.aci.ai.mcp.servers.code_interpreter.librechat.model.UploadResponse;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class UploadApiController {

    private static final Logger log = LoggerFactory.getLogger(UploadApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    public UploadApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @RequestMapping(value = "/upload", produces = { "application/json" }, consumes = {
            "multipart/form-data" }, method = RequestMethod.POST)
    public ResponseEntity<UploadResponse> uploadPost(
            @RequestPart(value = "entity_id", required = false) String entityId,
            @RequestPart(value = "file", required = true) List<MultipartFile> files) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<UploadResponse>(objectMapper.readValue(
                        "{\n  \"session_id\" : \"session_id\",\n  \"files\" : [ {\n    \"metadata\" : {\n      \"content-type\" : \"content-type\",\n      \"original-filename\" : \"original-filename\"\n    },\n    \"size\" : 0.8008281904610115,\n    \"name\" : \"name\",\n    \"session_id\" : \"session_id\",\n    \"etag\" : \"etag\",\n    \"id\" : \"id\",\n    \"lastModified\" : \"lastModified\",\n    \"contentType\" : \"contentType\",\n    \"content\" : \"content\"\n  }, {\n    \"metadata\" : {\n      \"content-type\" : \"content-type\",\n      \"original-filename\" : \"original-filename\"\n    },\n    \"size\" : 0.8008281904610115,\n    \"name\" : \"name\",\n    \"session_id\" : \"session_id\",\n    \"etag\" : \"etag\",\n    \"id\" : \"id\",\n    \"lastModified\" : \"lastModified\",\n    \"contentType\" : \"contentType\",\n    \"content\" : \"content\"\n  } ],\n  \"message\" : \"success\"\n}",
                        UploadResponse.class), HttpStatus.OK);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<UploadResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<UploadResponse>(HttpStatus.NOT_IMPLEMENTED);
    }

}
