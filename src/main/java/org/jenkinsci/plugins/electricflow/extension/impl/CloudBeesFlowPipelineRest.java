package org.jenkinsci.plugins.electricflow.extension.impl;

import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import hudson.model.Run;
import java.util.ArrayList;
import java.util.List;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowPipeline;
import org.jenkinsci.plugins.variant.OptionalExtension;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

@OptionalExtension(requirePlugins = "pipeline-rest-api")
public class CloudBeesFlowPipelineRest extends CloudBeesFlowPipeline {
  @Override
  public List<CloudBeesFlowPipeline> generate(Run run) {
    List<CloudBeesFlowPipeline> result = new ArrayList<>();
    try {
      RunExt re = RunExt.create((WorkflowRun) run);
      List<StageNodeExt> stages = re.getStages();
      for (int i = 0; i < stages.size(); i++) {
        StageNodeExt node = stages.get(i);
        CloudBeesFlowPipeline pipelineNode = new CloudBeesFlowPipeline();
        pipelineNode.setDuration(node.getDurationMillis());
        pipelineNode.setStageName(node.getName());
        pipelineNode.setTimestamp(node.getStartTimeMillis());
        result.add(pipelineNode);
      }
      return result;
    } catch (ClassCastException e) {
      return null;
    }
  }
}
