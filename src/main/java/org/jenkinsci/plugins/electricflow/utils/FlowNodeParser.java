package org.jenkinsci.plugins.electricflow.utils;

import hudson.model.Queue.Executable;
import hudson.model.Run;
import java.io.IOException;
import org.antlr.v4.runtime.misc.NotNull;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/*
 * This class implements a custom logic for parsing
 * FlowNode object, that we're getting using onNewHead method in
 * the event handling mechanism. It returns a State object.
 *
 */
public class FlowNodeParser {
  protected FlowNode flowNode;

  public FlowNodeParser(FlowNode flowNode) {
    this.flowNode = flowNode;
  }
  public FlowNode getFlowNode() {
    return flowNode;
  }

  public void setFlowNode(FlowNode flowNode) {
    this.flowNode = flowNode;
  }
  private WorkflowRun getRun() {
    FlowNode flowNode = this.getFlowNode();
    Executable exec;
    try {
      exec = flowNode.getExecution().getOwner().getExecutable();
    } catch (IOException e) {
      // Ignore the error, that step cannot be monitored.
      return null;
    }

    if (exec instanceof WorkflowRun) {
      return (WorkflowRun) exec;
    }
    return null;
  }

}
class CloudBeesFlowPipelineExecutionState {
  String state;
  public CloudBeesFlowPipelineExecutionState(String state) {
    this.state = state;
  }
}