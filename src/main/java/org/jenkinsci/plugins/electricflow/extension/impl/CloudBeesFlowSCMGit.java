package org.jenkinsci.plugins.electricflow.extension.impl;

import hudson.Extension;
import hudson.plugins.git.GitChangeSet;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowSCM;
import org.jenkinsci.plugins.variant.OptionalExtension;

// @Extension
@OptionalExtension(requirePlugins="git")
public class CloudBeesFlowSCMGit extends CloudBeesFlowSCM {

    public void populate (Object obj) {
        GitChangeSet object = (GitChangeSet) obj;
        this.setAuthor(object.getAuthorName());
        this.setAuthorEmail(object.getAuthorEmail());
        this.setCommitId(object.getCommitId());
        this.setCommitMessage(object.getComment());
        this.setTimestamp(object.getTimestamp());
        this.setScmType("git");
        // this.scmReportUrl = object.get
    }
    // returns true if changeset is GitChangeset or its subclass.
    public boolean isApplicable(Object object) {
        return object instanceof GitChangeSet;
    }
}
