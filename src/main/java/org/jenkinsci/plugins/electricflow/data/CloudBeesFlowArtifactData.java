
// CloudBeesFlowArtifactData.java --
//
// CloudBeesFlowArtifactData.java is part of CloudBees Flow.
//
// Copyright (c) 2020 CloudBees, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow.data;

import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.electricflow.ArtifactUploadSummaryTextAction;
import org.jenkinsci.plugins.electricflow.extension.ArtifactUploadData;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowArtifact;

import hudson.model.Run;

import hudson.model.Run.Artifact;
import hudson.model.Run.ArtifactList;

public class CloudBeesFlowArtifactData
{

    //~ Instance fields --------------------------------------------------------

    private List<CloudBeesFlowArtifact> artifactData;

    //~ Constructors -----------------------------------------------------------

    public CloudBeesFlowArtifactData(Run<?, ?> run)
    {
        this.artifactData = new ArrayList<>();

        List<ArtifactUploadSummaryTextAction> artifactUploadSummaryTextActions =
            run.getActions(ArtifactUploadSummaryTextAction.class);
        ArtifactList                          artifactList                     =
            (ArtifactList) run.getArtifacts();

        for (Object artifactRow : artifactList) {
            Artifact artifact = (Artifact) artifactRow;

            for (ArtifactUploadSummaryTextAction artifactUploadSummaryTextAction
                    : artifactUploadSummaryTextActions) {
                ArtifactUploadData    artifactUploadData    =
                    artifactUploadSummaryTextAction.getArtifactUploadData();
                CloudBeesFlowArtifact cloudBeesFlowArtifact = null;

                // we've found a match!
                if (artifactUploadData.getFilePath()
                                      .equalsIgnoreCase(artifact.getHref())) {
                    cloudBeesFlowArtifact = CloudBeesFlowArtifact.build(
                            artifact,
                            artifactUploadSummaryTextAction
                                .getArtifactUploadData());
                }
                else {
                    cloudBeesFlowArtifact = CloudBeesFlowArtifact.build(
                            artifact,
                            null /* No matching artifact published to flow */);
                }

                this.artifactData.add(cloudBeesFlowArtifact);
            }
        }
    }

    //~ Methods ----------------------------------------------------------------

    public List<CloudBeesFlowArtifact> getArtifactData()
    {
        return artifactData;
    }

    public void setArtifactData(List<CloudBeesFlowArtifact> artifactData)
    {
        this.artifactData = artifactData;
    }
}
