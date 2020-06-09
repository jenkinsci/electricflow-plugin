package org.jenkinsci.plugins.electricflow.models;

import net.sf.json.JSONObject;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;

public class CIBuildDetail {

  private String buildName;
  private String projectName;

  // CIBuildDetail attached to a release
  private String releaseName;

  // CIBuildDetail attached to a pipeline run
  private String flowRuntimeId;

  // CIBuildDetail attached to a specific task
  private String stageName;
  private String flowRuntimeStateId;

  private CloudBeesFlowBuildData buildData;

  // Defaults
  private BuildTriggerSource buildTriggerSource = BuildTriggerSource.CI;
  private BuildAssociationType associationType = BuildAssociationType.ATTACHED;

  public CIBuildDetail(CloudBeesFlowBuildData buildData, String projectName) {
    this.setBuildData(buildData);
    this.setBuildName(buildData.getDisplayName());
    this.setProjectName(projectName);
  }

  public JSONObject toJsonObject() {
    validate();

    JSONObject jsonObject = new JSONObject();

    if (buildName == null) {
      // buildName = buildData.getDisplayName();
      this.setBuildName(buildData.getDisplayName());
    }

    jsonObject.put("ciBuildDetailName", getBuildName());
    jsonObject.put("projectName", getProjectName());
    jsonObject.put("buildData", getBuildData().toJsonObject().toString());
    jsonObject.put("buildTriggerSource", getBuildTriggerSource());
    jsonObject.put("ciBuildAssociationType", getAssociationType());

    if (flowRuntimeId != null) {
      jsonObject.put("flowRuntimeId", getFlowRuntimeId());

      if (this.getStageName() != null && this.getFlowRuntimeStateId() != null){
        jsonObject.put("stageName", this.getStageName());
        jsonObject.put("flowRuntimeStateId", this.getFlowRuntimeStateId());
      }

    } else if (this.getProjectName() != null && this.getReleaseName() != null) {
      jsonObject.put("releaseName", this.getReleaseName());
    }

    return jsonObject;
  }

  public void validate() throws RuntimeException {
    if (this.getBuildData() == null) {
      throw new RuntimeException("Field 'CloudBeesFlowData buildData' is not set up.");
    }

    boolean hasValuesForReleaseAttach = (this.getProjectName() != null && this.getReleaseName() != null);
    boolean hasValuesForPipelineAttach = (this.getFlowRuntimeId() != null);

    if (hasValuesForPipelineAttach && hasValuesForReleaseAttach) {
      throw new RuntimeException(
          "Only one of 'flowRuntimeId' or 'projectName and releaseName' can be specified.");
    } else if (!hasValuesForPipelineAttach && !hasValuesForReleaseAttach) {
      throw new RuntimeException(
          "One of 'flowRuntimeId' or 'projectName and releaseName' should be specified.");
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

  public CIBuildDetail setBuildData(CloudBeesFlowBuildData buildData) {
    this.buildData = buildData;
    return this;
  }

  public String getBuildTriggerSource() {
    switch (this.buildTriggerSource) {
      case FLOW:
        return "CD";
      case CI:
        return "CI";
    }
    return null;
  }

  public CIBuildDetail setBuildTriggerSource(BuildTriggerSource buildTriggerSource) {
    this.buildTriggerSource = buildTriggerSource;
    return this;
  }

  public String getAssociationType() {
    switch (this.associationType) {
      case ATTACHED:
        return "attached";
      case TRIGGERED_BY_FLOW:
        return "triggeredByCD";
      case TRIGGERED_BY_CI:
        return "triggeredByCI";
    }
    return null;
  }

  public CIBuildDetail setAssociationType(BuildAssociationType associationType) {
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

  public String getStageName() {
    return stageName;
  }

  public CIBuildDetail setStageName(String stageName) {
    this.stageName = stageName;
    return this;
  }

  public String getFlowRuntimeStateId() {
    return flowRuntimeStateId;
  }

  public CIBuildDetail setFlowRuntimeStateId(String flowRuntimeStateId) {
    this.flowRuntimeStateId = flowRuntimeStateId;
    return this;
  }

  public enum BuildTriggerSource {
    CI,
    FLOW
  }

  public enum BuildAssociationType {
    ATTACHED,
    TRIGGERED_BY_FLOW,
    TRIGGERED_BY_CI,
  }
}
