package org.jenkinsci.plugins.electricflow.data;

import hudson.model.Cause;
import hudson.model.Cause.UserIdCause;
import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import java.io.IOException;
import java.util.List;
import jenkins.model.Jenkins;
import jenkins.scm.RunWithSCM;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowArtifact;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowPipeline;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowSCM;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowTestResult;

public class CloudBeesFlowBuildData {

  private static final Log log = LogFactory.getLog(CloudBeesFlowBuildData.class);

  protected String jobName;
  protected String displayName;
  protected String launchedBy;
  protected int buildNumber;
  protected boolean building;
  protected String result;
  protected String reason;
  protected long duration;
  protected long estimatedDuration;
  protected long timestamp;
  protected String logs;
  protected String url;
  protected String blueOceanUrl;
  protected CloudBeesFlowPipelineData stages;
  protected CloudBeesFlowSCMData changeSets;
  protected CloudBeesFlowArtifactData artifacts;
  protected CloudBeesFlowTestResultData testResult;

  // constructor
  public CloudBeesFlowBuildData(Run<?, ?> run) {

    this.jobName = run.getCharacteristicEnvVars().get("JOB_NAME");

    Jenkins instance = Jenkins.get();
    String rootUrl = instance.getRootUrl();

    this.setBuildNumber(run.getNumber());
    this.setDisplayName(this.getJobName() + run.getDisplayName());
    this.setBuilding(run.isBuilding());

    try {
      List<String> runLogs = run.getLog(200);
      String logLines = String.join("\n", runLogs);
      this.setLogs(logLines);
    } catch (IOException e) {
      log.error(e.getMessage());
    }

    // todo: improve result handling
    Result result = run.getResult();
    if (result != null) {
      this.setResult(result.toString());
    }

    //resolve the launchedBy
    List<Cause> causes = run.getCauses();
    if (!causes.isEmpty()) {
      Cause cause = causes.stream().findFirst().get();
      this.setLaunchedBy(cause.getShortDescription());
    }

    // todo: Improve reason handling
    long duration = run.getDuration();
    if (duration == 0) {
      duration = Math.max(System.currentTimeMillis() - run.getStartTimeInMillis(), 0);
    }
    this.setDuration(duration);
    this.setEstimatedDuration(run.getEstimatedDuration());
    this.setTimestamp(run.getTimestamp().getTimeInMillis());
    this.setUrl(rootUrl + run.getUrl());

    // getting changesets information:
    RunWithSCM<?, ?> abstractBuild = (RunWithSCM<?, ?>) run;
    List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = abstractBuild.getChangeSets();

    this.changeSets = new CloudBeesFlowSCMData(changeSets);
    this.testResult = new CloudBeesFlowTestResultData(run);
    this.artifacts = new CloudBeesFlowArtifactData(run);
    this.stages = new CloudBeesFlowPipelineData(run);
  }

  public JSONObject toJsonObject() {
    JSONObject json = new JSONObject();

    // adding non-complex values of this
    if (this.getDisplayName() != null) {
      json.put("displayName", this.getDisplayName());
    }

    if (this.getLaunchedBy() != null) {
      json.put("launchedBy", this.getLaunchedBy());
    }

    json.put("buildNumber", Integer.toString(this.getBuildNumber()));

    json.put("building", this.isBuilding());

    if (this.getResult() != null) {
      json.put("result", this.getResult());
    }
    if (this.getReason() != null) {
      json.put("reason", this.getReason());
    }
    json.put("duration", this.getDuration());
    json.put("estimatedDuration", this.getEstimatedDuration());
    json.put("timestamp", this.getTimestamp());

    if (this.getLogs() != null) {
      json.put("logs", this.getLogs());
    }

    if (this.getUrl() != null) {
      json.put("url", this.getUrl());
      blueOceanUrl =
          this.getUrl()
              .replace(
                  "job/" + this.jobName,
                  "blue/organizations/jenkins/" + this.jobName + "/detail/" + this.jobName);
      json.put("consoleLogUrl", this.getUrl() + "console");
      json.put("blueOceanUrl", this.blueOceanUrl);
    }

    // now adding object values to json

    // processing pipeline data
    CloudBeesFlowPipelineData pipelineData = this.getStages();
    if (pipelineData != null
        && pipelineData.getPipelineData() != null
        && pipelineData.getPipelineData().size() > 0) {
      JSONArray pipelineJsonArray = new JSONArray();
      List<CloudBeesFlowPipeline> pipelineRows = pipelineData.getPipelineData();
      for (int i = 0; i < pipelineRows.size(); i++) {
        pipelineJsonArray.add(pipelineRows.get(i).toJsonObject());
      }

      json.put("stage", pipelineJsonArray);
    }

    // processing artifacts data
    CloudBeesFlowArtifactData artifactsData = this.getArtifacts();

    // TODO: Improve not-null validation here
    if (artifactsData != null
        && artifactsData.getArtifactData() != null
        && artifactsData.getArtifactData().size() > 0) {
      JSONArray artifactsJsonArray = new JSONArray();
      List<CloudBeesFlowArtifact> artifactRows = artifactsData.getArtifactData();
      for (int i = 0; i < artifactRows.size(); i++) {
        artifactsJsonArray.add(artifactRows.get(i).toJsonObject());
      }
      json.put("artifacts", artifactsJsonArray);
    }

    // processing test results data
    CloudBeesFlowTestResultData testResultData = this.getTestResult();
    if (testResultData != null
        && testResultData.getTestResultData() != null
        && testResultData.getTestResultData().size() > 0) {
      JSONArray testResultsJsonArray = new JSONArray();
      List<CloudBeesFlowTestResult> testResultRows = testResultData.getTestResultData();
      for (int i = 0; i < testResultRows.size(); i++) {
        testResultsJsonArray.add(testResultRows.get(i).toJsonObject());
      }
      json.put("testResult", testResultsJsonArray);
    }
    // processing SCM data
    CloudBeesFlowSCMData scmData = this.getChangeSets();
    if (scmData != null && scmData.getScmData().size() > 0) {
      JSONArray scmJsonArray = new JSONArray();
      List<CloudBeesFlowSCM> scmRows = scmData.getScmData();
      for (int i = 0; i < scmRows.size(); i++) {
        scmJsonArray.add(scmRows.get(i).toJsonObject());
      }
      json.put("changeSets", scmJsonArray);
    }

    return json;
  }

  // end of constructor
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getLaunchedBy() {
    return launchedBy;
  }

  public void setLaunchedBy(String launchedBy) {
    this.launchedBy = launchedBy;
  }

  public int getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(int buildNumber) {
    this.buildNumber = buildNumber;
  }

  public boolean isBuilding() {
    return building;
  }

  public void setBuilding(boolean building) {
    this.building = building;
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

  public long getEstimatedDuration() {
    return estimatedDuration;
  }

  public void setEstimatedDuration(long estimatedDuration) {
    this.estimatedDuration = estimatedDuration;
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

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public CloudBeesFlowPipelineData getStages() {
    return stages;
  }

  public void setStages(CloudBeesFlowPipelineData stages) {
    this.stages = stages;
  }

  public CloudBeesFlowSCMData getChangeSets() {
    return changeSets;
  }

  public void setChangeSets(CloudBeesFlowSCMData changeSets) {
    this.changeSets = changeSets;
  }

  public CloudBeesFlowArtifactData getArtifacts() {
    return artifacts;
  }

  public void setArtifacts(CloudBeesFlowArtifactData artifacts) {
    this.artifacts = artifacts;
  }

  public CloudBeesFlowTestResultData getTestResult() {
    return testResult;
  }

  public void setTestResult(CloudBeesFlowTestResultData testResult) {
    this.testResult = testResult;
  }
}
