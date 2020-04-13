package org.jenkinsci.plugins.electricflow.event;

import hudson.Extension;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import org.acegisecurity.acls.domain.AuditLogger;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;

@Extension
public class ElectricFlowBuildWatcher extends RunListener<Run> {
    public ElectricFlowBuildWatcher() {
        super(Run.class);
    }

    @Override
    public void onStarted(Run run, TaskListener listener) {
        System.out.println("Got onStarted event...");
        CloudBeesFlowBuildData cbf = new CloudBeesFlowBuildData(run);
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
        CloudBeesFlowBuildData cbf = new CloudBeesFlowBuildData(run);
        System.out.println("Got onCompleted build data object");
    }
}
