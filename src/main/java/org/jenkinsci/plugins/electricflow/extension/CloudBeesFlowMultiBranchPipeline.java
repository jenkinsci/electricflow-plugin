package org.jenkinsci.plugins.electricflow.extension;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Run;
import jenkins.model.Jenkins;

public class CloudBeesFlowMultiBranchPipeline implements ExtensionPoint {
    protected String branchName = "";

    public CloudBeesFlowMultiBranchPipeline() {}
    public static CloudBeesFlowMultiBranchPipeline build(Run run) {
        CloudBeesFlowMultiBranchPipeline retval = new CloudBeesFlowMultiBranchPipeline();
        final Jenkins jenkins = Jenkins.get();
        final ExtensionList<CloudBeesFlowMultiBranchPipeline> makers = ExtensionList.lookup(CloudBeesFlowMultiBranchPipeline.class);
        for (CloudBeesFlowMultiBranchPipeline m : makers) {
            m.populate(run);
            if (m.getBranchName() != null && !m.getBranchName().equals("")) {
                retval.setBranchName(m.getBranchName());
            }
        }
        return retval;
    }
    // this method is required to use it in the optional extension
    public boolean isApplicable() {return false;}

    public String getBranchName() {
        return branchName;
    }
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }
    public void populate(Run run) {}
}
