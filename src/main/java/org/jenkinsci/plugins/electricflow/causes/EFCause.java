package org.jenkinsci.plugins.electricflow.causes;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class EFCause extends hudson.model.Cause {
  @Exported public String flowRuntimeId = "";
  @Exported public String projectName = "";
  @Exported public String releaseName = "";
  @Exported public String flowRuntimeStateId = "";
  @Exported public String stageName = "";

  // @Exported
  public String getFlowRuntimeId() {
    return flowRuntimeId;
  }

  public void setFlowRuntimeId(String flowRuntimeId) {
    this.flowRuntimeId = flowRuntimeId;
  }

  // @Exported
  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  // @Exported
  public String getReleaseName() {
    return releaseName;
  }

  public void setReleaseName(String releaseName) {
    this.releaseName = releaseName;
  }

  public String getFlowRuntimeStateId() {
    return flowRuntimeStateId;
  }

  public void setFlowRuntimeStateId(String flowRuntimeStateId) {
    this.flowRuntimeStateId = flowRuntimeStateId;
  }

  public String getStageName() {
    return stageName;
  }

  public void setStageName(String stageName) {
    this.stageName = stageName;
  }

  public String getShortDescription() {
    StringBuilder shortDescription = new StringBuilder();
    shortDescription.append("CloudBees CD Triggered this build:");

    String flowRuntimeId = this.getFlowRuntimeId();
    String projectName = this.getProjectName();
    String releaseName = this.getReleaseName();
    String flowRuntimeStateId = this.getFlowRuntimeStateId();
    String stageName = this.getStageName();

    if (!isEmptyOrNullString(flowRuntimeId)) {
      shortDescription.append("<br/>");
      shortDescription.append("Flow Runtime ID: ").append(flowRuntimeId);
    }
    if (!isEmptyOrNullString(projectName)) {
      shortDescription.append("<br/>");
      shortDescription.append("Project Name: ").append(projectName);
    }
    if (!isEmptyOrNullString(releaseName)) {
      shortDescription.append("<br/>");
      shortDescription.append("Release Name: ").append(releaseName);
    }
    if (!isEmptyOrNullString(flowRuntimeStateId)) {
      shortDescription.append("<br/>");
      shortDescription.append("Flow Runtime State ID: ").append(flowRuntimeStateId);
    }
    if (!isEmptyOrNullString(stageName)) {
      shortDescription.append("<br/>");
      shortDescription.append("Stage Name: ").append(stageName);
    }
    return shortDescription.toString();
  }
  private static boolean isEmptyOrNullString(String str) {
    if (str == null) {
      return true;
    }
    return str.equals("");
  }
}
