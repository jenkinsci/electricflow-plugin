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
    public String flowRuntimeStateId = "";

    @Exported
    public String stageName = "";

    final String LAUNCHED_BY_CD_TEXT = "Launched by CloudBees CD";
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

    @Override
    public String getShortDescription() {
        return "CloudBees CD Triggered this build";
    }

    public String getLaunchedByText() {
        return this.LAUNCHED_BY_CD_TEXT;
    }

    private static boolean isEmptyOrNullString(String str) {
        if (str == null) {
            return true;
        }
        return str.equals("");
    }
}
