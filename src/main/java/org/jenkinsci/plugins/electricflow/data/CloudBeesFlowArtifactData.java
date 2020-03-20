package org.jenkinsci.plugins.electricflow.data;

import hudson.model.Run;
import hudson.model.Run.Artifact;
import hudson.model.Run.ArtifactList;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowArtifact;

public class CloudBeesFlowArtifactData {

  private static final Log log = LogFactory.getLog(CloudBeesFlowArtifactData.class);

  private List<CloudBeesFlowArtifact> artifactData;

  public CloudBeesFlowArtifactData(Run<?,?> run) {
    this.artifactData = new ArrayList<>();
    ArtifactList obj = (ArtifactList) run.getArtifacts();
    for (Object o : obj) {
      Artifact row = (Artifact) o;
      CloudBeesFlowArtifact art = CloudBeesFlowArtifact.build(row);
      this.artifactData.add(art);
    }
  }

  public List<CloudBeesFlowArtifact> getArtifactData() {
    return artifactData;
  }

  public void setArtifactData(List<CloudBeesFlowArtifact> artifactData) {
    this.artifactData = artifactData;
  }
}
