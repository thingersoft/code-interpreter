package it.aci.ai.mcp.servers.code_interpreter.librechat.model;

import java.util.Objects;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

/**
 * RequestFile
 */
@Validated

public class RequestFile {
  @JsonProperty("id")

  private String id = null;

  @JsonProperty("session_id")

  private String sessionId = null;

  @JsonProperty("name")

  private String name = null;

  public RequestFile id(String id) {

    this.id = id;
    return this;
  }

  /**
   * Get id
   * 
   * @return id
   **/

  @NotNull
  public String getId() {
    return id;
  }

  public void setId(String id) {

    this.id = id;
  }

  public RequestFile sessionId(String sessionId) {

    this.sessionId = sessionId;
    return this;
  }

  /**
   * Get sessionId
   * 
   * @return sessionId
   **/

  @NotNull
  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {

    this.sessionId = sessionId;
  }

  public RequestFile name(String name) {

    this.name = name;
    return this;
  }

  /**
   * Get name
   * 
   * @return name
   **/

  @NotNull
  public String getName() {
    return name;
  }

  public void setName(String name) {

    this.name = name;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RequestFile requestFile = (RequestFile) o;
    return Objects.equals(this.id, requestFile.id) &&
        Objects.equals(this.sessionId, requestFile.sessionId) &&
        Objects.equals(this.name, requestFile.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, sessionId, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RequestFile {\n");

    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    sessionId: ").append(toIndentedString(sessionId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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
