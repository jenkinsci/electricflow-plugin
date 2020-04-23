package org.jenkinsci.plugins.electricflow.models;

import net.sf.json.JSONObject;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;

public class JenkinsBuildDetail {

  private String buildName;
  private String projectName;
  private String releaseName;
  private String flowRuntimeId;

  private CloudBeesFlowBuildData jenkinsData;

  private BuildTriggerSource buildTriggerSource = BuildTriggerSource.JENKINS;
  private JenkinsBuildAssociationType associationType = JenkinsBuildAssociationType.ATTACHED;

  public enum BuildTriggerSource {
    JENKINS,
    FLOW
  }

  public enum JenkinsBuildAssociationType {
    ATTACHED,
    TRIGGERED_BY_FLOW,
    TRIGGERED_BY_JENKINS,
  }

  public JenkinsBuildDetail(CloudBeesFlowBuildData jenkinsData, String projectName){
    this.jenkinsData = jenkinsData;
    this.buildName = jenkinsData.getDisplayName();
    this.projectName = projectName;
  }

  public JSONObject toJsonObject() {
    this.validate();

    JSONObject jsonObject = new JSONObject();

    if (buildName == null){
      buildName = jenkinsData.getDisplayName();
    }

    jsonObject.put("buildName", this.getBuildName());
    jsonObject.put("projectName", this.getProjectName());
    jsonObject.put("jenkinsData", this.getJenkinsData().toJsonObject().toString());
    jsonObject.put("buildTriggerSource", this.getBuildTriggerSource());
    jsonObject.put("jenkinsBuildAssociationType", this.getAssociationType());

    if (this.flowRuntimeId != null) {
      jsonObject.put("flowRuntimeId", this.getFlowRuntimeId());
    } else if (this.projectName != null && this.releaseName != null) {
      jsonObject.put("releaseName", this.getReleaseName());
    }

    return jsonObject;
  }

  public void validate() throws RuntimeException{
    if (jenkinsData == null){
      throw new RuntimeException("Field 'CloudBeesFlowData jenkinsData' is not set up.");
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

  public JenkinsBuildDetail setBuildName(String buildName) {
    this.buildName = buildName;
    return this;
  }

  public String getProjectName() {
    return projectName;
  }

  public JenkinsBuildDetail setProjectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

  public String getReleaseName() {
    return releaseName;
  }

  public JenkinsBuildDetail setReleaseName(String releaseName) {
    this.releaseName = releaseName;
    return this;
  }

  public CloudBeesFlowBuildData getJenkinsData() {
    return jenkinsData;
  }

  public JenkinsBuildDetail setJenkinsData(
      CloudBeesFlowBuildData jenkinsData) {
    this.jenkinsData = jenkinsData;
    return this;
  }

  public String getBuildTriggerSource() {
    switch (this.buildTriggerSource) {
      case FLOW:
        return "Flow";
      case JENKINS:
        return "Jenkins";
    }
    return null;
  }

  public JenkinsBuildDetail setBuildTriggerSource(
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
      case TRIGGERED_BY_JENKINS:
        return "triggeredByJenkins";
    }
    return null;
  }

  public JenkinsBuildDetail setAssociationType(
      JenkinsBuildAssociationType associationType) {
    this.associationType = associationType;
    return this;
  }

  public String getFlowRuntimeId() {
    return flowRuntimeId;
  }

  public JenkinsBuildDetail setFlowRuntimeId(String flowRuntimeId) {
    this.flowRuntimeId = flowRuntimeId;
    return this;
  }
}
