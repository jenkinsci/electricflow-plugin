package com.electriccloud.plugin.spec.nativeplugin.utils

import com.electriccloud.plugin.spec.core.Job

class JenkinsBuildJob extends Job {

    @Lazy
    String jenkinsJobName = { getJobProperty("jobName") }()

    @Lazy
    String jenkinsBuildNumber = {
        getJobProperty("/myJob/jobSteps/RunAndWait/buildNumber")
    }()

    @Lazy
    String jenkinsBuildUrl = {
        getJobProperty("/myJob/report-urls/Jenkins Build #${jenkinsBuildNumber}")
    }()

    @Lazy
    String jenkinsBuildName = { jenkinsJobName + '#' + jenkinsBuildNumber }()

    @Lazy
    String jenkinsBuildDisplayName = { jenkinsJobName + ' #' + jenkinsBuildNumber }()

    @Lazy
    String jenkinsBuildLogs = {
        collectLogsFromJobStep(this.jobSteps.get(0)['calledProcedure']['jobStep'].find({
            it['stepName'].equals('Wait')
        }))
    }()

    @Lazy
    String fullLogs = {
        _retrieveFullLogs()
    }()

    JenkinsBuildJob(String jobId) {
        super(jobId)
    }

    @Override
    boolean isSuccess() {
        if (this.getJobProperty('outcome') != 'success') {
            return false
        }

        // TODO: condition may be to wide
        if (!jenkinsBuildLogs.contains("Finished: SUCCESS")) {
            System.err.println("Pipeline on Jenkins has failed. Showing 'Wait' step logs: \n")
            System.err.println(jenkinsBuildLogs)
            System.err.println("This log may be incomplete. Check getFullLogs() to get full console logs.")
            return false
        }

        return true
    }

    String _retrieveFullLogs() {
        String jobName = this.getJenkinsJobName()
        String number = this.getJenkinsBuildNumber()
        return JenkinsJobRunner.collectJenkinsLogs(jobName, number)
    }

    boolean consoleLogContains(String logMessage) {
        // Trying to avoid calling GetBuildLog, but the 'Wait' step logs are sometimes incomplete
        return this.logs.contains(logMessage) || this.fullLogs.contains(logMessage)
    }


}
