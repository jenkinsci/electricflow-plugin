package org.jenkinsci.plugins.electricflow.data;

import hudson.model.Run;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowTestResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CloudBeesFlowTestResultData {
    private List<CloudBeesFlowTestResult> testResultData;

    public CloudBeesFlowTestResultData(Run run) {
        this.testResultData = new ArrayList<>();

        CloudBeesFlowTestResult testResult = CloudBeesFlowTestResult.build(run);
        this.testResultData.add(testResult);
    }

    public List<CloudBeesFlowTestResult> getTestResultData() {
        return testResultData;
    }

    public void setTestResultData(List<CloudBeesFlowTestResult> testResultData) {
        this.testResultData = testResultData;
    }
}
