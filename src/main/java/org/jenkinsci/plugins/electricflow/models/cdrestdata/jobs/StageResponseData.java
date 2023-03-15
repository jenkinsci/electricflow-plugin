package org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StageResponseData {

  @JsonProperty private String stageId;
  @JsonProperty private String stageName;
  @JsonProperty private int index;

  public String getStageId() {
    return stageId;
  }

  public void setStageId(String stageId) {
    this.stageId = stageId;
  }

  public String getStageName() {
    return stageName;
  }

  public void setStageName(String stageName) {
    this.stageName = stageName;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  public String toString() {
    return "CD Pipeline Stage Response Data: {"
        + "stageId="
        + stageId
        + ", stageName="
        + stageName
        + ", index="
        + index
        + '}';
  }
}
