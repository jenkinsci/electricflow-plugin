package org.jenkinsci.plugins.electricflow.data;

import hudson.model.Run;
import hudson.model.Run.Artifact;
import hudson.model.Run.ArtifactList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.electricflow.ArtifactUploadSummaryTextAction;
import org.jenkinsci.plugins.electricflow.extension.ArtifactUploadData;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowArtifact;

public class CloudBeesFlowArtifactData {

  // ~ Instance fields --------------------------------------------------------

  private List<CloudBeesFlowArtifact> artifactData;

  // ~ Constructors -----------------------------------------------------------

  public CloudBeesFlowArtifactData(Run<?, ?> run) {
    this.artifactData = new ArrayList<>();

    List<ArtifactUploadSummaryTextAction> artifactUploadSummaryTextActions =
        run.getActions(ArtifactUploadSummaryTextAction.class);
    ArtifactList artifactList = (ArtifactList) run.getArtifacts();

    Map<String, ArtifactUploadData> artifactUploadDataMap = new HashMap<>();

    for (ArtifactUploadSummaryTextAction artifactUploadSummaryTextAction :
        artifactUploadSummaryTextActions) {
      ArtifactUploadData artifactUploadData =
          artifactUploadSummaryTextAction.getArtifactUploadData();
      artifactUploadDataMap.put(artifactUploadData.getFilePath(), artifactUploadData);
    }

    for (Object artifactRow : artifactList) {
      Artifact artifact = (Artifact) artifactRow;
      CloudBeesFlowArtifact cloudBeesFlowArtifact;

      // we've found a match!
      if (artifactUploadDataMap.containsKey(artifact.getHref())) {
        cloudBeesFlowArtifact =
            CloudBeesFlowArtifact.build(artifact, artifactUploadDataMap.get(artifact.getHref()));
      } else {
        cloudBeesFlowArtifact =
            CloudBeesFlowArtifact.build(
                artifact, null /* No matching artifact published to flow */);
      }

      this.artifactData.add(cloudBeesFlowArtifact);
    }
  }

  // ~ Methods ----------------------------------------------------------------

  public List<CloudBeesFlowArtifact> getArtifactData() {
    return artifactData;
  }

  public void setArtifactData(List<CloudBeesFlowArtifact> artifactData) {
    this.artifactData = artifactData;
  }
}
