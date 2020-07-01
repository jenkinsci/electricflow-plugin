package org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs;

import static org.jenkinsci.plugins.electricflow.Utils.formatJsonOutput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import org.jenkinsci.plugins.electricflow.utils.JsonUtils.NumericBooleanDeserializer;
import org.jenkinsci.plugins.electricflow.utils.JsonUtils.NumericBooleanSerializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetPipelineRuntimeDetailsResponseData {

  @JsonProperty private String flowRuntimeId;

  @JsonProperty
  @JsonSerialize(using = NumericBooleanSerializer.class)
  @JsonDeserialize(using = NumericBooleanDeserializer.class)
  private Boolean completed;

  @JsonProperty private CdPipelineStatus status = CdPipelineStatus.unknown;
  private String content;

  public String getFlowRuntimeId() {
    return flowRuntimeId;
  }

  public void setFlowRuntimeId(String flowRuntimeId) {
    this.flowRuntimeId = flowRuntimeId;
  }

  public Boolean isCompleted() {
    return completed;
  }

  public void setCompleted(Boolean completed) {
    this.completed = completed;
  }

  public CdPipelineStatus getStatus() {
    return status;
  }

  public void setStatus(CdPipelineStatus status) {
    this.status = status;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public String toString() {
    if (getStatus() == CdPipelineStatus.unknown && getContent() != null) {
      try {
        return "CD Pipeline Runtime Details Response (unexpected json): "
            + formatJsonOutput(getContent());
      } catch (IOException e) {
        return "CD Pipeline Runtime Details Response (unexpected content): " + getContent();
      }
    }
    return "CD Pipeline Runtime Details Response Data: {"
        + "flowRuntimeId="
        + flowRuntimeId
        + ", completed="
        + completed
        + ", status="
        + status
        + '}';
  }
}
