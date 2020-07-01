package org.jenkinsci.plugins.electricflow.event;

import hudson.Extension;
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
import org.jenkinsci.plugins.electricflow.Configuration;
import org.jenkinsci.plugins.electricflow.ElectricFlowClient;
import org.jenkinsci.plugins.electricflow.Utils;
import org.jenkinsci.plugins.electricflow.causes.EFCause;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildAssociationType;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildTriggerSource;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

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
    WorkflowRun run = getRun(flowNode);
    try {
      String log = run.getLog();
      System.out.println(log);
    } catch (IOException | NullPointerException ignore) { }
    String dname = flowNode.getDisplayName();
    String dname2 = flowNode.getDisplayFunctionName();
    try {
      CpsFlowExecution execution = (CpsFlowExecution) flowNode.getExecution();
      String script = execution.getScript();
      System.out.println(script);
      // String state = execution.
    } catch (ClassCastException ignore) { }
    System.out.println("End of GraphListener block");
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
