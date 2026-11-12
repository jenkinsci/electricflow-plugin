package org.jenkinsci.plugins.electricflow.extension.impl;

import hudson.model.Run;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowPipeline;
import org.jenkinsci.plugins.variant.OptionalExtension;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.NotExecutedNodeAction;
import org.jenkinsci.plugins.workflow.actions.TagsAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * Publishes the name, start time and duration of each stage of a Pipeline run.
 */
@OptionalExtension(requirePlugins = {"workflow-api", "workflow-job"})
public class CloudBeesFlowPipelineRest extends CloudBeesFlowPipeline {

    /** Tag added by Declarative Pipeline to stages it skipped. */
    private static final String STAGE_STATUS_TAG = "STAGE_STATUS";

    @Override
    public List<CloudBeesFlowPipeline> generate(Run run) {
        if (!(run instanceof WorkflowRun)) {
            return null;
        }
        WorkflowRun workflowRun = (WorkflowRun) run;
        List<CloudBeesFlowPipeline> result = new ArrayList<>();
        FlowExecution execution = workflowRun.getExecution();
        if (execution == null) {
            return result;
        }

        List<BlockStartNode> stages = new ArrayList<>();
        Map<String, BlockEndNode<?>> endByStartId = new HashMap<>();
        Map<String, FlowNode> nextByNodeId = new HashMap<>();
        for (FlowNode node : new DepthFirstScanner().allNodes(execution)) {
            if (node instanceof BlockStartNode && isStage(node)) {
                stages.add((BlockStartNode) node);
            } else if (node instanceof BlockEndNode) {
                BlockEndNode<?> end = (BlockEndNode<?>) node;
                endByStartId.put(end.getStartNode().getId(), end);
            }
            for (FlowNode parent : node.getParents()) {
                FlowNode existing = nextByNodeId.get(parent.getId());
                if (existing == null || compareIds(node, existing) < 0) {
                    nextByNodeId.put(parent.getId(), node);
                }
            }
        }
        stages.sort(CloudBeesFlowPipelineRest::compareIds);

        for (BlockStartNode stage : stages) {
            CloudBeesFlowPipeline pipelineNode = new CloudBeesFlowPipeline();
            pipelineNode.setStageName(stage.getDisplayName());
            long start = TimingAction.getStartTime(stage);
            pipelineNode.setTimestamp(start);
            pipelineNode.setDuration(
                    isNotExecuted(stage)
                            ? 0
                            : duration(workflowRun, start, endByStartId.get(stage.getId()), nextByNodeId));
            result.add(pipelineNode);
        }
        return result;
    }

    private static boolean isStage(FlowNode node) {
        return node.getPersistentAction(LabelAction.class) != null
                && node.getPersistentAction(ThreadNameAction.class) == null;
    }

    private static boolean isNotExecuted(BlockStartNode stage) {
        if (stage.getPersistentAction(NotExecutedNodeAction.class) != null) {
            return true;
        }
        TagsAction tags = stage.getPersistentAction(TagsAction.class);
        String status = tags == null ? null : tags.getTagValue(STAGE_STATUS_TAG);
        return status != null && status.startsWith("SKIPPED_");
    }

    /**
     * Measures from the start of the stage to the node that follows its end node, falling back to
     * the end of the run, or to now while the run is still going.
     */
    private static long duration(WorkflowRun run, long start, BlockEndNode<?> end, Map<String, FlowNode> nextByNodeId) {
        long endTime;
        FlowNode after = end == null ? null : nextByNodeId.get(end.getId());
        if (after != null) {
            endTime = TimingAction.getStartTime(after);
        } else if (run.isBuilding()) {
            endTime = System.currentTimeMillis();
        } else {
            endTime = run.getStartTimeInMillis() + run.getDuration();
        }
        return Math.max(0, endTime - start);
    }

    private static int compareIds(FlowNode a, FlowNode b) {
        return Comparator.<String>comparingInt(String::length)
                .thenComparing(Comparator.naturalOrder())
                .compare(a.getId(), b.getId());
    }
}
