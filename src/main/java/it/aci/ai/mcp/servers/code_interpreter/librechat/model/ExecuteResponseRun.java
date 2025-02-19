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
 * ExecuteResponseRun
 */
@Validated

public class ExecuteResponseRun {
  @JsonProperty("stdout")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String stdout = null;

  @JsonProperty("stderr")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String stderr = null;

  @JsonProperty("code")

  private Integer code = null;

  @JsonProperty("signal")

  private String signal = null;

  @JsonProperty("output")

  @JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL) // FAIL setting if the value is null
  private String output = null;

  @JsonProperty("memory")

  private Integer memory = null;

  @JsonProperty("message")

  private String message = null;

  @JsonProperty("status")

  private String status = null;

  @JsonProperty("cpu_time")

  private BigDecimal cpuTime = null;

  @JsonProperty("wall_time")

  private BigDecimal wallTime = null;

  public ExecuteResponseRun stdout(String stdout) {

    this.stdout = stdout;
    return this;
  }

  /**
   * Get stdout
   * 
   * @return stdout
   **/

  public String getStdout() {
    return stdout;
  }

  public void setStdout(String stdout) {
    this.stdout = stdout;
  }

  public ExecuteResponseRun stderr(String stderr) {

    this.stderr = stderr;
    return this;
  }

  /**
   * Get stderr
   * 
   * @return stderr
   **/

  public String getStderr() {
    return stderr;
  }

  public void setStderr(String stderr) {
    this.stderr = stderr;
  }

  public ExecuteResponseRun code(Integer code) {

    this.code = code;
    return this;
  }

  /**
   * Get code
   * 
   * @return code
   **/

  public Integer getCode() {

    return code;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public ExecuteResponseRun signal(String signal) {

    this.signal = signal;
    return this;
  }

  /**
   * Get signal
   * 
   * @return signal
   **/

  public String getSignal() {

    return signal;
  }

  public void setSignal(String signal) {
    this.signal = signal;
  }

  public ExecuteResponseRun output(String output) {

    this.output = output;
    return this;
  }

  /**
   * Get output
   * 
   * @return output
   **/

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public ExecuteResponseRun memory(Integer memory) {

    this.memory = memory;
    return this;
  }

  /**
   * Get memory
   * 
   * @return memory
   **/

  public Integer getMemory() {

    return memory;
  }

  public void setMemory(Integer memory) {
    this.memory = memory;
  }

  public ExecuteResponseRun message(String message) {

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

  public ExecuteResponseRun status(String status) {

    this.status = status;
    return this;
  }

  /**
   * Get status
   * 
   * @return status
   **/

  public String getStatus() {

    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public ExecuteResponseRun cpuTime(BigDecimal cpuTime) {

    this.cpuTime = cpuTime;
    return this;
  }

  /**
   * Get cpuTime
   * 
   * @return cpuTime
   **/

  @Valid
  public BigDecimal getCpuTime() {

    return cpuTime;
  }

  public void setCpuTime(BigDecimal cpuTime) {
    this.cpuTime = cpuTime;
  }

  public ExecuteResponseRun wallTime(BigDecimal wallTime) {

    this.wallTime = wallTime;
    return this;
  }

  /**
   * Get wallTime
   * 
   * @return wallTime
   **/

  @Valid
  public BigDecimal getWallTime() {

    return wallTime;
  }

  public void setWallTime(BigDecimal wallTime) {
    this.wallTime = wallTime;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExecuteResponseRun executeResponseRun = (ExecuteResponseRun) o;
    return Objects.equals(this.stdout, executeResponseRun.stdout) &&
        Objects.equals(this.stderr, executeResponseRun.stderr) &&
        Objects.equals(this.code, executeResponseRun.code) &&
        Objects.equals(this.signal, executeResponseRun.signal) &&
        Objects.equals(this.output, executeResponseRun.output) &&
        Objects.equals(this.memory, executeResponseRun.memory) &&
        Objects.equals(this.message, executeResponseRun.message) &&
        Objects.equals(this.status, executeResponseRun.status) &&
        Objects.equals(this.cpuTime, executeResponseRun.cpuTime) &&
        Objects.equals(this.wallTime, executeResponseRun.wallTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stdout, stderr, code, signal, output, memory, message, status, cpuTime, wallTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExecuteResponseRun {\n");

    sb.append("    stdout: ").append(toIndentedString(stdout)).append("\n");
    sb.append("    stderr: ").append(toIndentedString(stderr)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    signal: ").append(toIndentedString(signal)).append("\n");
    sb.append("    output: ").append(toIndentedString(output)).append("\n");
    sb.append("    memory: ").append(toIndentedString(memory)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    cpuTime: ").append(toIndentedString(cpuTime)).append("\n");
    sb.append("    wallTime: ").append(toIndentedString(wallTime)).append("\n");
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
