package it.aci.ai.mcp.servers.code_interpreter.librechat.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import jakarta.validation.Valid;

/**
 * UploadResponse
 */
@Validated

public class UploadResponse {
  @JsonProperty("message")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String message = null;

  @JsonProperty("session_id")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String sessionId = null;

  @JsonProperty("files")
  @Valid
  private List<FileObject> files = null;

  public UploadResponse message(String message) {

    this.message = message;
    return this;
  }

  /**
   * Get message
   * 
   * @return message
   **/

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public UploadResponse sessionId(String sessionId) {

    this.sessionId = sessionId;
    return this;
  }

  /**
   * Get sessionId
   * 
   * @return sessionId
   **/

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public UploadResponse files(List<FileObject> files) {

    this.files = files;
    return this;
  }

  public UploadResponse addFilesItem(FileObject filesItem) {
    if (this.files == null) {
      this.files = new ArrayList<>();
    }
    this.files.add(filesItem);
    return this;
  }

  /**
   * Get files
   * 
   * @return files
   **/

  @Valid
  public List<FileObject> getFiles() {
    return files;
  }

  public void setFiles(List<FileObject> files) {
    this.files = files;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UploadResponse uploadResponse = (UploadResponse) o;
    return Objects.equals(this.message, uploadResponse.message) &&
        Objects.equals(this.sessionId, uploadResponse.sessionId) &&
        Objects.equals(this.files, uploadResponse.files);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, sessionId, files);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UploadResponse {\n");

    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    sessionId: ").append(toIndentedString(sessionId)).append("\n");
    sb.append("    files: ").append(toIndentedString(files)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
