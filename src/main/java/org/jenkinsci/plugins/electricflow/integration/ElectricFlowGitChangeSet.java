package org.jenkinsci.plugins.electricflow.integration;

import hudson.plugins.git.GitChangeSet;
import org.jenkinsci.plugins.variant.OptionalExtension;

@OptionalExtension(requirePlugins = "git")
public class ElectricFlowGitChangeSet extends ElectricFlowChangeSet {

    public void populate(Object obj) {
        GitChangeSet object = (GitChangeSet) obj;
        this.commitId = object.getCommitId();
        this.authorEmail = object.getAuthorEmail();
        this.authorName = object.getAuthorName();
        this.comments = object.getComment();
    }

    public boolean isApplicable(Object object) {
        return object instanceof GitChangeSet;
    }
}
