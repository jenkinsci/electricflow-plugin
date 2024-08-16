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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.plugins.electricflow.Configuration;
import org.jenkinsci.plugins.electricflow.ElectricFlowClient;
import org.jenkinsci.plugins.electricflow.Utils;
import org.jenkinsci.plugins.electricflow.action.CloudBeesCDPBABuildDetails;
import org.jenkinsci.plugins.electricflow.causes.EFCause;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildAssociationType;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildTriggerSource;

@Extension
public class ElectricFlowBuildWatcher extends RunListener<Run> {
    private static final Log log = LogFactory.getLog(ElectricFlowBuildWatcher.class);

    public ElectricFlowBuildWatcher() {
        super(Run.class);
    }

    @Override
    public void onStarted(Run run, TaskListener listener) {
        // NOTE: Commented to be used for debug reasons during development later.
        // CloudBeesFlowBuildData cloudBeesFlowBuildData = new CloudBeesFlowBuildData(run);
        this.addActionForBuildsTriggeredByCD(run);
        this.sendBuildDetailsToInstanceImproved(run, listener);
    }

    @Override
    public void onCompleted(Run run, TaskListener listener) {
        // NOTE: Commented to be used for debug reasons during development later.
        // CloudBeesFlowBuildData cloudBeesFlowBuildData = new CloudBeesFlowBuildData(run);
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
        } catch (ClassCastException ignored) {
        }
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
            ElectricFlowClient electricFlowClient =
                    ElectricFlowClientFactory.getElectricFlowClient(tc.getConfigurationName(), null, run, null);
            // 4. Creating CloudBeesFlowBuildData object out of run:
            CloudBeesFlowBuildData cbf = new CloudBeesFlowBuildData(run);
            // EFCause has higher priority. It means that if we have EFCause object and
            // CloudBeesCDPBABuildDetails at the same time - we should use EFCause logic.
            CIBuildDetail details = null;
            if (efCause != null) {
                details = new CIBuildDetail(cbf, efCause.getProjectName());
                details.setFlowRuntimeId(efCause.getFlowRuntimeId());
                details.setAssociationType(BuildAssociationType.TRIGGERED_BY_CD);
                details.setBuildTriggerSource(BuildTriggerSource.CD);

                if (!efCause.getStageName().equals("null")) {
                    details.setStageName(efCause.getStageName());
                }
                if (!efCause.getFlowRuntimeStateId().equals("null")) {
                    details.setFlowRuntimeStateId(efCause.getFlowRuntimeStateId());
                }

            } else if (cdPBABuildDetails != null) {
                // If build details were sent using some configuration name there is no point
                // in trying to send data to any other configuration.
                if (cdPBABuildDetails.getOverriddenCredential() != null) {
                    electricFlowClient = ElectricFlowClientFactory.getElectricFlowClient(
                            tc.getConfigurationName(), cdPBABuildDetails.getOverriddenCredential(), run, null);
                }
                if (cdPBABuildDetails.getConfigurationName() != null
                        && !cdPBABuildDetails.getConfigurationName().equals(tc.getConfigurationName())) {
                    continue;
                }
                details = new CIBuildDetail(cbf, cdPBABuildDetails.getProjectName());
                details.setFlowRuntimeId(cdPBABuildDetails.getFlowRuntimeId());

                // Handling forwarding of build association type
                if (cdPBABuildDetails.getBuildAssociationType() != null) {
                    details.setAssociationType(cdPBABuildDetails.getBuildAssociationType());
                } else {
                    details.setAssociationType(BuildAssociationType.TRIGGERED_BY_CI);
                }

                // Handling forwarding of trigger source
                if (cdPBABuildDetails.getTriggerSource() != null) {
                    details.setBuildTriggerSource(cdPBABuildDetails.getTriggerSource());
                }

                if (!cdPBABuildDetails.getStageName().equals("null")) {
                    details.setStageName(cdPBABuildDetails.getStageName());
                }
                if (!cdPBABuildDetails.getFlowRuntimeStateId().equals("null")) {
                    details.setFlowRuntimeStateId(cdPBABuildDetails.getFlowRuntimeStateId());
                }
            }

            if (details != null) {
                try {
                    if (log.isDebugEnabled()) {
                        taskListener.getLogger().printf("Sending Build Details to CD:%n%s%n", details.toString());
                    }
                    JSONObject attachResult = electricFlowClient.attachCIBuildDetails(details);
                    if (log.isDebugEnabled()) {
                        taskListener
                                .getLogger()
                                .printf("Send Build Details execution result:%n%s%n", attachResult.toString());
                    }

                    //          System.out.println(details.toString());
                } catch (IOException e) {
                    continue;
                } catch (RuntimeException ex) {
                    taskListener.getLogger().printf("Sent Build Details to CD:%n%s%n", details.toString());
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

    /**
     * CI builds triggered by CD primarily store related information in {@link EFCause}, whereas CD pipelines, releases,
     * and attachments triggered by CI store related information in {@link CloudBeesCDPBABuildDetails}.
     *
     * <p>This method adds a {@link CloudBeesCDPBABuildDetails} action corresponding the the {@link EFCause} if one is
     * present so that all CD-related associations can be viewed in the Jenkins REST API just by browsing the
     * {@link CloudBeesCDPBABuildDetails} actions attached to a build.
     *
     * <p>{@link EFCause} takes priority in {@link #sendBuildDetailsToInstanceImproved} so this should not affect existing
     * behavior.
     */
    private void addActionForBuildsTriggeredByCD(Run<?, ?> run) {
        EFCause cause = run.getCause(EFCause.class);
        if (cause != null) {
            CloudBeesCDPBABuildDetails.applyToRuntime(
                    run,
                    null,
                    null,
                    cause.getFlowRuntimeId(),
                    cause.getFlowRuntimeStateId(),
                    cause.getProjectName(),
                    cause.getReleaseName(),
                    cause.getStageName(),
                    BuildTriggerSource.CD,
                    BuildAssociationType.TRIGGERED_BY_CD);
        }
    }
}
