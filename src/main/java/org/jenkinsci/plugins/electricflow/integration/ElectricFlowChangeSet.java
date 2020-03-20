package org.jenkinsci.plugins.electricflow.integration;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

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
    final Jenkins jenkins = Jenkins.get();
    if (jenkins != null) {
      ExtensionList.lookup(ElectricFlowChangeSet.class);
      // final ExtensionList<ElectricFlowChangeSet> makers =
      // jenkins.getExtensionList(ElectricFlowChangeSet.class);
      final ExtensionList<ElectricFlowChangeSet> makers =
          ExtensionList.lookup(ElectricFlowChangeSet.class);
      for (ElectricFlowChangeSet m : makers) {
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

  // default implementation of this method. Should return false if asked from this class object
  // directly.
  public boolean isApplicable(Object object) {
    return false;
  }

  public void populate(Object object) {}
  //    public static ExtensionList<Animal> all() {
  //        return Jenkins.getInstanceOrNull().getExtensionList(Animal.class); //
  // getActiveInstance() starting with Jenkins 1.590, else getInstance()
  //    }
}
