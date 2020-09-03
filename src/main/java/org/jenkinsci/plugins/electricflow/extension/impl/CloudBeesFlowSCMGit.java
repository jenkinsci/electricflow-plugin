package org.jenkinsci.plugins.electricflow.extension.impl;

import hudson.plugins.git.GitChangeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowSCM;
import org.jenkinsci.plugins.variant.OptionalExtension;

@OptionalExtension(requirePlugins = "git")
public class CloudBeesFlowSCMGit extends CloudBeesFlowSCM {

  private static final Log log = LogFactory.getLog(CloudBeesFlowSCMGit.class);

  public void populate(Object obj) {
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
  }
  public CloudBeesFlowSCM generate () {
    CloudBeesFlowSCM retval = new CloudBeesFlowSCM();

    // author
    if (this.getAuthor() != null) {
      retval.setAuthor(this.getAuthor());
    }

    // authorEmail
    if (this.getAuthorEmail() != null) {
      retval.setAuthorEmail(this.getAuthorEmail());
    }

    // commitMessage
    if (this.getCommitMessage() != null) {
      retval.setCommitMessage(this.getCommitMessage());
    }

    // scmReportUrl
    if (this.getScmReportUrl() != null) {
      retval.setScmReportUrl(this.getScmReportUrl());
    }

    // scmType
    if (this.getScmType() != null) {
      retval.setScmType(this.getScmType());
    }

    // commitId;
    if (this.getCommitId() != null) {
      retval.setCommitId(this.getCommitId());
    }

    // timestamp;
    retval.setTimestamp(this.getTimestamp());

    return retval;
  }
  public boolean isApplicable(Object object) {
    return object instanceof GitChangeSet;
  }
}
