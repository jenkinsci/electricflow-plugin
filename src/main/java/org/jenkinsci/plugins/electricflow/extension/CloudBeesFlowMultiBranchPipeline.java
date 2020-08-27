package org.jenkinsci.plugins.electricflow.extension;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Run;
import jenkins.model.Jenkins;

public class CloudBeesFlowMultiBranchPipeline implements ExtensionPoint {
    protected String scmBranchName = "";

    public CloudBeesFlowMultiBranchPipeline() {
        this.scmBranchName = "";
    }
    public static CloudBeesFlowMultiBranchPipeline build(Run run) {
        CloudBeesFlowMultiBranchPipeline retval = new CloudBeesFlowMultiBranchPipeline();
        final Jenkins jenkins = Jenkins.get();
        if (jenkins != null) {
            final ExtensionList<CloudBeesFlowMultiBranchPipeline> makers = ExtensionList.lookup(CloudBeesFlowMultiBranchPipeline.class);
            for (CloudBeesFlowMultiBranchPipeline m : makers) {
                m.populate(run);
                if (m.getScmBranchName() != null && !m.getScmBranchName().equals("")) {
                    retval.setScmBranchName(m.getScmBranchName());
                }
            }
        }
        return retval;
    }
    // this method is required to use it in the optional extension
    public boolean isApplicable() {return false;}

    public String getScmBranchName() {
        return scmBranchName;
    }
    public void setScmBranchName(String branchName) {
        this.scmBranchName = branchName;
    }
    public void populate(Run<?, ?> run) {}
}
