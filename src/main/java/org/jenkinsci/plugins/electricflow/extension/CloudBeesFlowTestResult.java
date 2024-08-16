package org.jenkinsci.plugins.electricflow.extension;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Run;
import net.sf.json.JSONObject;

public class CloudBeesFlowTestResult implements ExtensionPoint {

    protected int failCount;
    protected int failCountPrevious;
    protected int skipCount;
    protected int skipCountPrevious;
    protected int totalCount;
    protected int totalCountPrevious;
    protected double duration;
    protected double durationPrevious;
    protected String url;
    protected String displayName;
    protected boolean isPreviousRunExists = false;

    public CloudBeesFlowTestResult() {}

    public static CloudBeesFlowTestResult build(Run run) {
        ExtensionList.lookup(CloudBeesFlowTestResult.class);

        ExtensionList<CloudBeesFlowTestResult> makers = ExtensionList.lookup(CloudBeesFlowTestResult.class);
        for (CloudBeesFlowTestResult m : makers) {
            boolean popRes = m.populate(run);
            if (popRes) {
                return m;
            }
        }
        return null;
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();

        // Converting seconds for both duration and durationPrevious
        // to milliseconds.
        double durationSecs = 0.0;
        durationSecs = this.getDuration() * 1000;

        if (this.getUrl() != null) {
            json.put("url", this.getUrl());
        }

        json.put("displayName", this.getDisplayName());
        json.put("failCount", this.getFailCount());
        json.put("passCount", (this.getTotalCount() - this.getFailCount() - this.getSkipCount()));
        json.put("skipCount", this.getSkipCount());
        json.put("totalCount", this.getTotalCount());
        json.put("duration", durationSecs);

        // adding previous run
        if (this.isPreviousRunExists()) {
            double durationPreviousSecs = 0.0;
            durationPreviousSecs = this.getDurationPrevious() * 1000;
            json.put("failCountPrevious", this.getFailCountPrevious());
            json.put(
                    "passCountPrevious",
                    (this.getTotalCountPrevious() - this.getFailCountPrevious() - this.getSkipCountPrevious()));
            json.put("skipCountPrevious", this.getSkipCountPrevious());
            json.put("totalCountPrevious", this.getTotalCountPrevious());
            json.put("durationPrevious", durationPreviousSecs);
        }
        return json;
    }

    public boolean isApplicable(Object object) {
        return false;
    }

    public boolean populate(Run<?, ?> run) {
        return false;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public int getSkipCount() {
        return skipCount;
    }

    public void setSkipCount(int skipCount) {
        this.skipCount = skipCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public int getFailCountPrevious() {
        return failCountPrevious;
    }

    public void setFailCountPrevious(int failCountPrevious) {
        this.failCountPrevious = failCountPrevious;
    }

    public int getSkipCountPrevious() {
        return skipCountPrevious;
    }

    public void setSkipCountPrevious(int skipCountPrevious) {
        this.skipCountPrevious = skipCountPrevious;
    }

    public int getTotalCountPrevious() {
        return totalCountPrevious;
    }

    public void setTotalCountPrevious(int totalCountPrevious) {
        this.totalCountPrevious = totalCountPrevious;
    }

    public double getDurationPrevious() {
        return durationPrevious;
    }

    public void setDurationPrevious(double durationPrevious) {
        this.durationPrevious = durationPrevious;
    }

    public boolean isPreviousRunExists() {
        return isPreviousRunExists;
    }

    public void setPreviousRunExists(boolean previousRunExists) {
        isPreviousRunExists = previousRunExists;
    }
}
