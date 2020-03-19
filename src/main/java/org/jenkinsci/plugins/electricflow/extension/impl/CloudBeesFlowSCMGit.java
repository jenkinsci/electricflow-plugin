package org.jenkinsci.plugins.electricflow.extension.impl;

import hudson.Extension;
import hudson.plugins.git.GitChangeSet;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowSCM;
import org.jenkinsci.plugins.variant.OptionalExtension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



// @Extension
@OptionalExtension(requirePlugins="git")
public class CloudBeesFlowSCMGit extends CloudBeesFlowSCM {

    // Adding some debugging
    private static final Log log = LogFactory.getLog(CloudBeesFlowSCMGit.class);

    public void populate (Object obj) {
        GitChangeSet object = (GitChangeSet) obj;
        this.setAuthor(object.getAuthorName());
        
        if (log.isDebugEnabled()) {
            log.debug("CloudBeesFlowSCMGit:: Authorname is" + object.getAuthorName());
        }
        
        this.setAuthorEmail(object.getAuthorEmail());
        this.setCommitId(object.getCommitId());
        this.setCommitMessage(object.getComment());

        if (log.isDebugEnabled()) {
            log.debug("CloudBeesFlowSCMGit:: Commit Message is" + object.getComment());
        }
       
        this.setTimestamp(object.getTimestamp());
        this.setScmType("git");
        // this.scmReportUrl = object.get
    }
    // returns true if changeset is GitChangeset or its subclass.
    public boolean isApplicable(Object object) {
        return object instanceof GitChangeSet;
    }
}
