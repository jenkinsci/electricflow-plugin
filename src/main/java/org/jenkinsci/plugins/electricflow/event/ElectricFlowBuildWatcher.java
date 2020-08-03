package org.jenkinsci.plugins.electricflow.event;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.electricflow.Configuration;
import org.jenkinsci.plugins.electricflow.ElectricFlowClient;
import org.jenkinsci.plugins.electricflow.Utils;
import org.jenkinsci.plugins.electricflow.action.CloudBeesCDPBABuildDetails;
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
    this.sendBuildDetailsToInstanceImproved(run, listener);
  }

  @Override
  public void onCompleted(Run run, TaskListener listener) {
    this.sendBuildDetailsToInstanceImproved(run, listener);
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

  public boolean sendBuildDetailsToInstanceImproved(Run<?, ?> run, TaskListener taskListener) {
    EFCause efCause = null;
    CloudBeesCDPBABuildDetails cdPBABuildDetails = null;
    // BuildAssociationType buildAssociationType = null;
    // BuildTriggerSource buildTriggerSource = null;

    // getting EFCause object and CloudBeesCDPBABuildDetails object.
    try {
      efCause = (EFCause) run.getCause(EFCause.class);
    } catch (ClassCastException ignored) {
    }
    if (efCause == null) {
      cdPBABuildDetails = run.getAction(CloudBeesCDPBABuildDetails.class);
    }

    if (efCause == null && cdPBABuildDetails == null) {
      return false;
    }
    List<Configuration> cfgs = this.getConfigurations();
    // returning false because there is no applicable configurations to make it happen.
    if (cfgs.size() == 0) {
      return false;
    }

    for (Configuration tc : cfgs) {
      // 3. Getting configuration from iterator to create efclient out of it later.
      ElectricFlowClient electricFlowClient = new ElectricFlowClient(tc.getConfigurationName());
      // 4. Creating CloudBeesFlowBuildData object out of run:
      CloudBeesFlowBuildData cbf = new CloudBeesFlowBuildData(run);
      // EFCause has higher priority. It means that if we have EFCause object and
      // CloudBeesCDPBABuildDetails at the same time - we should use EFCause logic.
      CIBuildDetail details = null;
      if (efCause != null) {
        details = new CIBuildDetail(cbf, efCause.getProjectName());
        details.setFlowRuntimeId(efCause.getFlowRuntimeId());
        details.setAssociationType(BuildAssociationType.TRIGGERED_BY_FLOW);
        details.setBuildTriggerSource(BuildTriggerSource.FLOW);

        if (!efCause.getStageName().equals("null")) {
          details.setStageName(efCause.getStageName());
        }
        if (!efCause.getFlowRuntimeStateId().equals("null")) {
          details.setFlowRuntimeStateId(efCause.getFlowRuntimeStateId());
        }

      } else if (cdPBABuildDetails != null) {
        details = new CIBuildDetail(cbf, cdPBABuildDetails.getProjectName());
        details.setFlowRuntimeId(cdPBABuildDetails.getFlowRuntimeId());
        details.setAssociationType(BuildAssociationType.TRIGGERED_BY_CI);
        if (!cdPBABuildDetails.getStageName().equals("null")) {
          details.setStageName(cdPBABuildDetails.getStageName());
        }
        if (!cdPBABuildDetails.getFlowRuntimeStateId().equals("null")) {
          details.setFlowRuntimeStateId(cdPBABuildDetails.getFlowRuntimeStateId());
        }
      }

      if (details != null) {
        try {
          JSONObject attachResult = electricFlowClient.attachCIBuildDetails(details);
          System.out.println(details.toString());
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
    }

    return true;
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
    BuildAssociationType buildAssociationType = BuildAssociationType.TRIGGERED_BY_FLOW;
    BuildTriggerSource buildTriggerSource = BuildTriggerSource.FLOW;

    // No EFCause object. It means that it has not been started by efrun. We can't continue.
    if (efCause == null) {
      // there is no efCause, so we will be trying to get CloudBeesCDPBABuildDetails
      // The run has not been started by flow, but we need to catch it and send build details.
      CloudBeesCDPBABuildDetails bd = run.getAction(CloudBeesCDPBABuildDetails.class);
      if (bd == null) {
        return false;
      }
      // now we know that the CI is a trigger of a build, so we need to change the value of
      // build association type
      efCause = bd.newEFCause();
      buildAssociationType = BuildAssociationType.TRIGGERED_BY_CI;
      buildTriggerSource = BuildTriggerSource.CI;
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
                        .setAssociationType(buildAssociationType)
                        .setBuildTriggerSource(buildTriggerSource);
        // .setAssociationType(BuildAssociationType.TRIGGERED_BY_FLOW)
        // .setBuildTriggerSource(BuildTriggerSource.FLOW);

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
}
