package com.electriccloud.plugin.spec.nativeplugin

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.core.Job
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsBuildJob
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsJobRunner
import spock.lang.Shared
import spock.lang.Unroll

class RunProcedureSuite extends JenkinsHelper {

    public static final String testPbaName = "cloudBeesFlowRunProcedure"
    private static final String testProjectName = "Specs - electricflow-plugin - $testPbaName"

    private static final String PIPELINE_NAME = "nativeJenkinsPBAExtendedPipelineProject"
    public static final String CI_CONFIG_NAME = "electricflow"

    private static JenkinsJobRunner jjr = JenkinsJobRunner.getInstance()

    static def projects = [
            correct: 'pvNativeJenkinsProject01',
            invalid: 'incorrect',
    ]

    static def procedures = [
            correct: 'nativeJenkinsTestProcedure',
            invalid: 'incorrect'
    ]

    static def logMessages = [
            noSuchProcedure: '"code":"NoSuchProcedure"',
            noSuchProject  : '"code":"NoSuchProject"'
    ]

    @Shared
    String projectName, procedureName, caseId, logMessage

    def doSetupSpec() {
        // Do project import here
    }

    def "C368021. Run Procedure"() {
        given: 'Parameters for the pipeline'

        def projectName = projects.correct
        def procedureName = procedures.correct

        ArrayList<Job> jobsBefore = Job.findJobsOfProcedure(procedureName)

        def ciPipelineParameters = [
                flowConfigName   : CI_CONFIG_NAME,
                flowProjectName  : projectName,
                flowProcedureName: procedureName,
                runOnly          : testPbaName
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(PIPELINE_NAME, ciPipelineParameters)

        then: 'Collecting the result objects'
        assert ciJob.isSuccess(): "Pipeline on Jenkins is finished."

        then: "Checking that we have one new job for the procedure"
        ArrayList<Job> jobsAfter = Job.findJobsOfProcedure(procedureName)
        assert jobsAfter.size() - jobsBefore.size() == 1

        then: "Checking that last job has finished with success"
        assert jobsAfter.last().isSuccess()
    }

    @Unroll
    def "#caseId. Run Procedure - Negative"() {
        def ciPipelineParameters = [
                flowConfigName   : CI_CONFIG_NAME,
                flowProjectName  : projectName,
                flowProcedureName: procedureName,
                runOnly          : testPbaName
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(PIPELINE_NAME, ciPipelineParameters)

        then: 'Collecting the result objects'
        assert ciJob.getOutcome() == 'success': "Pipeline on Jenkins was finished."
        assert !ciJob.isSuccess()
        assert ciJob.consoleLogContains(logMessage)

        where:
        caseId      | projectName      | procedureName      | logMessage
        'C500308.1' | projects.correct | procedures.invalid | logMessages.noSuchProcedure
        'C500308.2' | projects.invalid | procedures.correct | logMessages.noSuchProcedure
    }


}
