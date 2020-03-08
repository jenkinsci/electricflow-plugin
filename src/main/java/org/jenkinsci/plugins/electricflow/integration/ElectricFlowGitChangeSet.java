package org.jenkinsci.plugins.electricflow.integration;

import hudson.Extension;
import hudson.plugins.git.GitChangeSet;
import org.jenkinsci.plugins.variant.OptionalExtension;

// @Extension
@OptionalExtension(requirePlugins="git")
public class ElectricFlowGitChangeSet extends ElectricFlowChangeSet {
    public void populate (Object obj) {
        GitChangeSet object = (GitChangeSet) obj;
        // ElectricFlowGitChangeSet git = new ElectricFlowGitChangeSet();
        this.commitId = object.getCommitId();
        this.authorEmail = object.getAuthorEmail();
        this.authorName = object.getAuthorName();
        this.comments = object.getComment();
        // return git;
    }
    public boolean isApplicable(Object object) {
        return object instanceof GitChangeSet;
    }
}
