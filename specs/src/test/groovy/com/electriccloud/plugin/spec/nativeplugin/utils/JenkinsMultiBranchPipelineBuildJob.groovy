package com.electriccloud.plugin.spec.nativeplugin.utils

import groovy.json.JsonSlurper

class JenkinsMultiBranchPipelineBuildJob extends JenkinsBuildJob {

    @Lazy
    String jenkinsBuildNumber = {
       getJobProperty("/myJob/jobSteps/GetBuildDetails/buildNumber")
    }()

    @Lazy
    String buildDetails = {
        getJobProperty("/myJob/jobSteps/GetBuildDetails/jobSteps/GetBuildDetails/buildDetails")
    }()

    JenkinsMultiBranchPipelineBuildJob(String jobId) {
        super(jobId)
    }


    @Override
    String getCiJobOutcome(){
        def jsonBuildDetail = new JsonSlurper().parseText(buildDetails)
        return jsonBuildDetail['result']
    }
}
