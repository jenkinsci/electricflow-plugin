package com.electriccloud.plugin.spec.nativeplugin

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.core.cibuilddetails.CiBuildDetail
import com.electriccloud.plugin.spec.core.cibuilddetails.CiBuildDetailInfo
import com.electriccloud.plugin.spec.core.cibuilddetails.TestResults
import com.electriccloud.plugin.spec.core.pipeline.Pipeline
import com.electriccloud.plugin.spec.core.pipeline.PipelineRun
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsBuildJob
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsJobRunner
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Unroll

class TriggerPipelineSuite extends JenkinsHelper {

    public static final String testPbaName = "cloudBeesFlowRunPipeline"
    private static final String testProjectName = "Specs - electricflow-plugin - $testPbaName"

    private static final String PIPELINE_NAME = "nativeJenkinsPBAExtendedPipelineProject"
    public static final String CI_CONFIG_NAME = "electricflow"

    private static JenkinsJobRunner jjr = JenkinsJobRunner.getInstance()

    def doSetupSpec() {
        // Do project import here
    }

    static def projects = [
            correct: 'pvNativeJenkinsProject01',
            invalid: 'incorrect'
    ]

    static def pipelines = [
            correct: 'pvNativeJenkinsTestPipeline01',
            invalid: 'incorrect'
    ]

    static def logMessages = [
            failedFormalParametersRetrieve: 'Error occurred during formal parameters fetch'
    ]

    @Shared
    String caseId, logMessage

    def "C388038. TriggerPipeline"() {
        given: 'Parameters for the pipeline'

        def flowProjectName = projects.correct
        def flowPipelineName = pipelines.correct

        // Check last pipeline run
        Pipeline pipeline = new Pipeline(flowProjectName, flowPipelineName)
        PipelineRun previousPipelineRun = pipeline.getLastRun()

        def ciPipelineParameters = [
                flowConfigName  : CI_CONFIG_NAME,
                flowProjectName : flowProjectName,
                flowPipelineName: flowPipelineName,
                runOnly         : testPbaName
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(PIPELINE_NAME, ciPipelineParameters)

        then: 'Collecting the result objects'
        assert ciJob.isSuccess(): "Pipeline on Jenkins is finished."
        String buildName = ciJob.getJenkinsBuildDisplayName()

        pipeline.refresh()
        PipelineRun newPipelineRun = pipeline.pipelineRuns.last()

        if (previousPipelineRun != null) {
            int prevNumber = previousPipelineRun.getNumber()
            int newNumber = newPipelineRun.getNumber()
            assert newNumber > prevNumber: 'new number is greater than previous'
        }

        CiBuildDetailInfo ciBuildDetailInfo = newPipelineRun.findCiBuildDetailInfo(buildName)
        CiBuildDetail cbd = ciBuildDetailInfo?.getCiBuildDetail()
        TestResults tr = ciBuildDetailInfo?.getTestResults()

        // Receiving extended information about the CI build details
        expect: 'Checking the CiBuildDetail values'
        verifyAll { // soft assert. Will show all the failed cases
            ciBuildDetailInfo['associationType'] == 'triggeredByCI'
            ciBuildDetailInfo['result'] == "SUCCESS"
            cbd['buildTriggerSource'] == "CI"
            tr.getTotalCount() == 3
            tr.getPassPercentage() == 100
            tr.getFailPercentage() == 0
        }
    }

    @Unroll
    @Issue("NTVEPLUGIN-319")
    def "#caseId. TriggerPipeline - Negative"() {
        given: 'Parameters for the pipeline'

        def ciPipelineParameters = [
                flowConfigName  : CI_CONFIG_NAME,
                flowProjectName : flowProjectName,
                flowPipelineName: flowPipelineName,
                runOnly         : testPbaName
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(PIPELINE_NAME, ciPipelineParameters)

        then: 'Collecting the result objects'
        assert ciJob.getOutcome() == 'success': "Pipeline on Jenkins was started"
        assert !ciJob.isSuccess(): "Pipeline on Jenkins is finished with error."
        assert ciJob.consoleLogContains(logMessage)

        where:
        caseId      | flowProjectName  | flowPipelineName  | logMessage
        'C500311.1' | projects.invalid | pipelines.correct | logMessages.failedFormalParametersRetrieve
        'C500311.2' | projects.correct | pipelines.invalid | logMessages.failedFormalParametersRetrieve
    }

}
