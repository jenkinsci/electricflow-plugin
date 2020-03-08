package org.jenkinsci.plugins.electricflow.extension;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Run;
import hudson.tasks.junit.TestResultAction;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import java.lang.Math;

public class CloudBeesFlowTestResult implements ExtensionPoint {
    protected int failCount;
    protected int failCountPrevious;
    protected int skipCount;
    protected int skipCountPrevious;
    protected int totalCount;
    protected int totalCountPrevious;
    protected float duration;
    protected float durationPrevious;
    protected String url;

    public CloudBeesFlowTestResult() {};
    public static CloudBeesFlowTestResult build (Run run) {
        final Jenkins jenkins = Jenkins.get();
        if (jenkins != null) {
            ExtensionList.lookup(CloudBeesFlowTestResult.class);

            final ExtensionList<CloudBeesFlowTestResult> makers = ExtensionList.lookup(CloudBeesFlowTestResult.class);
            for (CloudBeesFlowTestResult m : makers) {
                System.out.println("Iterating through extensions");
                Class varClass = m.getClass();
                boolean popRes = m.populate(run);
                if (popRes) {
                    return m;
                }
            }
        }
        return null;
    }
    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();

        // VJN:: Converting secondsfor both duration and durationPrevious
        //to milliseconds.

        long durationSecs = (long) Math.ceil(this.getDuration() * 1000);
        long durationPreviousSecs = (long) Math.ceil(this.getDurationPrevious() * 1000);

        if (this.getUrl() != null) {
            json.put("url", this.getUrl());
        }
        json.put("failCount", this.getFailCount());
        json.put("passCount", (this.getTotalCount() - this.getFailCount() - this.getSkipCount()) );
        json.put("skipCount", this.getSkipCount());
        json.put("totalCount", this.getTotalCount());
        //json.put("duration", this.getDuration());
        json.put("duration", durationSecs);


        // adding previous run
        json.put("failCountPrevious", this.getFailCountPrevious());
        json.put("passCountPrevious", (this.getTotalCountPrevious() - this.getFailCountPrevious() - this.getSkipCountPrevious()) );
        json.put("skipCountPrevious", this.getSkipCountPrevious());
        json.put("totalCountPrevious", this.getTotalCountPrevious());
        //json.put("durationPrevious", this.getDurationPrevious());
        json.put("durationPrevious", durationPreviousSecs);

        return json;
    }
//    public static CloudBeesFlowTestResult build (Run run) {
//        final Jenkins jenkins = Jenkins.get();
//        if (jenkins != null) {
//            TestResultAction obj = run.getAction(TestResultAction.class);
//            if (obj != null) {
//                TestResultAction testData = obj;
//                CloudBeesFlowTestResult cloudBeesFlowTestResult = new CloudBeesFlowTestResult();
//                cloudBeesFlowTestResult.setFailCount(obj.getFailCount());
//                cloudBeesFlowTestResult.setSkipCount(obj.getSkipCount());
//                cloudBeesFlowTestResult.setTotalCount(obj.getTotalCount());
//                return cloudBeesFlowTestResult;
//            }
//        }
//        return null;
//
//    }

    // service methods
    // isApplicable() returns false because it will be implemented in subclasses
    public boolean isApplicable(Object object) {
        return false;
    }
    // populate
    public boolean populate(Run run) {
        return false;
    }

    // getters and setters
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

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration ;
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

    public float getDurationPrevious() {
        return durationPrevious;
    }

    public void setDurationPrevious(float durationPrevious) {
        this.durationPrevious = durationPrevious;

    }
}
