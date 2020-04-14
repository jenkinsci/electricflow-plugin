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
    public String getReleaseName() {
        return releaseName;
    }
    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }


    public String getShortDescription() {
        StringBuilder shortDescription = new StringBuilder();
        shortDescription.append("CloudBeesFlow Triggered this build:");
        // shortDescription.append(System.getProperty("<br/>"));

        if (this.getFlowRuntimeId() != null) {
            shortDescription.append("<br/>");
            shortDescription.append("Flow Runtime ID: " + this.getFlowRuntimeId());
            // shortDescription.append(System.getProperty("line.separator"));
        }
        if (this.getProjectName() != null) {
            shortDescription.append("<br/>");
            shortDescription.append("Release Project Name: " + this.getReleaseName());
            // shortDescription.append(System.getProperty("line.separator"));
        }
        if (this.getReleaseName() != null) {
            shortDescription.append("<br/>");
            shortDescription.append("Release Name: " + this.getReleaseName());
            // shortDescription.append(System.getProperty("line.separator"));
        }

        return shortDescription.toString();
    }
}
