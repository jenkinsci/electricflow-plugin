package org.jenkinsci.plugins.electricflow.event;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jenkinsci.plugins.electricflow.Configuration;
import org.jenkinsci.plugins.electricflow.ElectricFlowClient;
import org.jenkinsci.plugins.electricflow.Utils;
import org.jenkinsci.plugins.electricflow.action.CloudBeesFlowRuntimeStateAction;
import org.jenkinsci.plugins.electricflow.causes.EFCause;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildAssociationType;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildTriggerSource;

@Extension
public class ElectricFlowBuildWatcher extends RunListener<Run> {
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
    // if efcause is present, we need to add state if it is not present.
    /* This section is commented out now because this behaviour should be investigated further.
    CloudBeesFlowRuntimeStateAction cloudBeesFlowRuntimeStateAction = run.getAction(CloudBeesFlowRuntimeStateAction.class);
    if (cloudBeesFlowRuntimeStateAction == null) {
      cloudBeesFlowRuntimeStateAction = new CloudBeesFlowRuntimeStateAction();
      run.addAction(cloudBeesFlowRuntimeStateAction);
    }
    */
    // 1. Getting configurations list:
    List<Configuration> cfgs = this.getConfigurations();
    // returning false because there is no applicable configurations to make it happen.
    if (cfgs.size() == 0) {
      return false;
    }

    // 2. Getting iterator out of configs.
    for (Configuration tc : cfgs) {
//      if (!cloudBeesFlowRuntimeStateAction.isWaitingForBuildData(tc.getConfigurationName())) {
//        continue;
//      }
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
        // cloudBeesFlowRuntimeStateAction.setNotWaitingForBuildData(tc.getConfigurationName());
        continue;
      } catch (RuntimeException ex) {
        taskListener
            .getLogger()
            .printf("[Configuration %s] Can't attach CiBuildData%n", tc.getConfigurationName());
        // cloudBeesFlowRuntimeStateAction.setNotWaitingForBuildData(tc.getConfigurationName());
        taskListener.getLogger().println(ex.getMessage());
        continue;
      }
    }
    return true;
  }
}
