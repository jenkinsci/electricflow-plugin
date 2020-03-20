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

  public CloudBeesFlowArtifact() {}

  public static CloudBeesFlowArtifact build(Artifact obj) {
    CloudBeesFlowArtifact cloudBeesFlowArtifact = new CloudBeesFlowArtifact();

    cloudBeesFlowArtifact.setDisplayPath(obj.getDisplayPath());
    // todo: switch to relativePath
    cloudBeesFlowArtifact.setRelativePath(obj.getDisplayPath());
    cloudBeesFlowArtifact.setName(obj.getFileName());
    cloudBeesFlowArtifact.setHref(obj.getHref());
    cloudBeesFlowArtifact.setLength(obj.getLength());
    cloudBeesFlowArtifact.setSize(obj.getFileSize());

    return cloudBeesFlowArtifact;
  }

  public long getSize() {
    return size;
  };

  public void setSize(long size) {
    this.size = size;
  }

  public JSONObject toJsonObject() {
    JSONObject json = new JSONObject();

    if (this.getDisplayPath() != null) {
      json.put("displayPath", this.getDisplayPath());
    }

    json.put("repositoryType", "Flow Artifact Repository");

    if (this.getName() != null) {
      json.put("name", this.getName());
    }
    if (this.getHref() != null) {
      json.put("href", this.getHref());
    }
    if (this.getSize() > 0) {
      json.put("size", this.getSize());
    }

    // Currently hardcoded. This will be fixed soon. TODO: remove them
    json.put("artifactName", "com.demo:helloworld");
    json.put("artifactVersion", "1.0-SNAPSHOT");
    json.put("repositoryName", "default");
    json.put(
        "url",
        "https://35.230.91.86/commander/link/artifactVersionDetails/artifactVersions/com.demo%3Ahelloworld%3A1.0-SNAPSHOT?s=Artifacts&ss=Artifacts");

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
}
