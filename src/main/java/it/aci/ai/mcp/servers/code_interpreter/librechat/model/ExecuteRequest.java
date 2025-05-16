package it.aci.ai.mcp.servers.code_interpreter.librechat.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.Nulls;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * RequestBody
 */
@Validated
public class ExecuteRequest {
  @JsonProperty("code")

  private String code = null;

  /**
   * The programming language of the code
   */
  public enum LangEnum {
    C("c"),

    CPP("cpp"),

    D("d"),

    F90("f90"),

    GO("go"),

    JAVA("java"),

    JS("js"),

    PHP("php"),

    PY("py"),

    RS("rs"),

    TS("ts"),

    R("r");

    private String value;

    LangEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static LangEnum fromValue(String text) {
      for (LangEnum b : LangEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("lang")

  private LangEnum lang = null;

  @JsonProperty("args")
  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private Object args = null;

  @JsonProperty("user_id")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String userId = null;

  @JsonProperty("entity_id")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String entityId = null;

  @JsonProperty("files")
  @Valid
  private List<RequestFile> files = null;

  public ExecuteRequest code(String code) {

    this.code = code;
    return this;
  }

  /**
   * The source code to be executed
   * 
   * @return code
   **/

  @NotNull
  public String getCode() {
    return code;
  }

  public void setCode(String code) {

    this.code = code;
  }

  public ExecuteRequest lang(LangEnum lang) {

    this.lang = lang;
    return this;
  }

  /**
   * The programming language of the code
   * 
   * @return lang
   **/

  @NotNull
  public LangEnum getLang() {
    return lang;
  }

  public void setLang(LangEnum lang) {

    this.lang = lang;
  }

  public ExecuteRequest args(String args) {

    this.args = args;
    return this;
  }

  /**
   * Optional command line arguments to pass to the program
   * 
   * @return args
   **/

  public Object getArgs() {
    return args;
  }

  public void setArgs(Object args) {
    this.args = args;
  }

  public ExecuteRequest userId(String userId) {

    this.userId = userId;
    return this;
  }

  /**
   * Optional user identifier
   * 
   * @return userId
   **/

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public ExecuteRequest entityId(String entityId) {

    this.entityId = entityId;
    return this;
  }

  /**
   * Optional assistant/agent identifier for file sharing and reference. Must be a
   * valid nanoid-compatible string.
   * 
   * @return entityId
   **/

  @Pattern(regexp = "^[A-Za-z0-9_-]+$")
  @Size(max = 40)
  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public ExecuteRequest files(List<RequestFile> files) {

    this.files = files;
    return this;
  }

  public ExecuteRequest addFilesItem(RequestFile filesItem) {
    if (this.files == null) {
      this.files = new ArrayList<>();
    }
    this.files.add(filesItem);
    return this;
  }

  /**
   * Array of file references to be used during execution
   * 
   * @return files
   **/

  @Valid
  public List<RequestFile> getFiles() {
    return files;
  }

  public void setFiles(List<RequestFile> files) {
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
    ExecuteRequest requestBody = (ExecuteRequest) o;
    return Objects.equals(this.code, requestBody.code) &&
        Objects.equals(this.lang, requestBody.lang) &&
        Objects.equals(this.args, requestBody.args) &&
        Objects.equals(this.userId, requestBody.userId) &&
        Objects.equals(this.entityId, requestBody.entityId) &&
        Objects.equals(this.files, requestBody.files);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, lang, args, userId, entityId, files);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RequestBody {\n");

    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    lang: ").append(toIndentedString(lang)).append("\n");
    sb.append("    args: ").append(toIndentedString(args)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
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
