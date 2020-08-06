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

    // 0. Getting EFCause object.
    try {
      efCause = (EFCause) run.getCause(EFCause.class);
    } catch (ClassCastException ignored) { }
    // 0a. Getting CloudBeesCDPBABuildDetails object only and only when EFCause is null
    if (efCause == null) {
      cdPBABuildDetails = run.getAction(CloudBeesCDPBABuildDetails.class);
    }

    if (efCause == null && cdPBABuildDetails == null) {
      return false;
    }

    // 1. Getting configurations
    List<Configuration> cfgs = this.getConfigurations();
    // returning false because there is no applicable configurations to make it happen.
    if (cfgs.size() == 0) {
      return false;
    }

    /*
    !!!IMPORTANT!!!
      Do not return any value from iterating configurations loop.
      It turns out that there is a scenario when we may have more than 1 configuration.
      In that case we need to iterate through them and if this method will return something
      inside of the loop under some condition - iteration will stop and some configs will not be
      processed.
    !!!IMPORTANT!!!
    */
    // 2. Getting iterator out of configs list.
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
//          System.out.println(details.toString());
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


}
