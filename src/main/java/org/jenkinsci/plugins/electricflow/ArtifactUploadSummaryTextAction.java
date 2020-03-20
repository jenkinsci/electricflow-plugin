package org.jenkinsci.plugins.electricflow;

import hudson.model.Run;
import java.util.ArrayList;
import java.util.List;
import org.jenkinsci.plugins.electricflow.extension.ArtifactUploadData;

public class ArtifactUploadSummaryTextAction extends SummaryTextAction {

  private ArtifactUploadData artifactUploadData;

  public ArtifactUploadSummaryTextAction(Run<?, ?> run, String summaryText) {
    super(run, summaryText);

    List<SummaryTextAction> projectActions = new ArrayList<>();

    projectActions.add(this);
    this.projectActions = projectActions;
  }

  public ArtifactUploadData getArtifactUploadData() {
    return artifactUploadData;
  }

  public void setArtifactUploadData(ArtifactUploadData artifactUploadData) {
    this.artifactUploadData = artifactUploadData;
  }
}
