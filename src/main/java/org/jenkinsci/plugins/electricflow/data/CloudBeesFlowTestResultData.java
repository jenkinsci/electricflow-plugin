package org.jenkinsci.plugins.electricflow.data;

import hudson.model.Run;
import java.util.ArrayList;
import java.util.List;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowTestResult;

public class CloudBeesFlowTestResultData {
  private List<CloudBeesFlowTestResult> testResultData;

  public CloudBeesFlowTestResultData(Run<?, ?> run) {
    this.testResultData = new ArrayList<>();

    CloudBeesFlowTestResult testResult = CloudBeesFlowTestResult.build(run);
    if (testResult != null) {
      this.testResultData.add(testResult);
    }
  }

  public List<CloudBeesFlowTestResult> getTestResultData() {
    return testResultData;
  }

  public void setTestResultData(List<CloudBeesFlowTestResult> testResultData) {
    this.testResultData = testResultData;
  }
}
