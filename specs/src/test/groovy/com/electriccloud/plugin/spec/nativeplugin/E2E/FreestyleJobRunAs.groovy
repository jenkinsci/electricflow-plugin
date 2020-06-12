package com.electriccloud.plugin.spec.nativeplugin.E2E

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsBuildJob
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsJobRunner
import com.electriccloud.plugins.annotations.Sanity

class FreestyleJobRunAs extends JenkinsHelper {

    private static final String PIPELINE_NAME = "nativeJenkinsRUNASSanityFreestyleProject"
    public static final String CI_CONFIG_NAME = "electricflow"

    private static JenkinsJobRunner jjr = JenkinsJobRunner.getInstance()

    def doSetupSpec() {
        // Do project import here
    }

    @Sanity
    def "Defaults - Freestyle Job with RunAs"() {
        given: 'Parameters for the pipeline'
        def pipelineName = PIPELINE_NAME

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(pipelineName)

        then: 'Assuming everything has finished with success'
        assert ciJob.isSuccess(): "Pipeline on Jenkins is finished with success."
    }

}
