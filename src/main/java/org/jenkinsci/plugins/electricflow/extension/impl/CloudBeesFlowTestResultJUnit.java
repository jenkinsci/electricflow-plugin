package org.jenkinsci.plugins.electricflow.extension.impl;

import hudson.model.Run;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowTestResult;
import org.jenkinsci.plugins.variant.OptionalExtension;

@OptionalExtension(requirePlugins = "junit")
public class CloudBeesFlowTestResultJUnit extends CloudBeesFlowTestResult {

  public boolean populate(Run run) {
    TestResultAction obj = run.getAction(TestResultAction.class);

    if (obj != null) {

      this.setFailCount(obj.getFailCount());
      this.setSkipCount(obj.getSkipCount());
      this.setTotalCount(obj.getTotalCount());
      this.setDisplayName(obj.getDisplayName());

      TestResult result = obj.getResult();

      if (result != null) {
        this.setDuration(result.getDuration());
        String urlName = obj.getUrlName();
        Jenkins instance = Jenkins.get();
        String rootUrl = instance.getRootUrl();
        String testReportUrl = rootUrl + '/' + run.getUrl() + '/' + urlName;
        this.setUrl(testReportUrl);

        // Based on Maven's  NP_NULL_ON_SOME_PATH spot check
        // this check is brought within the IF
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
