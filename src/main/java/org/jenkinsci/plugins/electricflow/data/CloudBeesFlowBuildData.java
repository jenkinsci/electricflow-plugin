package org.jenkinsci.plugins.electricflow.data;

import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import jenkins.scm.RunWithSCM;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowArtifact;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowSCM;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowTestResult;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
// import com.cloudbees.workflow.*
// im[prt]

public class CloudBeesFlowBuildData {
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
    protected CloudBeesFlowPipelineData stages;
    protected CloudBeesFlowSCMData changeSets;
    protected CloudBeesFlowArtifactData artifacts;
    protected CloudBeesFlowTestResultData testResult;

    public PrintStream logger;


    // constructor
    public CloudBeesFlowBuildData (Run<?,?> run, PrintStream logger) {
        this.logger = logger;
        // populating scalar values.
        this.setBuildNumber(run.getNumber());
        this.setDisplayName(run.getDisplayName());
        this.setBuilding(run.isBuilding());
        // todo: improve result handling
        Result result = run.getResult();
        if (result != null) {
            this.setResult(result.toString());
        }
        // todo: Improve reason handling
        // this.setReason(run.get);
        this.setDuration(run.getDuration());
        this.setEstimatedDuration(run.getEstimatedDuration());
        this.setTimestamp(run.getTimestamp().getTimeInMillis());
        this.setUrl(run.getUrl());
        // this.setLogs(run.getLog(200));
        // this.setLaunchedBy();
        // populating object values:

        // getting changesets information:
        RunWithSCM<?,?> abstractBuild = (RunWithSCM<?,?>) run;
        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = abstractBuild.getChangeSets();

        this.changeSets = new CloudBeesFlowSCMData(changeSets);
        this.testResult = new CloudBeesFlowTestResultData(run);
        this.artifacts = new CloudBeesFlowArtifactData(run);
        this.stages = new CloudBeesFlowPipelineData(run);
    }
    public void dump() {
        logger.println("===");
        logger.println("Beginning of CloudBeesFlowBuildData");
        logger.println("Build: " + this.getDisplayName());
        logger.println("Build Number: " + this.getBuildNumber());
        // logger.println("Is Building?: " + this.isBuilding());
        logger.println("Result: " + this.getResult());
        logger.println("Duration: " + this.getDuration());
        logger.println("Estimated duration: " + this.getEstimatedDuration());
        logger.println("Timestamp: " + this.getTimestamp());
        logger.println("URL: " + this.getUrl());

        logger.println("---");
        logger.println("Test results:");
        List<CloudBeesFlowTestResult> tr = this.testResult.getTestResultData();
        for (int i = 0; i < tr.size(); i++) {
            logger.println(i + ":");
            CloudBeesFlowTestResult row = tr.get(i);
            logger.println("Fail count: " + row.getFailCount());
            logger.println("Skip count: " + row.getSkipCount());
            logger.println("Total count: " + row.getTotalCount());
        }

        logger.println("---");
        logger.println("Artifacts: ");
        List<CloudBeesFlowArtifact> ar = this.artifacts.getArtifactData();
        for (int i = 0; i < ar.size(); i++) {
            logger.println(i + ":");
            CloudBeesFlowArtifact row = ar.get(i);
            logger.println("File Name: " + row.getName());
            logger.println("Size: " + row.getSize());
            logger.println("Href: " + row.getHref());
        }

        logger.println("---");
        logger.println("SCM data:");
        List<CloudBeesFlowSCM> cs = this.changeSets.getScmData();
        for (int i = 0; i < cs.size(); i++) {
            logger.println(i + ":");
            CloudBeesFlowSCM row = cs.get(i);
            logger.println("SCM: " + row.getScmType());
            logger.println("Timestamp: " + row.getTimestamp());
            logger.println("Author name: " + row.getAuthor());
            logger.println("Author email: " + row.getAuthorEmail());
            logger.println("Commit ID: " + row.getCommitId());
            logger.println("" + row.getCommitMessage());
        }
        logger.println("===");
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
}
