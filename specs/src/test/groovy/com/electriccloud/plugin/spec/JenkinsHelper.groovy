package com.electriccloud.plugin.spec


import com.electriccloud.spec.PluginSpockTestSupport
import spock.util.concurrent.PollingConditions

class JenkinsHelper extends PluginSpockTestSupport {
    static def pluginName = "EC-Jenkins"
    static def automationTestsContextRun = System.getenv('AUTOMATION_TESTS_CONTEXT_RUN') ?: ''
    static def pluginVersion = System.getenv('PLUGIN_VERSION') ?: ''

    static final Map DEFAULT_JENKINS_PARAMS = [
            url     : 'http://jenkins:8080',
            username: 'admin',
            password: 'changeme'
    ]

    def deleteConfiguration(String configName) {
        def result = dsl """
            runProcedure(
                projectName: "/plugins/${pluginName}/project",
                procedureName: 'DeleteConfiguration',
                actualParameter: [
                    config: '$configName'
                ]
            )
        """

        assert result.jobId
        waitUntil {
            // jobSucceeded(result.jobId)
            try {
                jobCompleted(result.jobId)
            } catch (Exception e) {
                logger.debug(e.getMessage())
            }
        }
    }

    def dslWithTimeout(dslString, timeout = 3600) {
        def result = dsl(dslString)
        PollingConditions poll = new PollingConditions(timeout: timeout, initialDelay: 0, factor: 1.25)
        poll.eventually {
            jobStatus(result.jobId).status == 'completed'
        }
        return result
    }

    def doesConfExist(def configName) {
        return doesConfExist("/plugins/${pluginName}/project/Jenkins_cfgs", configName)
    }

    def conditionallyDeleteConfiguration(String configName) {
        if (System.getenv("RECREATE_CONFIG") == 'true') {
            deleteConfiguration(pluginName, configName)
        }
    }

    def createConfiguration(String configName, options = [:]) {
        return createConfiguration(configName, [:], options)
    }
    //def createConfiguration(String configName, params = [:], options = [:]) {
    def createConfiguration(String configName, def params, def options) {
        if (doesConfExist(configName)) {
            if (System.getenv("RECREATE_CONFIG") == 'true') {
                deleteConfiguration(pluginName, configName)
            } else {
                logger.debug("Configuration $configName exists")
                return configName
            }
        }

        def username = System.getenv('JENKINS_USERNAME') ?: DEFAULT_JENKINS_PARAMS.username
        def password = System.getenv('JENKINS_PASSWORD') ?: DEFAULT_JENKINS_PARAMS.password
        def url = System.getenv('JENKINS_URL') ?: DEFAULT_JENKINS_PARAMS.url

        def efProxyUrl = System.getenv('EF_PROXY_URL') ?: ''
        def efProxyUsername = System.getenv('EF_PROXY_USERNAME') ?: ''
        def efProxyPassword = System.getenv('EF_PROXY_PASSWORD') ?: ''

        def coreTeam = ''
        if (System.getenv("JENKINS_TYPE") ?: '' == 'Core') {
            coreTeam = System.getenv('CORE_TEAM') ?: ''
        }

        if (params.userName) {
            username = params.userName
        }
        if (params.password) {
            password = params.password
        }
        if (params.url) {
            url = params.url
        }

        def result
        // create configuration with proxy only when proxy env is available.
        if (efProxyUrl != '') {
            result = dsl """
            runProcedure(
                projectName: "/plugins/${pluginName}/project",
                procedureName: 'CreateConfiguration',
                credential: [
                    [
                        credentialName: 'proxy_credential',
                        userName: '$efProxyUsername',
                        password: '$efProxyPassword'
                    ],
                    [
                        credentialName: 'credential',
                        userName: '$username',
                        password: '$password'
                    ],
                ],
                actualParameter: [
                    config: '$configName',
                    server: '$url',
                    credential: 'credential',
                    http_proxy: '$efProxyUrl',
                    proxy_credential: 'proxy_credential',
                ]
            )
            """
        }
        // There is no proxy, regular creation.
        else {
            def jobs_location_dsl = (coreTeam == '') ? '' : """,jobs_location: '$coreTeam'"""

            result = dsl """
            runProcedure(
                projectName: "/plugins/${pluginName}/project",
                procedureName: 'CreateConfiguration',
                credential: [
                    credentialName: 'credential',
                    userName: '$username',
                    password: '$password'
                ],
                actualParameter: [
                    config: '$configName',
                    server: '$url',
                    credential: 'credential'
                    $jobs_location_dsl
                ]
            )
            """
        }
        assert result.jobId
        waitUntil {
            try {
                //jobCompleted(result.jobId)
                jobCompleted(result)
            } catch (Exception e) {
                print e.getMessage()
            }
        }

        return configName
    }

    def createConfigurationHttps(def configName, options = [:]) {
        def jenkinsURL = System.getenv("JENKINS_URL")
        assert jenkinsURL: 'Environment variable "JENKINS_URL" should be set.'
        URI jenkinsUri = new URI(jenkinsURL)

        def params = [
                url: "https://${jenkinsUri.getHost()}:8043"
        ]
        return createConfiguration(configName, params, options)
    }

    def createConfigurationWrongCredentials(def configName, options = [:]) {
        def params = [
                userName: 'noexistentuser',
                password: 'completelywrongpassword'
        ]
        return createConfiguration(configName, params, options)
    }

    def createConfigurationWrongUrl(def configName, options = [:]) {
        def commanderHostname = (System.getenv("COMMANDER_SERVER")) ?: 'localhost'
        def params = [
                url: "http://${commanderHostname}:8000"
        ]
        return createConfiguration(configName, params, options)
    }

    def getLogFromPipeline(def taskName, def flowRuntimeId) {
        return getLogFromPipeline(taskName, taskName, flowRuntimeId)
    }

    def getLogFromPipeline(def taskName, def stepName, def flowRuntimeId) {
        def propLine = "/myFlowRuntime/tasks/$taskName/job/jobSteps/$stepName/ec_debug_log"
        return getPipelineProperty(propLine, flowRuntimeId)
    }

    def getLogFromJob(def stepName, def jobId) {
        return getLogFromJob(stepName, stepName, jobId)
    }

    def getLogFromJob(def stepName1, def stepName2, def jobId) {
        def propLine = "/myJob/jobSteps/$stepName1/jobSteps/$stepName2/ec_debug_log"
        return getJobProperty(propLine, jobId)
    }

    def conditionallyDeleteProject(String projectName) {
        if (System.getenv("LEAVE_TEST_PROJECTS")) {
            return
        }
        dsl "deleteProject '$projectName'"
    }

    def getJobLink(def jobId) {
        def jobLink = System.getProperty("COMMANDER_SERVER")
        jobLink = "https://" + jobLink + "/commander/link/jobDetails/jobs/" + jobId.toString()
        return jobLink
    }

    def getCurrentProcedureName(def jobId) {
        assert jobId
        def currentProcedureName
        def property = "/myJob/procedureName"
        try {
            currentProcedureName = getJobProperty(property, jobId)
            logger.debug("Current Procedure Name: " + currentProcedureName)
        } catch (Throwable e) {
            logger.debug("Can't retrieve Run Procedure Name from the property: '$property'; check job: " + jobId)
        }
        return currentProcedureName
    }

    def getJobUpperStepSummary(def jobId) {
        assert jobId
        def summary
        def currentProcedureName = getCurrentProcedureName(jobId)
        def property = "/myJob/jobSteps/$currentProcedureName/summary"
        logger.debug("Trying to get the summary for Procedure: $currentProcedureName, property: $property, jobId: $jobId")
        try {
            summary = getJobProperty(property, jobId)
        } catch (Throwable e) {
            logger.debug("Can't retrieve Upper Step Summary from the property: '$property'; check job: " + jobId)
        }
        return summary
    }

    def getStepSummary(def jobId, def stepName) {
        assert jobId
        def summary
        def property = "/myJob/jobSteps/$stepName/summary"
        logger.debug("Trying to get the summary for Procedure: checkConnection, property: $property, jobId: $jobId")
        try {
            summary = getJobProperty(property, jobId)
        } catch (Throwable e) {
            logger.debug("Can't retrieve Upper Step Summary from the property: '$property'; check job: " + jobId)
        }
        return summary
    }

    Map<String, String> getOutputParameters(def jobId, def stepNumber) {
        assert jobId
        def outputParameters = []
        def stepId = getJobStepId(jobId, stepNumber)
        try {
            outputParameters = dsl """
            getOutputParameters(
                jobStepId: '$stepId'
                )
          """
        } catch (Throwable e) {
            logger.debug("Can't retrieve output parameters for job: " + jobId)
            e.printStackTrace()
        }

        def map = [:]
        for (i in outputParameters.outputParameter) {
            map[(String) i.outputParameterName] = i.value
        }
        return map
    }

    def getJobStepId(def jobId, def stepNumber) {
        assert jobId
        def stepId
        try {
            stepId = dsl """
            getJobDetails(
                jobId: '$jobId',
                structureOnly: '1'
                )
          """
        } catch (Throwable e) {
            logger.debug("Can't retrieve output parameters for job: " + jobId)
        }
        stepId = stepId.job.jobStep[stepNumber - 1].jobStepId

    }

    //Fix http://jira.electric-cloud.com/browse/FLOWPLUGIN-7920

    String createResource(String hostname = "127.0.0.1", port = '7800', workspaceName = "default") {
        def result = dsl """
            createResource(
                resourceName: '${randomize(pluginName)}',
                hostName: '$hostname',
                port: '$port',
                workspaceName: '$workspaceName'
            )
        """

        logger.debug(objectToJson(result))
        def resName = result?.resource?.resourceName
        assert resName
        resName
    }

}


