package org.jenkinsci.plugins.electricflow.extension;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CloudBeesFlowSCM implements ExtensionPoint {
  private static final Log log = LogFactory.getLog(CloudBeesFlowSCM.class);

  protected String scmReportUrl;
  protected String scmType;
  protected String commitId;
  protected long timestamp;
  protected String author;
  protected String authorEmail;
  protected String commitMessage;

  // constructor
  public CloudBeesFlowSCM() {}

  public static CloudBeesFlowSCM build(Object obj) {
    final Jenkins jenkins = Jenkins.get();
    if (jenkins != null) {
      ExtensionList.lookup(CloudBeesFlowSCM.class);

      final ExtensionList<CloudBeesFlowSCM> makers = ExtensionList.lookup(CloudBeesFlowSCM.class);
      for (CloudBeesFlowSCM m : makers) {

        if (log.isDebugEnabled()) {
          log.debug("CloudBeesFlowSCM:: Iterating through extensions");
        }

        boolean applicable = m.isApplicable(obj);
        if (applicable) {

          if (log.isDebugEnabled()) {
            log.debug("CloudBeesFlowSCM:: Applicable");
          }

          m.populate(obj);
          return m;
        }
      }
    }
    return null;
  }

  public JSONObject toJsonObject() {
    JSONObject json = new JSONObject();

    if (this.getTimestamp() > 0) {
      json.put("timestamp", this.getTimestamp());
    }

    if (this.getScmReportUrl() != null) {
      json.put("scmRepoUrl", this.getScmReportUrl());
    }
    if (this.getScmType() != null) {
      json.put("scmType", this.getScmType());
    }
    if (this.getCommitId() != null) {
      json.put("commitId", this.getCommitId());
    }
    if (this.getAuthor() != null) {
      json.put("author", this.getAuthor());
    }
    if (this.getAuthorEmail() != null) {
      json.put("authorEmail", this.getAuthorEmail());
    }
    if (this.getCommitMessage() != null) {
      json.put("commitMessage", this.getCommitMessage());
    }

    return json;
  }
  // service methods
  // isApplicable() returns false because it will be implemented in subclasses
  public boolean isApplicable(Object object) {
    return false;
  }
  // populate
  public void populate(Object object) {}

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
