package com.electriccloud.plugin.spec.nativeplugin.E2E

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsBuildJob
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsJobRunner
import com.electriccloud.plugins.annotations.Sanity

class PipelineRunAs extends JenkinsHelper {

    private static final String PIPELINE_NAME = "nativeJenkinsRUNASExtendedPipelineProject"
    public static final String CI_CONFIG_NAME = "electricflow"

    private static JenkinsJobRunner jjr = JenkinsJobRunner.getInstance()

    def doSetupSpec() {
        // Do project import here
    }

    @Sanity
    def "Defaults - Pipeline project with RunAs"() {
        given: 'Parameters for the pipeline'
        def ciPipelineParameters = [
                flowConfigName: CI_CONFIG_NAME,
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(PIPELINE_NAME, ciPipelineParameters)

        then: 'Assuming everything has finished with success'
        assert ciJob.isSuccess(): "Pipeline on Jenkins is finished with success."
    }

}
