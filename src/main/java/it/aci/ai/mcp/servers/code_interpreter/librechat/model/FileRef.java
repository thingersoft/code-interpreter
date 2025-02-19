package it.aci.ai.mcp.servers.code_interpreter.librechat.model;

import java.util.Objects;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

/**
 * FileRef
 */
@Validated
public class FileRef {
  @JsonProperty("id")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String id = null;

  @JsonProperty("name")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String name = null;

  @JsonProperty("path")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String path = null;

  public FileRef id(String id) {

    this.id = id;
    return this;
  }

  /**
   * Get id
   * 
   * @return id
   **/

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public FileRef name(String name) {

    this.name = name;
    return this;
  }

  /**
   * Get name
   * 
   * @return name
   **/

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FileRef path(String path) {

    this.path = path;
    return this;
  }

  /**
   * Get path
   * 
   * @return path
   **/

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileRef fileRef = (FileRef) o;
    return Objects.equals(this.id, fileRef.id) &&
        Objects.equals(this.name, fileRef.name) &&
        Objects.equals(this.path, fileRef.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, path);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FileRef {\n");

    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
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
