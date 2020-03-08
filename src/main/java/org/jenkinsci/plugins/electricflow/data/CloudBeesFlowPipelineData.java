package org.jenkinsci.plugins.electricflow.data;

import hudson.model.Run;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowPipeline;

import java.util.List;

public class CloudBeesFlowPipelineData {
    private List<CloudBeesFlowPipeline> pipelineData;

    public CloudBeesFlowPipelineData(Run run) {
        this.pipelineData = CloudBeesFlowPipeline.build(run);
    }
    public List<CloudBeesFlowPipeline> getPipelineData() {
        return pipelineData;
    }

    public void setPipelineData(List<CloudBeesFlowPipeline> pipelineData) {
        this.pipelineData = pipelineData;
    }
}
