package org.jenkinsci.plugins.electricflow.models;

import net.sf.json.JSONObject;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;

public class CIBuildDetail {

  private String buildName;
  private String projectName;
  private String releaseName;
  private String flowRuntimeId;

  private CloudBeesFlowBuildData buildData;

  private BuildTriggerSource buildTriggerSource = BuildTriggerSource.CI;
  private BuildAssociationType associationType = BuildAssociationType.ATTACHED;

  public enum BuildTriggerSource {
    CI,
    FLOW
  }

  public enum BuildAssociationType {
    ATTACHED,
    TRIGGERED_BY_FLOW,
    TRIGGERED_BY_CI,
  }

  public CIBuildDetail(CloudBeesFlowBuildData buildData, String projectName){
    this.buildData = buildData;
    this.buildName = buildData.getDisplayName();
    this.projectName = projectName;
  }

  public JSONObject toJsonObject() {
    this.validate();

    JSONObject jsonObject = new JSONObject();

    if (buildName == null){
      buildName = buildData.getDisplayName();
    }

    jsonObject.put("ciBuildDetailName", this.getBuildName());
    jsonObject.put("projectName", this.getProjectName());
    jsonObject.put("buildData", this.getBuildData().toJsonObject().toString());
    jsonObject.put("buildTriggerSource", this.getBuildTriggerSource());
    jsonObject.put("ciBuildAssociationType", this.getAssociationType());

    if (this.flowRuntimeId != null) {
      jsonObject.put("flowRuntimeId", this.getFlowRuntimeId());
    } else if (this.projectName != null && this.releaseName != null) {
      jsonObject.put("releaseName", this.getReleaseName());
    }

    return jsonObject;
  }

  public void validate() throws RuntimeException{
    if (buildData == null){
      throw new RuntimeException("Field 'CloudBeesFlowData buildData' is not set up.");
    }

    boolean hasValuesForReleaseAttach = (projectName != null && releaseName != null);
    boolean hasValuesForPipelineAttach = (flowRuntimeId != null);

    if (hasValuesForPipelineAttach && hasValuesForReleaseAttach){
      throw new RuntimeException(
          "Only one of 'flowRuntimeId' or 'projectName and releaseName' can be specified."
      );
    }
    else if (!hasValuesForPipelineAttach && !hasValuesForReleaseAttach){
      throw new RuntimeException(
          "One of 'flowRuntimeId' or 'projectName and releaseName' should be specified."
      );
    }
  }

  public String getBuildName() {
    return buildName;
  }

  public CIBuildDetail setBuildName(String buildName) {
    this.buildName = buildName;
    return this;
  }

  public String getProjectName() {
    return projectName;
  }

  public CIBuildDetail setProjectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

  public String getReleaseName() {
    return releaseName;
  }

  public CIBuildDetail setReleaseName(String releaseName) {
    this.releaseName = releaseName;
    return this;
  }

  public CloudBeesFlowBuildData getBuildData() {
    return buildData;
  }

  public CIBuildDetail setBuildData(
      CloudBeesFlowBuildData buildData) {
    this.buildData = buildData;
    return this;
  }

  public String getBuildTriggerSource() {
    switch (this.buildTriggerSource) {
      case FLOW:
        return "Flow";
      case CI:
        return "CI";
    }
    return null;
  }

  public CIBuildDetail setBuildTriggerSource(
      BuildTriggerSource buildTriggerSource) {
    this.buildTriggerSource = buildTriggerSource;
    return this;
  }

  public String getAssociationType() {
    switch (this.associationType) {
      case ATTACHED:
        return "attached";
      case TRIGGERED_BY_FLOW:
        return "triggeredByFlow";
      case TRIGGERED_BY_CI:
        return "triggeredByCI";
    }
    return null;
  }

  public CIBuildDetail setAssociationType(
      BuildAssociationType associationType) {
    this.associationType = associationType;
    return this;
  }

  public String getFlowRuntimeId() {
    return flowRuntimeId;
  }

  public CIBuildDetail setFlowRuntimeId(String flowRuntimeId) {
    this.flowRuntimeId = flowRuntimeId;
    return this;
  }
}
