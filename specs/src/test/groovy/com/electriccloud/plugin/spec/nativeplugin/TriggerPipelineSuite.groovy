package com.electriccloud.plugin.spec.nativeplugin

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.core.cibuilddetails.CiBuildDetail
import com.electriccloud.plugin.spec.core.cibuilddetails.CiBuildDetailInfo
import com.electriccloud.plugin.spec.core.cibuilddetails.TestResults
import com.electriccloud.plugin.spec.core.pipeline.Pipeline
import com.electriccloud.plugin.spec.core.pipeline.PipelineRun
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsBuildJob
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsJobRunner
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsMultiBranchPipelineBuildJob
import groovy.json.JsonSlurper
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Unroll

class TriggerPipelineSuite extends JenkinsHelper {

    public static final String testPbaName = "cloudBeesFlowRunPipeline"
    private static final String testProjectName = "Specs - electricflow-plugin - $testPbaName"

    private static final String PIPELINE_NAME = "nativeJenkinsPBAExtendedPipelineProject"
    public static final String CI_CONFIG_NAME = "electricflow"

    public static ciConfigs = [
            correct: 'electricflow',
            incorrectPassword: 'incorrectPassword'
    ]

    public static def ciPipelinesName = [
            runAndWait: 'RunPipelineRunAndWaitPipeline',
    ]

    private static JenkinsJobRunner jjr = JenkinsJobRunner.getInstance()

    def doSetupSpec() {
        importJenkinsJob('RunPipelineRunAndWaitPipeline.xml', ciPipelinesName.runAndWait)
        dslFile('dsl/RunAndWait/runAndWaitProcedure.dsl')
        dslFile('dsl/RunAndWait/runAndWaitPipeline.dsl')
        // Do project import here
    }

    static def projects = [
            correct: 'pvNativeJenkinsProject01',
            invalid: 'incorrect'
    ]

    static def pipelines = [
            correct: 'pvNativeJenkinsTestPipeline01',
            invalid: 'incorrect',
            runAndWait: 'runProcedureRunAndWait'
    ]

    static def logMessages = [
            failedFormalParametersRetrieve: 'Error occurred during formal parameters fetch',
            pipelineIdFailed              : 'Failed to retrieve Id for pipeline',
            timing: "Waiting till CloudBees CD job is completed, checking every TIME seconds",
            defaultTiming: "Waiting till CloudBees CD pipeline is completed, checking every 5 seconds",
            jobOutcome: "CD Pipeline Runtime Details Response Data: .* status=completed, outcome=OUTCOME"
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
    def "Run Pipeline. Run and Wait"() {
        given: 'Parameters for the pipeline'

        Pipeline pipeline = new Pipeline(flowProjectName, flowPipelineName)
        PipelineRun previousPipelineRun = pipeline.getLastRun()

        def ciPipelineParameters = [
                flowConfigName  : ciConfig,
                flowProjectName : flowProjectName,
                flowPipelineName: flowPipelineName,
                dependOnCdJobOutcomeCh: dependOnCdJobOutcomeCh,
                runAndWaitInterval    : runAndWaitInterval,
                procedureOutcome: procedureOutcome,
                sleepTime: sleepTime,
                creds: creds
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(ciPipelinesName.runAndWait, ciPipelineParameters)

        then: 'Collecting the result objects'
        assert ciJob.getCiJobOutcome() == ciJobOutcome

        String buildName = ciJob.getJenkinsBuildDisplayName()
        pipeline.refresh()
        PipelineRun newPipelineRun = pipeline.pipelineRuns.last()
        if (previousPipelineRun != null) {
            int prevNumber = previousPipelineRun.getNumber()
            int newNumber = newPipelineRun.getNumber()
            assert newNumber > prevNumber: 'new number is greater than previous'
        }

        // NTVEPLUGIN-378
        assert !(ciJob.logs.contains('Unauthorized'))

        CiBuildDetailInfo ciBuildDetailInfo = newPipelineRun.findCiBuildDetailInfo(buildName)
        CiBuildDetail cbd = ciBuildDetailInfo?.getCiBuildDetail()

        // Receiving extended information about the CI build details
        expect: 'Checking the CiBuildDetail values'
        verifyAll { // soft assert. Will show all the failed cases
            ciBuildDetailInfo['associationType'] == 'triggeredByCI'
            if (dependOnCdJobOutcomeCh.toBoolean()){
                ciBuildDetailInfo['result'] == ciJobOutcome
            }
            else {
                ciBuildDetailInfo['result'] == "SUCCESS"
            }
            cbd['buildTriggerSource'] == "CI"
        }

        where:
        caseId      | ciConfig                    | flowProjectName  | flowPipelineName       | dependOnCdJobOutcomeCh | runAndWaitInterval  | ciJobOutcome     | procedureOutcome  | sleepTime | creds | logMessage
        'C519154'   | ciConfigs.correct           | projects.correct | pipelines.runAndWait   | 'false'                | '5'                 | 'SUCCESS'        | 'success'         | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C519155'   | ciConfigs.correct           | projects.correct | pipelines.runAndWait   | 'true'                 | '5'                 | 'SUCCESS'        | 'success'         | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C519156'   | ciConfigs.correct           | projects.correct | pipelines.runAndWait   | 'true'                 | '5'                 | 'UNSTABLE'       | 'warning'         | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C519157'   | ciConfigs.correct           | projects.correct | pipelines.runAndWait   | 'false'                | '5'                 | 'SUCCESS'        | 'warning'         | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C519158'   | ciConfigs.correct           | projects.correct | pipelines.runAndWait   | 'true'                 | '5'                 | 'FAILURE'        | 'error'           | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C519159'   | ciConfigs.correct           | projects.correct | pipelines.runAndWait   | 'false'                | '5'                 | 'SUCCESS'        | 'error'           | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C519160'   | ciConfigs.correct           | projects.correct | pipelines.runAndWait   | 'true'                 | '15'                | 'SUCCESS'        | 'success'         | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C519155'   | ciConfigs.incorrectPassword | projects.correct | pipelines.runAndWait   | 'true'                 | '5'                 | 'SUCCESS'        | 'success'         | '4'       | '4'   | [logMessages.timing, logMessages.jobOutcome]
        'NTPLGN-367'| ciConfigs.correct           | projects.correct | pipelines.runAndWait   | 'false'                | '5'                 | 'SUCCESS'        | 'success'         | '0'       | ''    | [logMessages.defaultTiming, logMessages.jobOutcome]
        'NTPLGN-367'| ciConfigs.correct           | projects.correct | pipelines.runAndWait   | 'false'                | '5'                 | 'SUCCESS'        | 'success'         | '-1'      | ''    | [logMessages.defaultTiming, logMessages.jobOutcome]
    }

    @Unroll
    @Issue("NTVEPLUGIN-377")
    // file "filesForCommit.txt" should exist for this case in the branch build/parametrizedQA
    def "Run MultiBranch"() {
        given: "Clone repository and make and push a commit to remote branch and Parameters for the pipeline"
        def gitFolder = gitHelper.pullAndCheckoutToBranch()
        def commitMessages = []
        commitMessages += gitHelper.addNewChangeToFile("filesForCommit.txt", "changes1", gitFolder)
        commitMessages += gitHelper.addNewChangeToFile("filesForCommit.txt", "changes2", gitFolder)
        gitHelper.createGitUserConfig(gitFolder)
        gitHelper.gitPushToRemoteRepository("build/parametrizedQA", gitFolder)

        Pipeline pipeline = new Pipeline(flowProjectName, flowPipelineName)
        PipelineRun previousPipelineRun = pipeline.getLastRun()

        def ciPipelineParameters = [
                flowConfigName  : ciConfig,
                flowProjectName : flowProjectName,
                flowPipelineName: flowPipelineName,
                dependOnCdJobOutcomeCh: dependOnCdJobOutcomeCh,
                runAndWaitInterval    : runAndWaitInterval,
                procedureOutcome: procedureOutcome,
                sleepTime: sleepTime,
                creds: creds
        ]

        when: 'Run pipeline and collect run properties'

        JenkinsBuildJob ciJob
        if (launchByScan){
            ciJob = jjr.scanMBPipeline("MultiBranchPipeline", "build%2FparametrizedQA")
        }
        else {
            ciJob = jjr.run("MultiBranchPipeline/build%2FparametrizedQA", ciPipelineParameters)
        }

        then: 'Collecting the result objects'
        println(ciJob.getClass())
        assert ciJob.getCiJobOutcome() == ciJobOutcome

        String buildNumber = ciJob.getJenkinsBuildNumber()
        pipeline.refresh()
        PipelineRun newPipelineRun = pipeline.pipelineRuns.last()
        if (previousPipelineRun != null) {
            int prevNumber = previousPipelineRun.getNumber()
            int newNumber = newPipelineRun.getNumber()
            assert newNumber > prevNumber: 'new number is greater than previous'
        }

        // NTVEPLUGIN-378
        assert !(ciJob.logs.contains('Unauthorized'))

        CiBuildDetailInfo ciBuildDetailInfo = newPipelineRun.findCiBuildDetailInfo("MultiBranch Pipeline » build/parametrizedQA #" + buildNumber)
        CiBuildDetail cbd = ciBuildDetailInfo?.getCiBuildDetail()
        def changesSets =  new JsonSlurper().parseText(ciBuildDetailInfo.ciBuildDetail.dslObject['buildData'])["changeSets"]

        // Receiving extended information about the CI build details
        expect: 'Checking the CiBuildDetail values'
        println(ciBuildDetailInfo)
        verifyAll { // soft assert. Will show all the failed cases
            ciBuildDetailInfo['associationType'] == 'triggeredByCI'
            changesSets.collect{ it['commitMessage'] }.sort() == commitMessages.sort()
            if (dependOnCdJobOutcomeCh.toBoolean()){
                ciBuildDetailInfo['result'] == ciJobOutcome
            }
            else {
                ciBuildDetailInfo['result'] == "SUCCESS"
            }
            cbd['buildTriggerSource'] == "CI"
            ciBuildDetailInfo['jobBranchName'] == "build/parametrizedQA"
            ciBuildDetailInfo['displayName'] == "MultiBranch Pipeline » build/parametrizedQA #" + buildNumber

            if (launchByScan) {
                ciBuildDetailInfo['launchedBy'] == "Branch indexing"
            }
            else {
                ciBuildDetailInfo['launchedBy'] == "Started by user admin"
            }
        }


        where:
        caseId      | ciConfig                    | flowProjectName  | flowPipelineName       | dependOnCdJobOutcomeCh | runAndWaitInterval  | ciJobOutcome     | procedureOutcome  | sleepTime | creds | launchByScan | logMessage
        'C519154'   | ciConfigs.correct           | projects.correct | pipelines.runAndWait   | 'false'                | '5'                 | 'SUCCESS'        | 'success'         | '4'       | ''    | false        | [logMessages.timing, logMessages.jobOutcome]
        'C519154'   | ciConfigs.correct           | projects.correct | pipelines.runAndWait   | 'false'                | '5'                 | 'SUCCESS'        | 'success'         | '4'       | ''    | true         | [logMessages.timing, logMessages.jobOutcome]
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
        'C500311.1' | projects.invalid | pipelines.correct | logMessages.pipelineIdFailed
        'C500311.2' | projects.correct | pipelines.invalid | logMessages.pipelineIdFailed
    }

}
