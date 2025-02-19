package it.aci.ai.mcp.servers.code_interpreter.librechat.model;

import java.util.Objects;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

/**
 * FileObjectMetadata
 */
@Validated
public class FileObjectMetadata {
  @JsonProperty("content-type")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String contentType = null;

  @JsonProperty("original-filename")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String originalFilename = null;

  public FileObjectMetadata contentType(String contentType) {

    this.contentType = contentType;
    return this;
  }

  /**
   * Get contentType
   * 
   * @return contentType
   **/

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public FileObjectMetadata originalFilename(String originalFilename) {

    this.originalFilename = originalFilename;
    return this;
  }

  /**
   * Get originalFilename
   * 
   * @return originalFilename
   **/

  public String getOriginalFilename() {
    return originalFilename;
  }

  public void setOriginalFilename(String originalFilename) {
    this.originalFilename = originalFilename;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileObjectMetadata fileObjectMetadata = (FileObjectMetadata) o;
    return Objects.equals(this.contentType, fileObjectMetadata.contentType) &&
        Objects.equals(this.originalFilename, fileObjectMetadata.originalFilename);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentType, originalFilename);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FileObjectMetadata {\n");

    sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
    sb.append("    originalFilename: ").append(toIndentedString(originalFilename)).append("\n");
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
