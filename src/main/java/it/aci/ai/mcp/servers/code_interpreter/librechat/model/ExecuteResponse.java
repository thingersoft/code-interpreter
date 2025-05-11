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
 * ExecuteResponse
 */
@Validated

public class ExecuteResponse {

  private String stdout;
  private String stderr;

  @JsonProperty("run")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private ExecuteResponseRun run = null;

  @JsonProperty("language")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String language = null;

  @JsonProperty("version")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String version = null;

  @JsonProperty("session_id")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String sessionId = null;

  @JsonProperty("files")
  @Valid
  private List<FileRef> files = null;

  public ExecuteResponse run(ExecuteResponseRun run) {

    this.run = run;
    return this;
  }

  /**
   * Get run
   * 
   * @return run
   **/

  @Valid
  public ExecuteResponseRun getRun() {
    return run;
  }

  public void setRun(ExecuteResponseRun run) {
    this.run = run;
  }

  public ExecuteResponse language(String language) {

    this.language = language;
    return this;
  }

  /**
   * Get language
   * 
   * @return language
   **/

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public ExecuteResponse version(String version) {

    this.version = version;
    return this;
  }

  /**
   * Get version
   * 
   * @return version
   **/

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public ExecuteResponse sessionId(String sessionId) {

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

  public ExecuteResponse files(List<FileRef> files) {

    this.files = files;
    return this;
  }

  public ExecuteResponse addFilesItem(FileRef filesItem) {
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
  public List<FileRef> getFiles() {
    return files;
  }

  public void setFiles(List<FileRef> files) {
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
    ExecuteResponse executeResponse = (ExecuteResponse) o;
    return Objects.equals(this.run, executeResponse.run) &&
        Objects.equals(this.language, executeResponse.language) &&
        Objects.equals(this.version, executeResponse.version) &&
        Objects.equals(this.sessionId, executeResponse.sessionId) &&
        Objects.equals(this.files, executeResponse.files);
  }

  @Override
  public int hashCode() {
    return Objects.hash(run, language, version, sessionId, files);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExecuteResponse {\n");

    sb.append("    run: ").append(toIndentedString(run)).append("\n");
    sb.append("    language: ").append(toIndentedString(language)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

  public String getStdout() {
    return stdout;
  }

  public void setStdout(String stdout) {
    this.stdout = stdout;
  }

  public String getStderr() {
    return stderr;
  }

  public void setStderr(String stderr) {
    this.stderr = stderr;
  }
}
