package org.jenkinsci.plugins.electricflow.extension;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Run;
import hudson.tasks.junit.TestResultAction;
import jenkins.model.Jenkins;

public class CloudBeesFlowTestResult implements ExtensionPoint {
    protected int failCount;
    protected int skipCount;
    protected int totalCount;
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
}
