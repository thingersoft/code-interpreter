package it.aci.ai.mcp.servers.code_interpreter.librechat.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import jakarta.validation.Valid;

/**
 * UploadBody
 */
@Validated

public class UploadBody {
  @JsonProperty("entity_id")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String entityId = null;

  @JsonProperty("files")
  @Valid
  private List<Resource> files = null;

  public UploadBody entityId(String entityId) {

    this.entityId = entityId;
    return this;
  }

  /**
   * Get entityId
   * 
   * @return entityId
   **/

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public UploadBody files(List<Resource> files) {

    this.files = files;
    return this;
  }

  public UploadBody addFilesItem(Resource filesItem) {
    if (this.files == null) {
      this.files = new ArrayList<Resource>();
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
  public List<Resource> getFiles() {
    return files;
  }

  public void setFiles(List<Resource> files) {
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
    UploadBody uploadBody = (UploadBody) o;
    return Objects.equals(this.entityId, uploadBody.entityId) &&
        Objects.equals(this.files, uploadBody.files);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityId, files);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UploadBody {\n");

    sb.append("    entityId: ").append(toIndentedString(entityId)).append("\n");
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
