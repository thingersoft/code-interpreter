package it.aci.ai.mcp.servers.code_interpreter.librechat.model;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import jakarta.validation.Valid;

/**
 * FileObject
 */
@Validated
public class FileObject {
  @JsonProperty("name")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String name = null;

  @JsonProperty("id")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String id = null;

  @JsonProperty("session_id")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String sessionId = null;

  @JsonProperty("content")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String content = null;

  @JsonProperty("size")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private BigDecimal size = null;

  @JsonProperty("lastModified")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String lastModified = null;

  @JsonProperty("etag")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String etag = null;

  @JsonProperty("metadata")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private FileObjectMetadata metadata = null;

  @JsonProperty("contentType")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String contentType = null;

  public FileObject name(String name) {

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

  public FileObject id(String id) {

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

  public FileObject sessionId(String sessionId) {

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

  public FileObject content(String content) {

    this.content = content;
    return this;
  }

  /**
   * Get content
   * 
   * @return content
   **/

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public FileObject size(BigDecimal size) {

    this.size = size;
    return this;
  }

  /**
   * Get size
   * 
   * @return size
   **/

  @Valid
  public BigDecimal getSize() {
    return size;
  }

  public void setSize(BigDecimal size) {
    this.size = size;
  }

  public FileObject lastModified(String lastModified) {

    this.lastModified = lastModified;
    return this;
  }

  /**
   * Get lastModified
   * 
   * @return lastModified
   **/

  public String getLastModified() {
    return lastModified;
  }

  public void setLastModified(String lastModified) {
    this.lastModified = lastModified;
  }

  public FileObject etag(String etag) {

    this.etag = etag;
    return this;
  }

  /**
   * Get etag
   * 
   * @return etag
   **/

  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
  }

  public FileObject metadata(FileObjectMetadata metadata) {

    this.metadata = metadata;
    return this;
  }

  /**
   * Get metadata
   * 
   * @return metadata
   **/

  @Valid
  public FileObjectMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(FileObjectMetadata metadata) {
    this.metadata = metadata;
  }

  public FileObject contentType(String contentType) {

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

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileObject fileObject = (FileObject) o;
    return Objects.equals(this.name, fileObject.name) &&
        Objects.equals(this.id, fileObject.id) &&
        Objects.equals(this.sessionId, fileObject.sessionId) &&
        Objects.equals(this.content, fileObject.content) &&
        Objects.equals(this.size, fileObject.size) &&
        Objects.equals(this.lastModified, fileObject.lastModified) &&
        Objects.equals(this.etag, fileObject.etag) &&
        Objects.equals(this.metadata, fileObject.metadata) &&
        Objects.equals(this.contentType, fileObject.contentType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, id, sessionId, content, size, lastModified, etag, metadata, contentType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FileObject {\n");

    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    sessionId: ").append(toIndentedString(sessionId)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    lastModified: ").append(toIndentedString(lastModified)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
    sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
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
