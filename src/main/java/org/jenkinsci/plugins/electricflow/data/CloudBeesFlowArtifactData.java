package org.jenkinsci.plugins.electricflow.data;

import hudson.model.Run;
import hudson.model.Run.ArtifactList;
import hudson.model.Run.Artifact;

import org.jenkinsci.plugins.electricflow.ArtifactSummaryTextAction;
import org.jenkinsci.plugins.electricflow.FlowArtifactObject;
import org.jenkinsci.plugins.electricflow.SummaryTextAction;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowArtifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<ArtifactSummaryTextAction> summaryActions = run.getActions(ArtifactSummaryTextAction.class);

        int count = 0;
        boolean found = false;
        int location = 0;
        for (int i = 0; i < obj.size(); i++) {
            CloudBeesFlowArtifact art = new CloudBeesFlowArtifact();
            Artifact row = (Artifact) obj.get(i);
            found = false;
            for(ArtifactSummaryTextAction action : summaryActions){
                if (action.getArtifactDetails().getFilePath().equals(row.getHref())){
                    art = CloudBeesFlowArtifact.build(row, action.getArtifactDetails());
                    found = true;
                    break;
                }
            }
            if(!found) {
                art = CloudBeesFlowArtifact.build(row, null);
            }

            this.artifactData.add(art);
        }
        System.out.println("Done with artifacts");
    }

}
