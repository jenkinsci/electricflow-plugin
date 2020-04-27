package org.jenkinsci.plugins.electricflow.causes;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class EFCause extends hudson.model.Cause {
    @Exported
    public String flowRuntimeId = "";
    @Exported
    public String projectName = "";
    @Exported
    public String releaseName = "";
    @Exported
    public String flowRuntimeStateId;
    @Exported
    public String stageName;

    // @Exported
    public String getFlowRuntimeId() {
        return flowRuntimeId;
    }
    public void setFlowRuntimeId(String flowRuntimeId) {
        this.flowRuntimeId = flowRuntimeId;
    }

    //@Exported
    public String getProjectName() {
        return projectName;
    }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    //@Exported
    public String getReleaseName() { return releaseName; }
    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    public String getFlowRuntimeStateId() { return flowRuntimeStateId; }
    public void setFlowRuntimeStateId(String flowRuntimeStateId) { this.flowRuntimeStateId = flowRuntimeStateId; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public String getShortDescription() {
        StringBuilder shortDescription = new StringBuilder();
        shortDescription.append("CloudBeesFlow Triggered this build:");
        // shortDescription.append(System.getProperty("<br/>"));
        String flowRuntimeId = this.getFlowRuntimeId();
        String projectName = this.getProjectName();
        String releaseName = this.getReleaseName();

        // String nullString = new String("null");
        String nullString = "null";
        // char[4] nullChars = "null";
        // TODO: Convert flowRuntimeId, projectName and releaseName retrieval to getters
        if (!flowRuntimeId.equals(nullString)) {
            shortDescription.append("<br/>");
            shortDescription.append("Flow Runtime ID: " + flowRuntimeId);
        }
        if (!projectName.equals(nullString)) {
            shortDescription.append("<br/>");
            shortDescription.append("Release Project Name: " + projectName);
        }
        if (!releaseName.equals(nullString)) {
            shortDescription.append("<br/>");
            shortDescription.append("Release Name: " + releaseName);
        }
        if (!this.getFlowRuntimeStateId().equals(nullString)) {
            shortDescription.append("<br/>");
            shortDescription.append("Flow Runtime State ID: " + this.getFlowRuntimeStateId());
        }
        if (!this.getStageName().equals(nullString)) {
            shortDescription.append("<br/>");
            shortDescription.append("Stage Name: " + this.getStageName());
        }
        return shortDescription.toString();
    }

//    public String getIconFileName() {
//
//    }
}
