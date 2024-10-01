package org.jenkinsci.plugins.electricflow.action;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Action;
import hudson.model.Run;
import org.jenkinsci.plugins.electricflow.Credential;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

// TODO: Since we have now 2 classes that are doing pretty match the same
@ExportedBean
public class CloudBeesCDPBABuildDetails implements Action {
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

    @Exported
    public String configurationName = "";

    @Exported
    public CIBuildDetail.BuildTriggerSource triggerSource;

    @Exported
    public CIBuildDetail.BuildAssociationType buildAssociationType;

    private Credential overriddenCredential = null;

    public CloudBeesCDPBABuildDetails() {}

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

    public String getConfigurationName() {
        return configurationName;
    }

    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }

    public Credential getOverriddenCredential() {
        return overriddenCredential;
    }

    public void setOverriddenCredential(Credential overriddenCredential) {
        this.overriddenCredential = overriddenCredential;
    }

    public CIBuildDetail.BuildTriggerSource getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(CIBuildDetail.BuildTriggerSource triggerSource) {
        this.triggerSource = triggerSource;
    }

    public CIBuildDetail.BuildAssociationType getBuildAssociationType() {
        return buildAssociationType;
    }

    public void setBuildAssociationType(CIBuildDetail.BuildAssociationType buildAssociationType) {
        this.buildAssociationType = buildAssociationType;
    }

    public static void applyToRuntime(
            Run<?, ?> run,
            String configurationName,
            Credential credential,
            String flowRuntimeId,
            String flowRuntimeStateId,
            String projectName,
            String releaseName,
            String stageName,
            CIBuildDetail.BuildTriggerSource triggerSource,
            CIBuildDetail.BuildAssociationType buildAssociationType) {
        CloudBeesCDPBABuildDetails cdpbaBuildDetails = new CloudBeesCDPBABuildDetails();
        if (configurationName != null) {
            cdpbaBuildDetails.setConfigurationName(configurationName);
        }
        if (credential != null) {
            cdpbaBuildDetails.setOverriddenCredential(credential);
        }
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
        if (triggerSource != null) {
            cdpbaBuildDetails.setTriggerSource(triggerSource);
        }
        if (buildAssociationType != null) {
            cdpbaBuildDetails.setBuildAssociationType(buildAssociationType);
        }
        run.addAction(cdpbaBuildDetails);
    }

    Object readResolve() {
        // Required for backward compatibility of the serialized data
        if (this.buildAssociationType == CIBuildDetail.BuildAssociationType.TRIGGERED_BY_FLOW) {
            this.buildAssociationType = CIBuildDetail.BuildAssociationType.TRIGGERED_BY_CD;
        }
        if (this.triggerSource == CIBuildDetail.BuildTriggerSource.FLOW) {
            this.triggerSource = CIBuildDetail.BuildTriggerSource.CD;
        }
        return this;
    }
}
