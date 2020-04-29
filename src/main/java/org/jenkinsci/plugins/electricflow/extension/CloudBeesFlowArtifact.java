package org.jenkinsci.plugins.electricflow.extension;

import hudson.model.Run.Artifact;
import net.sf.json.JSONObject;

public class CloudBeesFlowArtifact {

  protected String relativePath;
  protected String displayPath;
  protected String name;
  protected String href;
  protected String length;
  protected long size;
  protected String artifactName;
  protected String artifactVersion;
  protected String artifactVersionName;
  protected String artifactUrl;
  protected String repositoryName;
  protected String repositoryType;

  public CloudBeesFlowArtifact() {}

  public static CloudBeesFlowArtifact build(
      Artifact artifact, ArtifactUploadData artifactUploadData) {
    CloudBeesFlowArtifact cloudBeesFlowArtifact = new CloudBeesFlowArtifact();

    cloudBeesFlowArtifact.setRelativePath(artifact.relativePath);
    cloudBeesFlowArtifact.setDisplayPath(artifact.getDisplayPath());
    cloudBeesFlowArtifact.setName(artifact.getFileName());
    cloudBeesFlowArtifact.setHref(artifact.getHref());
    cloudBeesFlowArtifact.setLength(artifact.getLength());
    cloudBeesFlowArtifact.setSize(artifact.getFileSize());

    if (artifactUploadData != null) {
      cloudBeesFlowArtifact.setArtifactName(artifactUploadData.getArtifactName());
      cloudBeesFlowArtifact.setArtifactVersion(artifactUploadData.getArtifactVersion());
      cloudBeesFlowArtifact.setArtifactVersionName(artifactUploadData.getArtifactVersionName());
      cloudBeesFlowArtifact.setArtifactUrl(artifactUploadData.getArtifactUrl());
      cloudBeesFlowArtifact.setRepositoryName(artifactUploadData.getRepositoryName());
      cloudBeesFlowArtifact.setRepositoryType(artifactUploadData.getRepositoryType());
    }

    return cloudBeesFlowArtifact;
  }

  public JSONObject toJsonObject() {
    JSONObject json = new JSONObject();

    if (this.getDisplayPath() != null) {
      json.put("displayPath", this.getDisplayPath());
    }

    if (this.getName() != null) {
      json.put("name", this.getName());
    }

    if (this.getHref() != null) {
      json.put("href", this.getHref());
    }

    if (this.getSize() > 0) {
      json.put("size", this.getSize());
    }

    if (this.getArtifactName() != null) {
      json.put("artifactName", this.getArtifactName());
    }

    if (this.getArtifactVersion() != null) {
      json.put("artifactVersion", this.getArtifactVersion());
    }

    if (this.getArtifactVersionName() != null) {
      json.put("artifactVersionName", this.getArtifactVersionName());
    }

    if (this.getArtifactUrl() != null) {
      json.put("artifactUrl", this.getArtifactUrl());
    }

    if (this.getRepositoryName() != null) {
      json.put("repositoryName", this.getRepositoryName());
    }

    if (this.getRepositoryType() != null) {
      json.put("repositoryType", this.getRepositoryType());
    }

    return json;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  public String getDisplayPath() {
    return displayPath;
  }

  public void setDisplayPath(String displayPath) {
    this.displayPath = displayPath;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHref() {
    return href;
  }

  public void setHref(String href) {
    this.href = href;
  }

  public String getLength() {
    return length;
  }

  public void setLength(String length) {
    this.length = length;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

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

  public String getArtifactVersionName() {
    return artifactVersionName;
  }

  public void setArtifactVersionName(String artifactVersionName) {
    this.artifactVersionName = artifactVersionName;
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
