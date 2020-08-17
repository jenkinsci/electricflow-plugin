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
            invalid: 'incorrect',
            runAndWait: 'runAndWaitProcedure'
    ]

    static def logMessages = [
            noSuchProcedure: '"code":"NoSuchProcedure"',
            noSuchProject  : '"code":"NoSuchProject"',
            timing: "Waiting till CloudBees CD job is completed, checking every TIME seconds",
            jobOutcome: "CD Job Status Response Data: .* status=completed, outcome=OUTCOME"
    ]

    @Shared
    String projectName, procedureName, caseId, logMessage

    def doSetupSpec() {
        dslFile('dsl/RunAndWait/runAndWaitProcedure.dsl')
        // Do project import here
    }

    def "C368021. Run Procedure"() {
        given: 'Parameters for the pipeline'

        def projectName = projects.correct
        def procedureName = procedures.correct

        ArrayList<Job> jobsBefore = Job.findJobsOfProcedure(procedureName)

        def ciPipelineParameters = [
                flowConfigName        : CI_CONFIG_NAME,
                flowProjectName       : projectName,
                flowProcedureName     : procedureName,
                dependOnCdJobOutcomeCh: 'true',
                runAndWaitInterval    : '5',
                runOnly               : testPbaName
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
    def "Run Procedure. Run and Wait"() {
        given: 'Parameters for the pipeline'

        ArrayList<Job> jobsBefore = Job.findJobsOfProcedure(procedureName)

        def ciPipelineParameters = [
                flowConfigName        : CI_CONFIG_NAME,
                flowProjectName       : projectName,
                flowProcedureName     : procedureName,
                dependOnCdJobOutcomeCh: dependOnCdJobOutcomeCh,
                runAndWaitInterval    : runAndWaitInterval,
                value1                : cdJobOutcome,
                value2                : '5'
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run("RunProcedureRunAndWaitPipeline", ciPipelineParameters)

        then: 'Collecting the result objects'
        assert ciJob.getCiJobOutcome() == ciJobOutcome

        // NTVEPLUGIN-378
        assert !(ciJob.logs.contains('Unauthorized'))

        then: "Checking that we have one new job for the procedure"
        ArrayList<Job> jobsAfter = Job.findJobsOfProcedure(procedureName)
        assert jobsAfter.size() - jobsBefore.size() == 1

            then: "Checking that last job has finished with expected result"
            assert jobsAfter.last().outcome == cdJobOutcome

            for (message in logMessage) {
                assert ciJob.logs =~ logMessage
                .replace('TIME', runAndWaitInterval).replace('OUTCOME', cdJobOutcome)
        }

        where:
        caseId      | projectName      | procedureName         | dependOnCdJobOutcomeCh | runAndWaitInterval | ciJobOutcome  | cdJobOutcome | logMessage
        'C519130'   | projects.correct | procedures.runAndWait | 'false'                | '5'                | 'SUCCESS'     | 'success'    | [logMessages.timing, logMessages.jobOutcome]
        'C519131'   | projects.correct | procedures.runAndWait | 'true'                 | '5'                | 'SUCCESS'     | 'success'    | [logMessages.timing, logMessages.jobOutcome]
        'C519136'   | projects.correct | procedures.runAndWait | 'true'                 | '5'                | 'UNSTABLE'    | 'warning'    | [logMessages.timing, logMessages.jobOutcome]
        'C519137'   | projects.correct | procedures.runAndWait | 'false'                | '5'                | 'SUCCESS'     | 'warning'    | [logMessages.timing, logMessages.jobOutcome]
        'C519132'   | projects.correct | procedures.runAndWait | 'true'                 | '5'                | 'FAILURE'     | 'error'      | [logMessages.timing, logMessages.jobOutcome]
        'C519133'   | projects.correct | procedures.runAndWait | 'false'                | '5'                | 'SUCCESS'     | 'error'      | [logMessages.timing, logMessages.jobOutcome]
        'C519134'   | projects.correct | procedures.runAndWait | 'true'                 | '10'               | 'SUCCESS'     | 'success'    | [logMessages.timing, logMessages.jobOutcome]
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
