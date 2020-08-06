package org.jenkinsci.plugins.electricflow.action;

import hudson.model.Action;
import hudson.model.Run;
import javax.annotation.CheckForNull;
import org.jenkinsci.plugins.electricflow.causes.EFCause;
import org.kohsuke.stapler.export.Exported;

// TODO: Since we have now 2 classes that are doing pretty match the same
public class CloudBeesCDPBABuildDetails implements Action {
  @Exported public String flowRuntimeId = "";
  @Exported public String projectName = "";
  @Exported public String releaseName = "";
  @Exported public String flowRuntimeStateId = "";
  @Exported public String stageName = "";

  public  CloudBeesCDPBABuildDetails() { }
  @CheckForNull
  @Override
  public String getIconFileName() {
    return null;
  }

  @CheckForNull
  @Override
  public String getDisplayName() {
    return null;
  }

  @CheckForNull
  @Override
  public String getUrlName() {
    return null;
  }

  public String getFlowRuntimeId() {
    return flowRuntimeId;
  }

  public void setFlowRuntimeId(String flowRuntimeId) {
    this.flowRuntimeId = flowRuntimeId;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

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
//  public EFCause newEFCause() {
//    EFCause efCause = new EFCause();
//    if (this.getFlowRuntimeId() != null) {
//      efCause.setFlowRuntimeId(this.getFlowRuntimeId());
//    }
//    if (this.getFlowRuntimeStateId() != null) {
//      efCause.setFlowRuntimeStateId(this.getFlowRuntimeStateId());
//    }
//    if (this.getProjectName() != null) {
//      efCause.setProjectName(this.getProjectName());
//    }
//    if (this.getReleaseName() != null) {
//      efCause.setReleaseName(this.getReleaseName());
//    }
//    if (this.getStageName() != null) {
//      efCause.setStageName(this.getStageName());
//    }
//    return efCause;
//  }
  public static void applyToRuntime(
          Run<?, ?> run,
          String flowRuntimeId,
          String flowRuntimeStateId,
          String projectName,
          String releaseName,
          String stageName
  ) {
    CloudBeesCDPBABuildDetails cdpbaBuildDetails = new CloudBeesCDPBABuildDetails();
    if (flowRuntimeId != null) {
      cdpbaBuildDetails.setFlowRuntimeId(flowRuntimeId);
    }
    if (flowRuntimeStateId != null) {
      cdpbaBuildDetails.setFlowRuntimeStateId(flowRuntimeStateId);
    }
    if (projectName != null) {
      cdpbaBuildDetails.setProjectName(projectName);
    }
    if (releaseName != null) {
      cdpbaBuildDetails.setReleaseName(releaseName);
    }
    if (stageName != null) {
      cdpbaBuildDetails.setStageName(stageName);
    }

    run.addAction(cdpbaBuildDetails);
  }
}
