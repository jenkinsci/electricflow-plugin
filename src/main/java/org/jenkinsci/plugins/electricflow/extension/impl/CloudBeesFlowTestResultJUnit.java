package org.jenkinsci.plugins.electricflow.extension.impl;

import hudson.model.Run;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowTestResult;
import org.jenkinsci.plugins.variant.OptionalExtension;

@OptionalExtension(requirePlugins = "junit")
public class CloudBeesFlowTestResultJUnit extends CloudBeesFlowTestResult {
  public boolean populate(Run<?, ?> run) {
    TestResultAction testResultAction = run.getAction(TestResultAction.class);

    // VJN :: An ugly if-else block for handling NULL is to get around
    // the warning thrown from Maven

    if (testResultAction != null) {
      this.setFailCount(testResultAction.getFailCount());
      this.setSkipCount(testResultAction.getSkipCount());
      this.setTotalCount(testResultAction.getTotalCount());
      this.setDisplayName(testResultAction.getDisplayName());

      TestResult result = testResultAction.getResult();

      if (result != null) {
        this.setDuration(result.getDuration());
        String urlName = testResultAction.getUrlName();
        Jenkins instance = Jenkins.get();
        String rootUrl = instance.getRootUrl();
        String testReportUrl = rootUrl + '/' + run.getUrl() + '/' + urlName;
        this.setUrl(testReportUrl);

        // VJN :: Based on Maven's  NP_NULL_ON_SOME_PATH spot check
        // had to bring the previousTestRun check within the IF
        hudson.tasks.test.TestResult previousTestRun = result.getPreviousResult();
        if (previousTestRun != null) {
          this.setTotalCountPrevious(previousTestRun.getTotalCount());
          this.setSkipCountPrevious(previousTestRun.getSkipCount());
          this.setFailCountPrevious(previousTestRun.getFailCount());
          this.setDurationPrevious(previousTestRun.getDuration());
        }
      }
      return true;
    }

    return false;
  }
}
