package com.electriccloud.plugin.spec.nativeplugin.utils

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.core.Job
import groovy.json.JsonSlurper

/**
 * Singleton jobs runner (to prevent importing of the same procedure in every suite)
 */
class JenkinsJobRunner {

    public static String configName = JenkinsHelper.randomize("configName")
    public static final String projectName = "Specs - electricflow-plugin - Helper"
    private static JenkinsJobRunner instance

    private static boolean runnerImported = false
    private static boolean configImported = false
    private static boolean loggerImported = false

    static JenkinsHelper jh = new JenkinsHelper()

    private JenkinsJobRunner() {}

    static JenkinsJobRunner getInstance() {
        if (instance != null) {
            return instance
        }
        instance = new JenkinsJobRunner()
        return instance
    }

    private static void initialize_config() {
        jh.setProperty('/plugins/EC-Jenkins/project/ec_debug_logToProperty', '1')
        jh.createConfiguration(configName)

        configImported = true
    }

    private static void initialize_runner() {
        configImported || initialize_config()

        jh.importProject(
                projectName, 'dsl/RunAndWait/Procedure.dsl',
                [projectName: projectName]
        )
        jh.importProject(
                projectName, 'dsl/Common/RunProcedure.dsl',
                [projectName: projectName, procedureName: 'JobAction', params: [
                        config_name: '',
                        job_name: '',
                        action: ''
                ]]
        )
        jh.importProject(
                projectName, 'dsl/Common/RunProcedure.dsl',
                [projectName: projectName, procedureName: 'GetBuildStatus', params: [
                        config_name: '',
                        job_name: '',
                        wait_for_build: ''
                ]]
        )
        jh.importProject(
                projectName, 'dsl/Common/RunProcedure.dsl',
                [projectName: projectName, procedureName: 'GetBuildDetails', params: [
                        config_name: '',
                        job_name: '',
                        result_outpp: ''
                ]]
        )

        runnerImported = true
    }

    private static void initialize_log() {
        configImported || initialize_config()

        jh.importProject(
                projectName, 'dsl/GetBuildLog/Procedure.dsl',
                [projectName: projectName]
        )

        loggerImported = true
    }

    JenkinsBuildJob run(String jobName, Map<String, String> buildParameters = null, String parallelMode = '0') {
        runnerImported || initialize_runner()

        String buildParamsStr = buildParameters.collect({ k, v -> "$k=$v" }).join(',')

        def code = """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'RunAndWait',
                    actualParameter: [
                        configName:                '$configName',
                        jobName:                   '$jobName',
                        buildParameters:           '$buildParamsStr',
                        jenkinsEnableParallelMode: '$parallelMode'
                    ]
                )
        """

        JenkinsBuildJob result = new JenkinsBuildJob(jh.dslWithTimeout(code)['jobId'] as String)
        println("Job Link: " + jh.getJobLink(result.jobId))

        if (result.getJobProperty('outcome') != 'success') {
            System.err.println("EC-Jenkins:RunAndWait job failed.")
            System.err.println(result.logs)
            return result
        }

        // Adding jenkins build related properties
        def buildNumber = result.getJenkinsBuildNumber()
        if (buildNumber == null) {
            throw new RuntimeException("EC-Jenkins:RunAndWait job does not contain a build number." +
                    " Looks like procedure has failed to start new build." + result.logs)
        }

        println("Jenkins Job : " + result.getJenkinsBuildUrl())

        return result
    }

    JenkinsBuildJob scanMBPipeline(String jobName, String branchName){
        runnerImported || initialize_runner()

        def scanMBPipelineCode = """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'JobAction',
                    actualParameter: [
                        config_name:                '$configName',
                        job_name:                   '$jobName',
                        action:                     'build'
                    ]
                )
            """
        def waitMBPipelineCode = """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'GetBuildStatus',
                    actualParameter: [
                        config_name:                '$configName',
                        job_name:                   '${jobName}/${branchName}',
                        wait_for_build:              '1'
                    ]
                )
            """
        def getMBPipelineDetailsCode = """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'GetBuildDetails',
                    actualParameter: [
                        config_name:                '$configName',
                        job_name:                   '${jobName}/${branchName}',
                        result_outpp:               '/myJobStep/buildDetails'
                    ]
                )
            """
        JenkinsMultiBranchPipelineBuildJob previousResultOfMBPipeline = new JenkinsMultiBranchPipelineBuildJob(jh.dslWithTimeout(getMBPipelineDetailsCode)['jobId'] as String)
        def prevBuildNumber = previousResultOfMBPipeline.getJenkinsBuildNumber()

        jh.dslWithTimeout(scanMBPipelineCode)
        jh.dslWithTimeout(waitMBPipelineCode)

        JenkinsMultiBranchPipelineBuildJob result
        for (int i=0; i<4; i++) {
            result = new JenkinsMultiBranchPipelineBuildJob(jh.dslWithTimeout(getMBPipelineDetailsCode)['jobId'] as String)
            def jsonBuildDetail = new JsonSlurper().parseText(result.buildDetails)

            // Sometimes Jenkins response contains null value for field "result". The problem is solved by making request again
            // or jenkins builds started by scan sometimes appear with delay, so we should be sure that new Jenkins build appeared
            if (jsonBuildDetail['result'] && result.getJenkinsBuildNumber() > prevBuildNumber){
                break
            }
            sleep(5000)
        }
        println("Job Link: " + jh.getJobLink(result.jobId))
        if (result.getJobProperty('outcome') != 'success') {
            System.err.println("EC-Jenkins:RunAndWait job failed.")
            System.err.println(result.logs)
            return result
        }

        return result

    }

    static String collectJenkinsLogs(String jobName, String buildNumber) {
        loggerImported || initialize_log()

        try {
            Job jenkinsLogsJob = (Job) jh.dslWithTimeout("""
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'GetBuildLog',
                    actualParameter: [
                        configName:   '$configName',
                        jobName:      '$jobName',
                        buildNumber:  '$buildNumber',
                        propertyPath: '/myJob/jenkinsJobLog'
                    ]
                )
            """)
            return jenkinsLogsJob.getJobProperty('jenkinsJobLog')
        } catch (RuntimeException ex) {
            System.err.println("Failed to get jenkins job logs: " + ex.getMessage())
        }
        return null
    }

    static void close() {
        if (!runnerImported && !loggerImported) return

//        jh.deleteConfiguration(configName)
//        jh.conditionallyDeleteProject(projectName)

        runnerImported = false
        loggerImported = false
        configImported = false
    }

    @Override
    protected void finalize() throws Throwable {
        close()
        super.finalize()
    }
}
