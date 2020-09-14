package com.electriccloud.plugin.spec.nativeplugin.E2E

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsProcedureJob
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsJobRunner
import com.electriccloud.plugins.annotations.Sanity

class FreestyleJob extends JenkinsHelper {

    private static final String PIPELINE_NAME = "nativeJenkinsPBASanityFreestyleProject"
    public static final String CI_CONFIG_NAME = "electricflow"

    private static JenkinsJobRunner jjr = JenkinsJobRunner.getInstance()

    def doSetupSpec() {
        // Do project import here
    }

    @Sanity
    def "Defaults - Freestyle Job"() {
        given: 'Parameters for the pipeline'
        def pipelineName = PIPELINE_NAME

        when: 'Run pipeline and collect run properties'
        JenkinsProcedureJob ciJob = jjr.run(pipelineName)

        then: 'Assuming everything has finished with success'
        assert ciJob.isSuccess(): "Pipeline on Jenkins is finished with success."
    }

}
