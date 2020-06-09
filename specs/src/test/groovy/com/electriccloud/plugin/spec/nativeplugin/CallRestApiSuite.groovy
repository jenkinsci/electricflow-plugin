package com.electriccloud.plugin.spec.nativeplugin

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsBuildJob
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsJobRunner
import spock.lang.Shared
import spock.lang.Unroll

class CallRestApiSuite extends JenkinsHelper {

    public static final String testPbaName = "cloudBeesFlowCallRestApi"
    private static final String testProjectName = "Specs-electricflow-plugin-$testPbaName"

    private static final String PIPELINE_NAME = "nativeJenkinsPBAExtendedPipelineProject"
    public static final String CI_CONFIG_NAME = "electricflow"

    @Shared
    String caseId
    @Shared
    String httpBody
    @Shared
    String envVarName
    @Shared
    String httpMethod
    @Shared
    String apiUrl
    @Shared
    String logMessage

    static def httpBodies = [
            empty        : '',
            createProject: """{"projectName":"$testProjectName","description":"Native Jenkins Test Proj","credentialName":"","tracked":"true","workspaceName":"Test TMP WorkSpace"}""",
            updateProject: """{"description":"Native Jenkins Test Proj 22222","credentialName":"","tracked":"true","workspaceName":"Test TMP WorkSpace 22222"}"""
    ]

    static def urls = [
            projects      : '/projects',
            createdProject: '/projects/' + URLEncoder.encode(testProjectName, "UTF-8"),
            incorrect     : '/incorrect'
    ]

    static def expectedLogMessages = [
            illegalMethod : 'java.lang.IllegalArgumentException: No enum constant ',
            badRequest    : 'HTTP error code : 400, Bad Request',
            notImplemented: 'HTTP error code : 501, Not Implemented'
    ]

    private static JenkinsJobRunner jjr = JenkinsJobRunner.getInstance()

    def doSetupSpec() {
        // Do project import here

        // Removing the project that we will create in tests
        dsl("deleteProject(projectName: '$testProjectName')")
    }

    @Unroll
    def "#caseId. CallRestApi - Method: #httpMethod"() {
        given: 'Parameters for the pipeline'

        if (httpBody) {
            httpBody = httpBody.replaceAll("\n", " ")
            httpBody = URLEncoder.encode(httpBody, "UTF-8")
        }

        def pipelineParameters = [
                runOnly                : testPbaName,
                buildParameters        : 'assemble --rerun-tasks test',
                flowConfigName         : CI_CONFIG_NAME,
                flowHTTPBody           : httpBody,
                flowEnvVarNameForResult: envVarName,
                flowHTTPMethod         : httpMethod,
                flowAPIURL             : apiUrl,
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(PIPELINE_NAME, pipelineParameters)
        assert ciJob.isSuccess(): "Pipeline on Jenkins is finished."

        then: 'Collecting the result objects'

        // Collect logs

        expect:
        assert true
        // Check logs

        where:
        caseId      | httpMethod | httpBody                 | apiUrl              | envVarName
        'C185520.3' | 'GET'      | httpBodies.empty         | urls.projects       | ''
        'C185520.5' | 'POST'     | httpBodies.createProject | urls.projects       | ''
        'C185520.6' | 'GET'      | httpBodies.empty         | urls.createdProject | ''
        'C185520.7' | 'PUT'      | httpBodies.updateProject | urls.createdProject | ''
        'C185520.8' | 'DELETE'   | httpBodies.empty         | urls.createdProject | ''
        'C185520.9' | 'POST'     | httpBodies.createProject | urls.projects       | ''
    }

    @Unroll
    def "#caseId. CallRestApi - Negative"() {
        given: 'Parameters for the pipeline'

        if (envVarName) {
            envVarName = URLEncoder.encode(envVarName, "UTF-8")
        }

        def pipelineParameters = [
                runOnly                : testPbaName,
                buildParameters        : 'assemble --rerun-tasks test',
                flowConfigName         : CI_CONFIG_NAME,
                flowHTTPBody           : httpBody,
                flowEnvVarNameForResult: envVarName,
                flowHTTPMethod         : httpMethod,
                flowAPIURL             : apiUrl,
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(PIPELINE_NAME, pipelineParameters)
        assert ciJob['jobId']
        assert ciJob.getOutcome() == 'success': "Pipeline was run by EC-Jenkins"


        then:
        assert !ciJob.isSuccess()
        assert ciJob.consoleLogContains(logMessage)

        where:
        caseId      | httpMethod  | httpBody         | apiUrl         | envVarName | logMessage
        'C500259.1' | 'POST'      | "incorrect"      | urls.projects  | ''         | expectedLogMessages.badRequest
        // This does not make build to fail
        // 'C500259.2' | 'GET'       | httpBodies.empty | urls.projects | '$ \'\\'        | expectedLogMessages.badRequest
        'C500259.3' | 'INCORRECT' | httpBodies.empty | urls.projects  | ''         | expectedLogMessages.illegalMethod
        'C500259.4' | 'GET'       | httpBodies.empty | urls.incorrect | ''         | expectedLogMessages.notImplemented

    }

}
