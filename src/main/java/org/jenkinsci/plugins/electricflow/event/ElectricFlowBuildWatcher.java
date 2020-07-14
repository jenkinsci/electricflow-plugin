package org.jenkinsci.plugins.electricflow.event;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Queue.Executable;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.antlr.v4.runtime.misc.NotNull;
import org.apache.tools.ant.taskdefs.Exec;
import org.jenkinsci.plugins.electricflow.Configuration;
import org.jenkinsci.plugins.electricflow.ElectricFlowClient;
import org.jenkinsci.plugins.electricflow.Utils;
import org.jenkinsci.plugins.electricflow.causes.EFCause;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildAssociationType;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildTriggerSource;
import org.jenkinsci.plugins.workflow.actions.BodyInvocationAction;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.StepNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;

@Extension
public class ElectricFlowBuildWatcher extends RunListener<Run> implements GraphListener {
  public ElectricFlowBuildWatcher() {
    super(Run.class);
  }

  @Override
  public void onStarted(Run run, TaskListener listener) {
    this.sendBuildDetailsToInstance(run, listener);
  }

  @Override
  public void onCompleted(Run run, TaskListener listener) {
    this.sendBuildDetailsToInstance(run, listener);
  }

  public List<Configuration> getConfigurations() {
    List<Configuration> configs = Utils.getConfigurations();
    List<Configuration> retval = new ArrayList<Configuration>();

    Iterator<Configuration> configIter = configs.iterator();
    while (configIter.hasNext()) {
      Configuration c = configIter.next();
      if (!c.getDoNotSendBuildDetails()) {
        retval.add(c);
      }
    }
    return retval;
  }

  @Override
  public void onNewHead(FlowNode flowNode) {
    // flowNode.
    System.out.println("Start of GraphListener block");
    // checking for stepnode

    WorkflowRun run = getRun(flowNode);
    Class<? extends FlowNode> cls = flowNode.getClass();
    //flowNode.getDescriptor();
    try {
      String log = run.getLog();
      System.out.println(log);
    } catch (IOException | NullPointerException ignore) { }
    String dname = flowNode.getDisplayName();
    String dname2 = flowNode.getDisplayFunctionName();
    String script = "";
    try {
      CpsFlowExecution execution = (CpsFlowExecution) flowNode.getExecution();
      script = execution.getScript();
      System.out.println(script);
      // String state = execution.
    } catch (ClassCastException ignore) { }
    StepNode sn = null;
    StepDescriptor sd = null;
    String fn = "";
    Action act = flowNode.getPersistentAction(LabelAction.class);
    Action act2 = flowNode.getPersistentAction(BodyInvocationAction.class);
    if (flowNode instanceof StepNode) {
      sn = (StepNode) flowNode;
      sd = sn.getDescriptor();
      fn = sd.getFunctionName();
    }
    ExecutionStatus es = showInfo(flowNode);
    if (es != null) {
      System.out.println("NOT NULL");
    }
    System.out.println("End of GraphListener block");
  }

  // this function takes flowNode as argument and then parses node data to report that
  // we're entering or leaving the stage.
  public ExecutionStatus showInfo(FlowNode flowNode) {
    StepNode sn = null;

    try {
      sn = (StepNode) flowNode;
    } catch (ClassCastException ignore) {}

    if (sn == null) {
      System.out.println("StepNode is Null, returning");
      return null;
    }
    StepDescriptor sd = sn.getDescriptor();
    String fn = sd.getFunctionName();
    String displayName = flowNode.getDisplayName();
    String displayFunctionName = flowNode.getDisplayFunctionName();
    Class nodeImplClass = flowNode.getClass();
    Action labelAction = flowNode.getAction(LabelAction.class);
    Action bodyInvocationAction = flowNode.getAction(BodyInvocationAction.class);
    ExecutionStatus executionStatus = new ExecutionStatus();

    String script = "";
    try {
      CpsFlowExecution execution = (CpsFlowExecution) flowNode.getExecution();
      script = execution.getScript();
      System.out.println(script);
      executionStatus.script = script;
      // String state = execution.
    } catch (ClassCastException ignore) { }

    if (nodeImplClass == StepStartNode.class) {
      System.out.println("Step Start Node");
      if (labelAction != null && bodyInvocationAction != null && fn.equals("stage")) {
        // TODO: Add script handling
        // executionStatus.script = flowNode.
        executionStatus.state = "Start";
        executionStatus.stageName = labelAction.getDisplayName();
        return executionStatus;
      }
      else {
        return null;
      }
    }
    else if (nodeImplClass == StepEndNode.class) {
      Action ila = ((StepEndNode) flowNode).getStartNode().getAction(LabelAction.class);
      if (ila != null && bodyInvocationAction != null && displayFunctionName.equals("}")) {
        executionStatus.state = "End";
        executionStatus.stageName = ila.getDisplayName();
        return executionStatus;
      }
    }

    return null;
  }
  public boolean sendBuildDetailsToInstance(Run<?, ?> run, TaskListener taskListener) {
    // 0. Getting EFCause
    EFCause efCause = null;
    try {
      efCause = (EFCause) run.getCause(EFCause.class);
    } catch (ClassCastException ignored) {
      // Ignoring - not triggered by Flow
      return false;
    }

    // No EFCause object. It means that it has not been started by efrun. We can't continue.
    if (efCause == null) {
      return false;
    }
    // 1. Getting configurations list:
    List<Configuration> cfgs = this.getConfigurations();
    // returning false because there is no applicable configurations to make it happen.
    if (cfgs.size() == 0) {
      return false;
    }

    // 2. Getting iterator out of configs.
    for (Configuration tc : cfgs) {
      // 3. Getting configuration from iterator to create efclient out of it later.
      ElectricFlowClient electricFlowClient = new ElectricFlowClient(tc.getConfigurationName());
      // 4. Creating CloudBeesFlowBuildData object out of run:
      CloudBeesFlowBuildData cbf = new CloudBeesFlowBuildData(run);

      try {
        // According to NTVEPLUGIN-277, triggeredByFlow should be passed back to flow in
        // case when build has been triggered by flow.
        CIBuildDetail details =
            new CIBuildDetail(cbf, efCause.getProjectName())
                .setFlowRuntimeId(efCause.getFlowRuntimeId())
                .setAssociationType(BuildAssociationType.TRIGGERED_BY_FLOW)
                .setBuildTriggerSource(BuildTriggerSource.FLOW);

        if (!efCause.getStageName().equals("null")) {
          details.setStageName(efCause.getStageName());
        }
        if (!efCause.getFlowRuntimeStateId().equals("null")) {
          details.setFlowRuntimeStateId(efCause.getFlowRuntimeStateId());
        }

        electricFlowClient.attachCIBuildDetails(details);
      } catch (IOException e) {
        continue;
      } catch (RuntimeException ex) {
        taskListener
            .getLogger()
            .printf("[Configuration %s] Can't attach CiBuildData%n", tc.getConfigurationName());
        taskListener.getLogger().println(ex.getMessage());
        continue;
      }
    }
    return true;
  }
  @CheckForNull
  private WorkflowRun getRun(@NotNull FlowNode flowNode) {
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

class ExecutionStatus {
  public String state;
  public String stageName;
  public String script;
  public ExecutionStatus() { }
}