package com.electriccloud.plugin.spec.nativeplugin

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.core.application.Application
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsProcedureJob
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsJobRunner
import spock.lang.Shared
import spock.lang.Unroll

class DeployApplicationSuite extends JenkinsHelper {

    public static final String testPbaName = "cloudBeesFlowDeployApplication"
    private static final String testProjectName = "Specs - electricflow-plugin - $testPbaName"

    private static final String PIPELINE_NAME = "nativeJenkinsPBAExtendedPipelineProject"
    private static final String CI_CONFIG_NAME = "electricflow"

    private static JenkinsJobRunner jjr = JenkinsJobRunner.getInstance()

    @Shared
    String projectName,
           app,
           appProcess,
           envName,
           stepId,
           expectedMessage

    static def projects = [
            correct: 'pvNativeJenkinsProject01',
            invalid: 'incorrect',
    ]

    static def apps = [
            correct: 'pvNativeJenkinsTestApplication01',
            invalid: 'incorrect'
    ]

    static def appProcesses = [
            correct: 'pvDeployProcess',
            invalid: 'incorrect'
    ]

    static def envs = [
            correct: 'pvEnvironment',
            invalid: 'incorrect'
    ]

    static def expectedLog = [
            notFound: 'HTTP error code : 404, Not Found'
    ]

    def doSetupSpec() {
        // Do project import here
    }

    def "C368023. Deploy Application"() {
        given: 'Parameters for the pipeline'

        String projectName = projects.correct
        String application = apps.correct
        String applicationProcess = appProcesses.correct
        String environmentName = envs.correct

        def applicationBefore = new Application(projectName, application)
        int runsBefore = applicationBefore.getProcessRunsCount(applicationProcess)

        def ciPipelineParameters = [
                flowConfigName        : CI_CONFIG_NAME,
                flowProjectName       : projectName,
                flowApplication       : application,
                flowApplicationProcess: applicationProcess,
                flowEnvironmentName   : environmentName,
                runOnly               : testPbaName
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsProcedureJob ciJob = jjr.run(PIPELINE_NAME, ciPipelineParameters)

        then: 'Collecting the result objects'
        assert ciJob.isSuccess(): "Pipeline on Jenkins is finished."
        def applicationAfter = new Application(projectName, application)
        int runsAfter = applicationAfter.getProcessRunsCount(applicationProcess)

        // Receiving extended information about the CI build details
        expect: 'Checking that new process run appeared'
        runsAfter != runsBefore
        applicationAfter.findProcess(applicationProcess).getRuns().last().isSuccess()
    }

    @Unroll
    def "C500313.#stepId. Deploy Application - Negative"() {
        given: 'Parameters for the pipeline'

        def ciPipelineParameters = [
                flowConfigName        : CI_CONFIG_NAME,
                flowProjectName       : projectName,
                flowApplication       : app,
                flowApplicationProcess: appProcess,
                flowEnvironmentName   : envName,
                runOnly               : testPbaName
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsProcedureJob ciJob = jjr.run(PIPELINE_NAME, ciPipelineParameters)

        then: 'Collecting the result objects'
        assert !ciJob.isSuccess(): "Pipeline on Jenkins is finished with error"
        assert ciJob.consoleLogContains(expectedMessage)

        where:
        stepId | projectName      | app          | appProcess           | envName      | expectedMessage
        '1'    | projects.invalid | apps.correct | appProcesses.correct | envs.correct | expectedLog.notFound
        '2'    | projects.correct | apps.invalid | appProcesses.correct | envs.correct | expectedLog.notFound
        '3'    | projects.correct | apps.correct | appProcesses.invalid | envs.correct | expectedLog.notFound
        '4'    | projects.correct | apps.correct | appProcesses.correct | envs.invalid | expectedLog.notFound
    }

}
