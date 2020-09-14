package com.electriccloud.plugin.spec.nativeplugin.utils

class JenkinsCLIJob implements JenkinsJob {

    String jenkinsUrl
    String jenkinsJobName
    String jenkinsBuildLogs

    @Lazy
    String fullLogs = { jenkinsBuildLogs }

    @Lazy
    String jenkinsBuildNumber = { 1 }

    @Lazy
    String jenkinsBuildUrl = '<URL>'

    @Lazy
    String outcome = { getCiJobOutcome() }

    String jenkinsBuildName = '<jenkinsBuildName>'

    String jenkinsBuildDisplayName = '<jenkinsBuildDisplayName>'

    @Override
    boolean isSuccess() {
        return getCiJobOutcome() == 'SUCCESS'
    }

    @Override
    String getCiJobOutcome(){
        String outcomeValue = ''
        if (jenkinsBuildLogs.contains("Finished: SUCCESS")) {
            outcomeValue = 'SUCCESS'
        }
        if (jenkinsBuildLogs.contains("Finished: UNSTABLE")) {
            outcomeValue = 'UNSTABLE'
        }
        if (jenkinsBuildLogs.contains("Finished: FAILURE")) {
            outcomeValue = 'FAILURE'
        }
        return outcomeValue
    }

    @Override
    boolean consoleLogContains(String logMessage) {
        return fullLogs.contains(logMessage)
    }
}
