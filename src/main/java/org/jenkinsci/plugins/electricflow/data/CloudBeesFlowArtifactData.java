package org.jenkinsci.plugins.electricflow.data;

import hudson.model.Run;
import hudson.model.Run.Artifact;
import hudson.model.Run.ArtifactList;
import java.util.ArrayList;
import java.util.List;
import org.jenkinsci.plugins.electricflow.ArtifactUploadSummaryTextAction;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowArtifact;

public class CloudBeesFlowArtifactData {

  private List<CloudBeesFlowArtifact> artifactData;

  public CloudBeesFlowArtifactData(Run<?, ?> run) {
    this.artifactData = new ArrayList<>();

    ArtifactUploadSummaryTextAction artifactUploadSummaryTextAction =
        run.getAction(ArtifactUploadSummaryTextAction.class);


    ArtifactList artifactList = (ArtifactList) run.getArtifacts();

    for (Object artifactRow : artifactList) {
      Artifact artifact = (Artifact) artifactRow;
      CloudBeesFlowArtifact art =
          CloudBeesFlowArtifact.build(
              artifact, artifactUploadSummaryTextAction.getArtifactUploadData());
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
