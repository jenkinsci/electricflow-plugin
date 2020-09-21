package com.electriccloud.plugin.spec.nativeplugin

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.core.cibuilddetails.CiBuildDetail
import com.electriccloud.plugin.spec.core.cibuilddetails.CiBuildDetailInfo
import com.electriccloud.plugin.spec.core.cibuilddetails.TestResults
import com.electriccloud.plugin.spec.core.pipeline.PipelineRun
import com.electriccloud.plugin.spec.core.release.Release
import com.electriccloud.plugin.spec.core.release.ReleasePipeline
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsBuildJob
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsJobRunner
import groovy.json.JsonSlurper
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Unroll

class TriggerReleaseSuite extends JenkinsHelper {

    public static final String testPbaName = "cloudBeesFlowTriggerRelease"
    private static final String testProjectName = "Specs - electricflow-plugin - $testPbaName"

    private static final String PIPELINE_NAME = "nativeJenkinsPBAExtendedPipelineProject"
    public static final String CI_CONFIG_NAME = "electricflow"

    public static ciConfigs = [
            correct: 'electricflow',
            incorrectPassword: 'incorrectPassword'
    ]

    public static def ciPipelinesNames = [
            runAndWait: 'TriggerReleaseRunAndWaitPipeline',
            MBPipeline: "MultiBranchPipeline2"
    ]

    private static JenkinsJobRunner jjr = JenkinsJobRunner.getInstance()

    private static def flowProjects = [
            correct       : 'pvNativeJenkinsProject01',
            withParameters: 'pvNativeJenkinsProject02',
            invalid       : 'incorrect'
    ]

    private static def flowReleases = [
            correct: 'pvRelease',
            invalid: 'incorrect',
            runAndWait: 'TriggerReleaseRunAndWait',
    ]

    private static def stages = [
            default: 'Stage 1',
            second : 'Stage 1 Copy 1',
            invalid: 'incorrect'
    ]

    private static def logMessages = [
            jsonIsNull : 'net.sf.json.JSONException: null object',
            noSuchStage: '"code":"NoSuchStage"',
            timing: "Waiting till CloudBees CD job is completed, checking every TIME seconds",
            jobOutcome: "CD Pipeline Runtime Details Response Data: .* status=completed, outcome=OUTCOME"
    ]

    @Shared
    String flowProjectName,
           flowReleaseName,
           flowStartingStage,
           caseId

    def doSetupSpec() {
        importJenkinsJob('TriggerReleaseRunAndWaitPipeline.xml', ciPipelinesNames.runAndWait)
        importJenkinsJob('MultiBranchPipeline2.xml', ciPipelinesNames.MBPipeline)
        dslFile('dsl/RunAndWait/runAndWaitProcedure.dsl')
        dslFile('dsl/RunAndWait/runAndWaitRelease.dsl', [releaseName: flowReleases.runAndWait])
        // Do project import here
    }

    @Unroll
    def "#caseId. TriggerRelease - #flowProjectName"() {
        given: 'Parameters for the pipeline'

        def releaseName = flowReleases.correct

        Release release = new Release(flowProjectName, releaseName)
        ReleasePipeline pipeline = release.getReleasePipeline()

        // Check last pipeline run
        PipelineRun previousPipelineRun = pipeline.getLastRun()

        def ciPipelineParameters = [
                flowConfigName   : CI_CONFIG_NAME,
                flowProjectName  : flowProjectName,
                flowReleaseName  : releaseName,
                flowStartingStage: flowStartingStage,
                runOnly          : testPbaName
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

        where:
        caseId    | flowProjectName             | flowStartingStage
        // Here project and stage are wired in the pipeline script, other combinations will not work
        'C367661' | flowProjects.correct        | stages.default
        'C368277' | flowProjects.withParameters | stages.second
    }

    @Unroll
    def "#caseId. TriggerRelease Run and wait"() {
        given: 'Parameters for the pipeline'


        Release release = new Release(cdProjectName, releaseName)
        ReleasePipeline pipeline = release.getReleasePipeline()

        // Check last pipeline run
        PipelineRun previousPipelineRun = pipeline.getLastRun()

        def ciPipelineParameters = [
                flowConfigName   : ciConfig,
                flowProjectName  : cdProjectName,
                flowReleaseName  : releaseName,
                dependOnCdJobOutcomeCh: dependOnCdJobOutcomeCh,
                runAndWaitInterval    : runAndWaitInterval,
                procedureOutcome: procedureOutcome,
                sleepTime: sleepTime,
                creds: creds
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(ciPipelinesNames.runAndWait, ciPipelineParameters)

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
        caseId    | ciConfig                    | cdProjectName               | releaseName              | dependOnCdJobOutcomeCh | runAndWaitInterval | ciJobOutcome  | procedureOutcome | sleepTime | creds | logMessage
        'C367661' | ciConfigs.correct           | flowProjects.correct        | flowReleases.runAndWait  | 'false'                | '5'                | 'SUCCESS'     | 'success'        | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C367661' | ciConfigs.correct           | flowProjects.correct        | flowReleases.runAndWait  | 'true'                 | '5'                | 'SUCCESS'     | 'success'        | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C367661' | ciConfigs.correct           | flowProjects.correct        | flowReleases.runAndWait  | 'true'                 | '5'                | 'UNSTABLE'    | 'warning'        | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C367661' | ciConfigs.correct           | flowProjects.correct        | flowReleases.runAndWait  | 'false'                | '5'                | 'SUCCESS'     | 'warning'        | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C367661' | ciConfigs.correct           | flowProjects.correct        | flowReleases.runAndWait  | 'true'                 | '5'                | 'FAILURE'     | 'error'          | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C367661' | ciConfigs.correct           | flowProjects.correct        | flowReleases.runAndWait  | 'false'                | '5'                | 'SUCCESS'     | 'error'          | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C367661' | ciConfigs.correct           | flowProjects.correct        | flowReleases.runAndWait  | 'true'                 | '15'               | 'SUCCESS'     | 'success'        | '4'       | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C367661' | ciConfigs.incorrectPassword | flowProjects.correct        | flowReleases.runAndWait  | 'false'                | '5'                | 'SUCCESS'     | 'success'        | '4'       | '4'   | [logMessages.timing, logMessages.jobOutcome]
    }

    @Unroll
    @Issue("NTVEPLUGIN-377")
    def "#caseId. TriggerRelease Run MultiBranch"() {
        given: 'Parameters for the pipeline'
        def gitFolder = gitHelper.pullAndCheckoutToBranch()
        gitHelper.createGitUserConfig(gitFolder)
        def commitMessages = []
        def commitChangeTypeMessage = gitHelper.replaceDefaultValueOfParameterInJenkinsFile("Jenkinsfile", "release", 'type', gitFolder)
        if (commitChangeTypeMessage) {
            commitMessages += commitChangeTypeMessage
        }
        commitMessages += gitHelper.addNewChangeToFile("filesForCommit.txt", "changes1", gitFolder)
        commitMessages += gitHelper.addNewChangeToFile("filesForCommit.txt", "changes2", gitFolder)
        gitHelper.gitPushToRemoteRepository("build/parametrizedQA", gitFolder)

        Release release = new Release(cdProjectName, releaseName)
        ReleasePipeline pipeline = release.getReleasePipeline()

        // Check last pipeline run
        PipelineRun previousPipelineRun = pipeline.getLastRun()

        def ciPipelineParameters = [
                flowConfigName   : ciConfig,
                flowProjectName  : cdProjectName,
                flowReleaseName  : releaseName,
                dependOnCdJobOutcomeCh: dependOnCdJobOutcomeCh,
                runAndWaitInterval    : runAndWaitInterval,
                procedureOutcome: procedureOutcome,
                sleepTime: sleepTime,
                type: "release",
        ]

        when: 'Run pipeline and collect run properties'

        JenkinsBuildJob ciJob
        if (launchByScan){
            ciJob = jjr.scanMBPipeline(ciPipelinesNames.MBPipeline, "build%2FparametrizedQA")
        }
        else {
            ciJob = jjr.run("${ciPipelinesNames.MBPipeline}/build%2FparametrizedQA", ciPipelineParameters)
        }

        then: 'Collecting the result objects'
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

        CiBuildDetailInfo ciBuildDetailInfo = newPipelineRun.findCiBuildDetailInfo("${ciPipelinesNames.MBPipeline} » build/parametrizedQA #" + buildNumber)
        CiBuildDetail cbd = ciBuildDetailInfo?.getCiBuildDetail()

        def changesSets =  new JsonSlurper().parseText(ciBuildDetailInfo.ciBuildDetail.dslObject['buildData'])["changeSets"]
        // Receiving extended information about the CI build details
        expect: 'Checking the CiBuildDetail values'
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
            ciBuildDetailInfo['displayName'] == "${ciPipelinesNames.MBPipeline} » build/parametrizedQA #" + buildNumber

            if (launchByScan) {
                ciBuildDetailInfo['launchedBy'] == "Branch indexing"
            }
            else {
                ciBuildDetailInfo['launchedBy'] == "Started by user admin"
            }
        }

        where:
        caseId      | ciConfig                    | cdProjectName               | releaseName              | dependOnCdJobOutcomeCh | runAndWaitInterval | ciJobOutcome  | procedureOutcome | sleepTime | launchByScan | creds | logMessage
        'C367661_1' | ciConfigs.correct           | flowProjects.correct        | flowReleases.runAndWait  | 'false'                | '5'                | 'SUCCESS'     | 'success'        | '4'       | false        | ''    | [logMessages.timing, logMessages.jobOutcome]
        'C367661_2' | ciConfigs.correct           | flowProjects.correct        | flowReleases.runAndWait  | 'false'                | '5'                | 'SUCCESS'     | 'success'        | '4'       | true         | ''    | [logMessages.timing, logMessages.jobOutcome]
    }

    @Unroll
    @Issue("NTVEPLUGIN-318")
    def "#caseId. TriggerRelease - Negative"() {
        given: 'Parameters for the pipeline'

        def ciPipelineParameters = [
                flowConfigName   : CI_CONFIG_NAME,
                flowProjectName  : flowProjectName,
                flowReleaseName  : flowReleaseName,
                flowStartingStage: flowStartingStage,
                runOnly          : testPbaName
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(PIPELINE_NAME, ciPipelineParameters)

        then: 'Checking the result'
        assert ciJob.getOutcome() == 'success': "Pipeline on Jenkins was started."
        assert !ciJob.isSuccess(): "Pipeline on Jenkins is finished with error."
        assert ciJob.consoleLogContains(logMessage)

        where:
        caseId       | flowProjectName      | flowReleaseName      | flowStartingStage | logMessage
        'C500258.1'  | flowProjects.invalid | flowReleases.correct | stages.correct    | logMessages.jsonIsNull
        'C500258.2'  | flowProjects.correct | flowReleases.invalid | stages.correct    | logMessages.noSuchStage
        'C500258.3'  | flowProjects.correct | flowReleases.correct | stages.invalid    | logMessages.noSuchStage

        // Dev: Stage does not exist in the release
        'C500258.xx' | flowProjects.correct | flowReleases.correct | stages.second     | logMessages.noSuchStage
    }

}
