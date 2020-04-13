package org.jenkinsci.plugins.electricflow.causes;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class EFCause extends hudson.model.Cause {
    @Exported
    public String flowRuntimeId;
    @Exported
    public String projectName;
    @Exported
    public String releaseName;

    @Exported
    public String getFlowRuntimeId() {
        return flowRuntimeId;
    }

    @Exported
    public void setFlowRuntimeId(String flowRuntimeId) {
        this.flowRuntimeId = flowRuntimeId;
    }

    @Exported
    public String getProjectName() {
        return projectName;
    }

    @Exported
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Exported
    public String getReleaseName() {
        return releaseName;
    }

    @Exported
    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }


    public String getShortDescription() {
        return "EF Data";
    }
}
