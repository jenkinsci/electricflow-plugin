package org.jenkinsci.plugins.electricflow.extension;

public class ArtifactUploadData {

  protected String artifactName;
  protected String artifactVersion;
  protected String artifactUrl;
  protected String repositoryName;
  protected String repositoryType;

  public String getArtifactName() {
    return artifactName;
  }

  public void setArtifactName(String artifactName) {
    this.artifactName = artifactName;
  }

  public String getArtifactVersion() {
    return artifactVersion;
  }

  public void setArtifactVersion(String artifactVersion) {
    this.artifactVersion = artifactVersion;
  }

  public String getArtifactUrl() {
    return artifactUrl;
  }

  public void setArtifactUrl(String artifactUrl) {
    this.artifactUrl = artifactUrl;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getRepositoryType() {
    return repositoryType;
  }

  public void setRepositoryType(String repositoryType) {
    this.repositoryType = repositoryType;
  }
}
