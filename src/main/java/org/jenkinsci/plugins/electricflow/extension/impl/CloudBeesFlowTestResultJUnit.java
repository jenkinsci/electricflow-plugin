package org.jenkinsci.plugins.electricflow.extension.impl;

import hudson.model.Run;
import hudson.tasks.junit.TestResultAction;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowTestResult;
import org.jenkinsci.plugins.variant.OptionalExtension;

@OptionalExtension(requirePlugins="junit")
public class CloudBeesFlowTestResultJUnit extends CloudBeesFlowTestResult {
    public boolean populate(Run run) {
        TestResultAction obj = run.getAction(TestResultAction.class);
            if (obj != null) {
                TestResultAction testData = obj;
                // CloudBeesFlowTestResult cloudBeesFlowTestResult = new CloudBeesFlowTestResult();
                this.setFailCount(obj.getFailCount());
                this.setSkipCount(obj.getSkipCount());
                this.setTotalCount(obj.getTotalCount());
//                cloudBeesFlowTestResult.setFailCount(obj.getFailCount());
//                cloudBeesFlowTestResult.setSkipCount(obj.getSkipCount());
//                cloudBeesFlowTestResult.setTotalCount(obj.getTotalCount());
                return true;
            }
        return false;
    }
}
