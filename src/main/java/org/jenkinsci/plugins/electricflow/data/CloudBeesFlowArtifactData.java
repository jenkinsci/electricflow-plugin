package org.jenkinsci.plugins.electricflow.data;

import hudson.model.Run;
import hudson.model.Run.ArtifactList;
import hudson.model.Run.Artifact;

import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowArtifact;

import java.util.ArrayList;
import java.util.List;

public class CloudBeesFlowArtifactData {
    private List<CloudBeesFlowArtifact> artifactData;

    public List<CloudBeesFlowArtifact> getArtifactData() {
        return artifactData;
    }

    public void setArtifactData(List<CloudBeesFlowArtifact> artifactData) {
        this.artifactData = artifactData;
    }

    public CloudBeesFlowArtifactData(Run run) {
        this.artifactData = new ArrayList<>();
        ArtifactList obj = (ArtifactList) run.getArtifacts();
        for (int i = 0; i < obj.size(); i++) {
            Artifact row = (Artifact) obj.get(i);
            CloudBeesFlowArtifact art = CloudBeesFlowArtifact.build(row);
            this.artifactData.add(art);
        }
        System.out.println("Done with artifacts");

    }

}
