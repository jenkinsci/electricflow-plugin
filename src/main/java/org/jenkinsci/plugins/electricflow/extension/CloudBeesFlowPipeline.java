package org.jenkinsci.plugins.electricflow.extension;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;

public class CloudBeesFlowPipeline implements ExtensionPoint {
    protected String stageName;
    protected String result;
    protected String reason;
    protected long duration;
    protected long timestamp;
    protected String logs;

    public CloudBeesFlowPipeline() {}
    public static List<CloudBeesFlowPipeline> build (Run run) {
        final Jenkins jenkins = Jenkins.get();
        List<CloudBeesFlowPipeline> result = new ArrayList<>();
        if (jenkins != null) {
            ExtensionList.lookup(CloudBeesFlowPipeline.class);
            final ExtensionList<CloudBeesFlowPipeline> makers = ExtensionList.lookup(CloudBeesFlowPipeline.class);
            for (CloudBeesFlowPipeline m : makers) {
                result = m.generate(run);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
//    public boolean populate(Run run) {
//        return false;
//    }
    public List<CloudBeesFlowPipeline> generate(Run run) {
        return new ArrayList<>();
    }
    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        this.logs = logs;
    }
}
