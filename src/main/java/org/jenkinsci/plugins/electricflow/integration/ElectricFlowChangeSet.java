package org.jenkinsci.plugins.electricflow.integration;

import hudson.ExtensionList;
import hudson.ExtensionPoint;

public class ElectricFlowChangeSet implements ExtensionPoint {

    protected String authorName;
    protected String authorEmail;
    protected String commitId;
    protected String comments;

    public ElectricFlowChangeSet() {}

    public static ElectricFlowChangeSet getChangeset(Object obj) {
        return null;
    }

    public static ElectricFlowChangeSet getChangesetFromObject(Object obj) {
        ExtensionList.lookup(ElectricFlowChangeSet.class);
        ExtensionList<ElectricFlowChangeSet> makers = ExtensionList.lookup(ElectricFlowChangeSet.class);
        for (ElectricFlowChangeSet m : makers) {
            boolean applicable = m.isApplicable(obj);
            if (applicable) {
                m.populate(obj);
                return m;
            }
        }

        return null;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return this.authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getCommitId() {
        return this.commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getComments() {
        return this.comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isApplicable(Object object) {
        return false;
    }

    public void populate(Object object) {}
}
