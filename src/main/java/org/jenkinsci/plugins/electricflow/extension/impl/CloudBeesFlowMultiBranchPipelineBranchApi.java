package org.jenkinsci.plugins.electricflow.extension.impl;

import hudson.model.ItemGroup;
import hudson.model.Run;
import jenkins.branch.Branch;
import jenkins.branch.BranchProjectFactory;
import jenkins.branch.MultiBranchProject;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowMultiBranchPipeline;
import org.jenkinsci.plugins.variant.OptionalExtension;

@OptionalExtension(requirePlugins = {"branch-api", "scm-api"})
public class CloudBeesFlowMultiBranchPipelineBranchApi extends CloudBeesFlowMultiBranchPipeline {
    public CloudBeesFlowMultiBranchPipelineBranchApi() {
        this.scmBranchName = "";
    }

    public CloudBeesFlowMultiBranchPipeline generate(Run<?, ?> run) {
        ItemGroup parent = run.getParent().getParent();
        CloudBeesFlowMultiBranchPipeline retval = new CloudBeesFlowMultiBranchPipeline();
        // CEV-25644
        // Explicit check for base class has been added to not populate any data if
        // job is not a member of a MultiBranch project.
        if (!(parent instanceof jenkins.branch.MultiBranchProject)) {
            return retval;
        }
        // CEV-25644

        BranchProjectFactory projectFactory = ((MultiBranchProject) parent).getProjectFactory();
        if (projectFactory.isProject(run.getParent())) {
            Branch branch = projectFactory.getBranch(run.getParent());
            SCMHead head = branch.getHead();
            if (head instanceof ChangeRequestSCMHead) {
                // This logic will be executed if we're in pull request.
                SCMHead target = ((ChangeRequestSCMHead) head).getTarget();
                String targetBranchName = target.getName();
                retval.setScmBranchName(targetBranchName);
            } else {
                retval.setScmBranchName(head.getName());
            }
        }
        return retval;
    }
    //    public void populate(Run<?, ?> run) {
    //        ItemGroup parent = run.getParent().getParent();
    //        // CEV-25644
    //        // Explicit check for base class has been added to not populate any data if
    //        // job is not a member of a MultiBranch project.
    //        if (!(parent instanceof jenkins.branch.MultiBranchProject)) {
    //            return;
    //        }
    //        // CEV-25644
    //
    //        BranchProjectFactory projectFactory = ((MultiBranchProject) parent).getProjectFactory();
    //        if (projectFactory.isProject(run.getParent())) {
    //            Branch branch = projectFactory.getBranch(run.getParent());
    //            SCMHead head = branch.getHead();
    //            if (head instanceof ChangeRequestSCMHead) {
    //                // This logic will be executed if we're in pull request.
    //                SCMHead target = ((ChangeRequestSCMHead) head).getTarget();
    //                String targetBranchName = target.getName();
    //                this.setScmBranchName(targetBranchName);
    //            }
    //            else {
    //                this.setScmBranchName(head.getName());
    //            }
    //        }
    //
    //    }
}
