package org.jenkinsci.plugins.electricflow.extension.impl;

import hudson.model.Run;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowTestResult;
import org.jenkinsci.plugins.variant.OptionalExtension;

@OptionalExtension(requirePlugins="junit")
public class CloudBeesFlowTestResultJUnit extends CloudBeesFlowTestResult {
    public boolean populate(Run run) {
        //TestResult obj2 = run.getAction(TestResult.class);
        TestResultAction obj = run.getAction(TestResultAction.class);
        // TestResult tr2 = obj.getTestResultPath();

        // VJN :: An ugly if-else block for handling NULL is to get around 
        // the warning thrown from Maven
        
        if (obj != null) {

            // CloudBeesFlowTestResult cloudBeesFlowTestResult = new CloudBeesFlowTestResult();
            this.setFailCount(obj.getFailCount());
            this.setSkipCount(obj.getSkipCount());
            this.setTotalCount(obj.getTotalCount());
            // this.setUrl(obj.getUrlName());
            TestResult result = obj.getResult();

            if (result != null) {
                this.setDuration(result.getDuration());
                String urlName = obj.getUrlName();
                Jenkins instance = Jenkins.get();
                String rootUrl = instance.getRootUrl();
                String testReportUrl = rootUrl + '/' + run.getUrl() + '/' + urlName;
                this.setUrl(testReportUrl);

             // VJN :: Based on Maven's  NP_NULL_ON_SOME_PATH spot check
             // had to bring the previousTestRun check within the IF
               hudson.tasks.test.TestResult previousTestRun = result.getPreviousResult();
               if (previousTestRun != null) {
                // previousTestRun.getFal
                this.setTotalCountPrevious(previousTestRun.getTotalCount());
                this.setSkipCountPrevious(previousTestRun.getSkipCount());
                this.setFailCountPrevious(previousTestRun.getFailCount());
                this.setDurationPrevious(previousTestRun.getDuration());

            }

            }
            // this.setDuration(obj.getResult().getDuration());
//                cloudBeesFlowTestResult.setFailCount(obj.getFailCount());
//                cloudBeesFlowTestResult.setSkipCount(obj.getSkipCount());
//                cloudBeesFlowTestResult.setTotalCount(obj.getTotalCount());
            return true;
            }
        
        return false;
    }
}
