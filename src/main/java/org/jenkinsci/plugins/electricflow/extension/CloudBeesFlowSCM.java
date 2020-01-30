package org.jenkinsci.plugins.electricflow.extension;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

public class CloudBeesFlowSCM implements ExtensionPoint {
    protected String scmReportUrl;
    protected String scmType;
    protected String commitId;
    protected long timestamp;
    protected String author;
    protected String authorEmail;
    protected String commitMessage;

    // constructor
    public CloudBeesFlowSCM() {}
    public static CloudBeesFlowSCM build (Object obj) {
        final Jenkins jenkins = Jenkins.get();
        if (jenkins != null) {
            ExtensionList.lookup(CloudBeesFlowSCM.class);

            final ExtensionList<CloudBeesFlowSCM> makers = ExtensionList.lookup(CloudBeesFlowSCM.class);
            for (CloudBeesFlowSCM m : makers) {
                System.out.println("Iterating through extensions");
                boolean applicable = m.isApplicable(obj);
                if (applicable) {
                    System.out.println("Applicable");
                    m.populate(obj);
                    return m;
                }
            }
        }
        return null;
    }
    // service methods
    // isApplicable() returns false because it will be implemented in subclasses
    public boolean isApplicable(Object object) {
        return false;
    }
    // populate
    public void populate(Object object) { }


    /* getters and setters */
    public String getScmReportUrl() {
        return scmReportUrl;
    }

    public void setScmReportUrl(String scmReportUrl) {
        this.scmReportUrl = scmReportUrl;
    }

    public String getScmType() {
        return scmType;
    }

    public void setScmType(String scmType) {
        this.scmType = scmType;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
}
