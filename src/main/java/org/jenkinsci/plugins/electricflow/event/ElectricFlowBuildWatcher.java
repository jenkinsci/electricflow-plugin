package org.jenkinsci.plugins.electricflow.event;

import hudson.Extension;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import org.acegisecurity.acls.domain.AuditLogger;
import org.jenkinsci.plugins.electricflow.Configuration;
import org.jenkinsci.plugins.electricflow.ElectricFlowClient;
import org.jenkinsci.plugins.electricflow.EnvReplacer;
import org.jenkinsci.plugins.electricflow.Utils;
import org.jenkinsci.plugins.electricflow.causes.EFCause;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import sun.security.krb5.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Extension
public class ElectricFlowBuildWatcher extends RunListener<Run> {
    public ElectricFlowBuildWatcher() {
        super(Run.class);
    }

    @Override
    public void onStarted(Run run, TaskListener listener) {
        System.out.println("Got onStarted event...");
        System.out.println("Sending details");
        this.sendBuildDetailsToInstance(run);
        System.out.println("Got onStarted build data object");
//        StringBuilder buf = new StringBuilder(100);
//        for (CauseAction action : run.getActions(CauseAction.class)) {
//            for (Cause cause : action.getCauses()) {
//                if (buf.length() > 0) buf.append(", ");
//                buf.append(cause.getShortDescription());
//            }
//        }
    }
    @Override
    public void onCompleted(Run run, TaskListener listener) {
        System.out.println("Got onCompleted event...");
        System.out.println("Sending details");
        // CloudBeesFlowBuildData cbf = new CloudBeesFlowBuildData(run);
        this.sendBuildDetailsToInstance(run);
        System.out.println("Got onCompleted build data object");
    }
    public List<Configuration>  getConfigurations() {
        List<Configuration> configs = Utils.getConfigurations();
        List<Configuration> retval = new ArrayList<Configuration>();

        Iterator<Configuration> configIter = configs.iterator();
        while(configIter.hasNext()) {
            Configuration c = configIter.next();
            if (!c.getDoNotSendBuildDetails()) {
                retval.add(c);
            }
        }
        return retval;
    }
    public boolean sendBuildDetailsToInstance(Run run) {
        // 0. Getting EFCause
        EFCause efCause = null;
        try {
            efCause = (EFCause) run.getCause(EFCause.class);
        } catch(ClassCastException ignored) {};

        // No EFCause object. It means that it has been started not by efrun. We can't continue.
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
        Iterator<Configuration> configurationIterator = cfgs.iterator();
        while(configurationIterator.hasNext()) {
            // 3. Getting configuration from iterator to create efclient out of it later.
            Configuration tc = configurationIterator.next();
            ElectricFlowClient electricFlowClient = new ElectricFlowClient(tc.getConfigurationName());
            // 4. Creating CloudBeesFlowBuildData object out of run:
            CloudBeesFlowBuildData cbf = new CloudBeesFlowBuildData(run);
            try {
                electricFlowClient.setJenkinsBuildDetailsRunPipeline(
                        cbf,
                        efCause.getProjectName(),
                        efCause.getFlowRuntimeId(),
                        efCause.getStageName(),
                        efCause.getFlowRuntimeStateId());
            } catch (IOException e) {
                return false;
            }
        }
        return true;

    }
}
