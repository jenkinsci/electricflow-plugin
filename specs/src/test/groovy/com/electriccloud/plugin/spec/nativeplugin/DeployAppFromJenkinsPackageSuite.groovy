package com.electriccloud.plugin.spec.nativeplugin

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.core.Job
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsBuildJob
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsJobRunner


class DeployAppFromJenkinsPackageSuite extends JenkinsHelper {

    public static final String testPbaName = "cloudBeesFlowCreateAndDeployAppFromJenkinsPackage"
    private static final String testProjectName = "Specs - electricflow-plugin - $testPbaName"

    private static final String PIPELINE_NAME = "nativeJenkinsPBAExtendedPipelineProject"
    public static final String CI_CONFIG_NAME = "electricflow"
    public static final String FLOW_PROJECT_NAME = "pvNativeJenkinsProject01"

    private static JenkinsJobRunner jjr = JenkinsJobRunner.getInstance()

    // Here we expect that the electricflow-plugin will:
    // 1. Upload an artifact that contains:
    //  - Application archive
    //  - manifest.json file
    // 2. Run the Electric-Cloud:CreateApplicationFromDeploymentPackage procedure that will:
    //  - read the manifest
    //  - publish Component artifacts
    //  - create Application from manifest
    //  - Create Tier Map
    //  - Create Snapshot
    //  - Run Application Process
    //  - Remove artifact

    // But for now we don't have:
    // a) Application server to deploy application to
    // b) manifest.json to use

    // So we only check that the procedure was started

    def doSetupSpec() {
        // Do project import here
    }

    def "C368022. Partial - Fire DeployApplicationFromJenkinsPackage"() {
        given: 'Parameters for the pipeline'
        // Looking for jobs before the launch
        ArrayList<Job> deploymentJobsBefore = Job.findJobs([
                [
                        propertyName: 'procedureName',
                        operator    : 'equals',
                        operand1    : 'CreateApplicationFromDeploymentPackage'
                ]
        ])

        def ciPipelineParameters = [
                flowConfigName : CI_CONFIG_NAME,
                flowProjectName: FLOW_PROJECT_NAME,
                runOnly        : testPbaName
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(PIPELINE_NAME, ciPipelineParameters)

        then: 'Collecting the result objects'
        assert ciJob.isSuccess(): "Pipeline on Jenkins is finished."

        expect: 'Checking that we have new job ran'
        ArrayList<Job> deploymentJobsAfter = Job.findJobs([
                [
                        propertyName: 'procedureName',
                        operator    : 'equals',
                        operand1    : 'CreateApplicationFromDeploymentPackage'
                ]
        ])
        assert deploymentJobsAfter.size() - deploymentJobsBefore.size() == 1

        // Missing manifest causes unable to check:
        // - Check Application created
        // - Check Component created?
        // - Check Application deployment process was run
        //
        // Missing deployment causes unable to check:
        // - Check that application was deployed

        assert true
    }

}

