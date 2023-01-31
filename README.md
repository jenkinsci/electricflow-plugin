# CloudBees CD/RO Native Jenkins plugin
CloudBees CD/RO application release orchestration.

# Overview

CloudBees CD/RO is an enterprise-grade DevOps Release Automation platform
that simplifies provisioning, build and release of multi-tiered
applications. Our model-driven approach to managing environments and
applications allows teams to collaborate on and coordinate multiple
pipelines and releases across hybrid infrastructure in an efficient,
predictable, and auditable way.

# CloudBees CD/RO version dependencies

Starting with CloudBees CD/RO v2023.02.0, you must upgrade to CloudBees CD/RO Native Jenkins plugin v1.1.30. Failure to do so will result in failed plugin procedures. v1.1.30 is also backwards compatible with previous CloudBees CD/RO releases.     

# Features

With the CloudBees CD/RO plugin you can:
-   [Connect to your CloudBees CD/RO server](#connecting-to-your-cloudbees-cd-server)
-   [Call CloudBees CD/RO REST APIs](#calling-cloudbees-cd-rest-api)
-   [Create applications from deployment packages](#creating-and-deploying-applications-from-deployment-packages-to-cloudbees-cd)
-   [Deploy applications](#deploying-applications-using-cloudbees-cd)
-   [Publish artifacts](#publishing-artifacts-to-cloudbees-cd)
-   [Run pipelines](#running-pipelines-in-cloudbees-cd)
-   [Run procedures](#running-procedures-in-cloudbees-cd)
-   [Trigger releases](#triggering-releases-in-cloudbees-cd)

# Connecting to your CloudBees CD/RO server

To use and integrate with CloudBees CD/RO, you must
create a connection configuration in Jenkins to store your CloudBees CD/RO Server information. Depending on the number of servers or environments you are integrating with, you can create one or more connection configurations that allow you to connect to them and call CloudBees CD/RO APIs.

To create and configure your connection:
1. Login into your Jenkins instance and navigate to **Manage Jenkins** > **Configure System**.
2. Under **Configurations**, find the **CloudBees CD** section and select **Add**.
3. Specify the following information for your configuration:
    - **Configuration Name:** Name you want to give this configuration

    - **Server URL**: CloudBees CD/RO server URL

    - **REST API Version**: CloudBees CD/RO Server REST API version
4. Select **Save** in the Jenkins UI to confirm your configuration.
5. If you are redirected to another page, navigate back to Manage Jenkins > Configure System and under CloudBees CD/RO > Configurations, and find the configuration you just created.
6. Add your **Credentials Type**:
    -   **Username and Password**: CloudBees CD/RO username and password for your connection

    - **Stored Credential**: Used to configure authentication via stored credentials, which can be a username/password or secret text that supports an SSO session ID token
   > **_TIP:_**  To configure a connection using an SSO session ID token, refer to [Configuring an SSO session ID token for CloudBees CD](#configuring-an-sso-session-id-token-for-cloudbees-cd).

7. Both **Do not send build details to CloudBees CD** and **Override CloudBees CD/RO SSL Validation Check** are **optional** settings. You can select their **?** icons and read the descriptions to decide if you want to use these features.
8. Select **Test Connection** to ensure your credential is working correctly. If you receive a ``Success`` message, your configuration is ready to use. If you receive an error code, ensure your **Server URL** is correct. If it is, typically there was an error in the credential configuration and the configuration should be reconfigured.

## Configuring an SSO session ID token for CloudBees CD/RO
The CloudBees CD/RO plugin allows you to authenticate actions using an SSO session ID token. Before starting:
- You must have a generated CloudBees SSO session ID token. If you have not generated a CloudBees SSO session ID token, refer to [Generate an SSO session ID token](https://docs.cloudbees.com/docs/cloudbees-cd/latest/intro/sign-in-cd#_generate_a_sso_session_id_token) for help.
- You must have already set up a **CloudBees CD** configuration and saved it to Jenkins. If not, follow the steps in [Connecting to your CloudBees CD/RO server](#connecting-to-your-cloudbees-cd-server) until **Credentials Type**.

The following steps describe how to configure a CloudBees SSO session ID token within a CloudBees CD/RO configuration.

1. Navigate to **Manage Jenkins** > **Configure System**. Under **CloudBees CD** > **Configurations**, find the configuration to which you want to add an SSO session ID token.
2. Under **Credentials Type**, select **Stored Credential**. A new entry field opens.
3. For **Stored Credential**, select **Add** and **Jenkins**. A new window opens for the **Jenkins Credential Provider**.
4. By default, the **Domain** field is set to *Global credentials (unrestricted)* and is unchangeable.
5. For **Kind**, select **Secret text**.
6. Select the **Scope** you want to use your token for.
7. In the **Secret** field, enter your CloudBees SSO session ID token.
8. The **ID** field is **optional**. If you want to provide an **ID**, select the **?** icon and read the message to ensure you understand the purpose and requirements of this field.
9. The **Description** field is **optional**. However, it is suggested to provide one to help you keep track of the token configuration in your **Credentials**  profile.

> **_NOTE:_** Ensure the information for your credential is correct before moving to the next step. If you add the credential with incorrect information, you cannot edit it and will have to delete the incorrect credential and reconfigure a new one.

10. Select **Add** to save the configuration. You are returned to the system configurations page.
> **_TIP:_**  You can check your profile to ensure your credential was added and manage it.
11. Select **Test Connection** to ensure your credential is working correctly. If you receive a ``Success`` message, your configuration is ready to use. If you receive an error code, ensure your **Server URL** is correct. If it is, typically there was an error in the credential configuration, and the configuration should be reconfigured.

# Supported Post Build Actions

The CloudBees CD/RO plugin enables you to perform post-build actions for your Jenkins jobs. These actions can be executed separately or combined sequentially.

> **_NOTE:_** To manage **Post-build Actions**, navigate to your Jenkins job's **Configuration** > **Post-build Actions**.

## Calling CloudBees CD/RO REST API

The CloudBees CD/RO plugin allows you to make calls to CloudBees CD's REST API. You can make for post-build actions and as pipeline steps.

To configure a calls to CloudBees CD's REST API:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD/RO - Call REST API`.
4. Complete the fields with your configuration details. Select the **?** icon to view field information in a new dialog.
5. To apply the configuration to your job, select **Save** in the Jenkins UI.

### Pipeline script example for REST API calls in a Scripted Pipeline step

``` syntaxhighlighter-pre
node{
    stage('Test') {
        def result = cloudBeesFlowCallRestApi body: '', configuration: 'CdConfiguration', envVarNameForResult: 'CALL_REST_API_CREATE_PROJECT_RESULT', httpMethod: 'POST', parameters: [[key: 'projectName', value: 'CD-TEST-Jenkins-1.00.00.01'], [key: 'description', value: 'Native Jenkins Test Project']], urlPath: '/projects'
        echo "result : $result"
        echo "CALL_REST_API_CREATE_PROJECT_RESULT environment variable: $CALL_REST_API_CREATE_PROJECT_RESULT"
    }
}
```

### Pipeline script example for REST API calls in a Declarative Pipeline step

``` syntaxhighlighter-pre
pipeline{
    agent none
    stages {
        stage('Example Build') {
            steps {
                cloudBeesFlowCallRestApi body: '', configuration: 'CdConfiguration', envVarNameForResult: 'CALL_REST_API_CREATE_PROJECT_RESULT', httpMethod: 'POST', parameters: [[key: 'projectName', value: 'EC-TEST-Jenkins-1.00.00.01'], [key: 'description', value: 'Native Jenkins Test Project']], urlPath: '/projects'
            }
        }
        stage('Example Build 2') {
            steps {
                echo "CALL_REST_API_CREATE_PROJECT_RESULT environment variable: $CALL_REST_API_CREATE_PROJECT_RESULT"
            }
        }
    }
}
```

## Creating and deploying applications from Deployment Packages to CloudBees CD/RO

Using deployment packages generated from your Jenkins CI builds, the CloudBees CD/RO plugin allows you to create and deploy Java, .NET, or any other
application to any environment in CloudBees CD.

> **_IMPORTANT:_** To create and deploy applications from a deployment package, the deployment package must contain a JSON manifest file and the artifacts to deploy.
>
> Sample manifest.json files can be found in the [CloudBees CD/RO plug repo](https://github.com/electric-cloud/DeploymentPackageManager/tree/master/SampleManifests).

To set up this post-build action:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD/RO - Create/Deploy Application from Deployment Package`.
4. Complete the fields with your configuration details. Select the **?** icon to view field information in a new dialog.
5. To apply the configuration to your job, select **Save** in the Jenkins UI.

### Pipeline script example to create and deploy an application from a deployment package

``` syntaxhighlighter-pre
node {
    cloudBeesFlowCreateAndDeployAppFromJenkinsPackage configuration: 'CdConfiguration', filePath: 'CdProject/target/'
}
```

## Deploying applications using CloudBees CD/RO

The CloudBees CD/RO plugin enables you to deploy applications.

To set up this post-build action:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD/RO - Deploy Application`.
4. Complete the fields with your configuration details. Select the **?** icon to view field information in a new dialog.
5. To apply the configuration to your job, select **Save** in the Jenkins UI.

### Pipeline script example to deploy an application

``` syntaxhighlighter-pre
node{
   cloudBeesFlowDeployApplication applicationName: 'DemoApplication', applicationProcessName: 'RunCommand', configuration: 'CdConfiguration', deployParameters: '{"runProcess":{"applicationName":"DemoApplication","applicationProcessName":"RunCommand","parameter":[{"actualParameterName":"Parameter1","value":"value1"},{"actualParameterName":"Parameter2","value":"value2"}]}}', environmentName: 'CdEnvironment', projectName: 'CloudBees'
}
```
## Publishing artifacts to CloudBees CD/RO

The CloudBees CD/RO plugin allows you to publish artifacts for your applications generated as part of your Jenkins CI jobs directly to CloudBees CD.

To set up this post-build action:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD/RO - Publish Artifact`.
4. Complete the fields with your configuration details. Select the **?** icon to view field information in a new dialog.
5. To apply the configuration to your job, select **Save** in the Jenkins UI.


### Pipeline script example tp publish artifacts

``` syntaxhighlighter-pre
node {
    cloudBeesFlowPublishArtifact artifactName: 'application:jpetstore', artifactVersion: '1.0', configuration: 'CdConfiguration', filePath: 'CdProject/target/jpetstore.war', repositoryName: 'default'
}
```
## Running pipelines in CloudBees CD/RO

The CloudBees plugin allows you to run pipelines in CloudBees CD.

To set up this post-build action:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD/RO - Run Pipeline`.
4. Complete the fields with your configuration details. Select the **?** icon to view field information in a new dialog.
5. To apply the configuration to your job, select **Save** in the Jenkins UI.

### Pipeline script example to run a pipeline

``` syntaxhighlighter-pre
node{
    cloudBeesFlowRunPipeline addParam: '{"pipeline":{"pipelineName":"CdPipeline","parameters":[{"parameterName":"PipelineParam","parameterValue":"185"}]}}', configuration: 'CdConfiguration', pipelineName: 'CdPipeline', projectName: 'CloudBees'
}
```

## Running procedures in CloudBees CD/RO

The CloudBees CD/RO plugin allows you to run procedures in CloudBees CD/RO.

To set up this post-build action:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD/RO - Run Procedure`.
4. Complete the fields with your configuration details. Select the **?** icon to view field information in a new dialog.
5. To apply the configuration to your job, select **Save** in the Jenkins UI.

### Pipeline script examples to run a procedure

``` syntaxhighlighter-pre
node{
    cloudBeesFlowRunProcedure configuration: 'CdConfiguration', procedureName: 'TomcatCheckServer', procedureParameters: '{"procedure":{"procedureName":"TomcatCheckServer","parameters":[{"actualParameterName":"max_time","value":"10"},{"actualParameterName":"tomcat_config_name","value":"Tomcat configuration"}]}}', projectName: 'CloudBees'
}
```

``` syntaxhighlighter-pre
node{
    cloudBeesFlowRunProcedure configuration: 'CdConfiguration', overrideCredential: [credentialId: 'CREDS_PARAM'], procedureName: 'TomcatCheckServer', procedureParameters: '{"procedure":{"procedureName":"TomcatCheckServer","parameters":[{"actualParameterName":"max_time","value":"10"},{"actualParameterName":"tomcat_config_name","value":"Tomcat configuration"}]}}', projectName: 'CloudBees', runAndWaitOption: [checkInterval: 5, dependOnCdJobOutcome: true]
}
```

### Pipeline script example for running a Procedure call with failed result handling

> **_IMPORTANT:_** This script relies on the Pipeline Stage API improvements and requires Jenkins 2.138.4 or newer.
>
> Other required plugin versions are noted here: [https://www.jenkins.io/blog/2019/07/05/jenkins-pipeline-stage-result-visualization-improvements/](https://www.jenkins.io/blog/2019/07/05/jenkins-pipeline-stage-result-visualization-improvements/).

```groovy
//...
script {
    try {
        // Note that both 'dependOnCdJobOutcome' and 'throwExceptionIfFailed' properties of the RunAndWait option should be set to true.
        cloudBeesFlowRunProcedure configuration: 'electricflow', procedureName: 'prepareDeployment', procedureParameters: '{"procedure":{"procedureName":"prepareDeployment","parameters":[]}}', projectName: 'CloudBees', runAndWaitOption: [dependOnCdJobOutcome: true, throwExceptionIfFailed: true]
    } catch (RuntimeException ex) {
        if (ex.getMessage() =~ 'outcome=warning') {
            unstable("The 'prepareDeployment' CD job was finished with warning. This needs further investigation.")
            currentBuild.result = 'UNSTABLE'
        } else if (ex.getMessage() =~ 'outcome=error') {
            currentBuild.result = 'FAILURE'
            unstable("The 'prepareDeployment' CD job was finished with error and the CI build should be stopped.")
            throw ex
        }
    }
}
//...
```
## Triggering releases in CloudBees CD/RO

This Integration allows you to trigger a release in CloudBees CD.

To set up this post-build action:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD/RO - Trigger Release`.
4. Complete the fields with your configuration details. Select the **?** icon to view field information in a new dialog.
5. To apply the configuration to your job, select **Save** in the Jenkins UI.

### Pipeline script example for triggering a release

``` syntaxhighlighter-pre
node{
    cloudBeesFlowTriggerRelease configuration: 'CdConfiguration', parameters: '{"release":{"releaseName":"CdRelease1.1.5","stages":[{"stageName":"Stage 1","stageValue":false},{"stageName":"Stage 2","stageValue":true}],"pipelineName":"pipeline_CdRelease1.1.5","parameters":[{"parameterName":"ReleaseParam","parameterValue":"test"}]}}', projectName: 'CloudBees', releaseName: 'CdRelease1.1.5', startingStage: ''
}
```


Details for this build are be attached to the Release Run (if supported by CloudBees CD/RO server).

# Known issues
## Adding credentials to a new CloudBees CD/RO configuration  
[BEE-27725] When creating a new CloudBees CD/RO configuration, you cannot add a new credential using the "Add" button.
### Workarounds
You can use the following workarounds instead of the **Add** button to help you add a credential:
- The "Add" button works for existing configurations. Create and save your configuration without the credential. You can then return to the configuration and add the credential using the **Add** button.
- Add your credentials as described in Jenkins' [Configuring credentials](https://www.jenkins.io/doc/book/using/using-credentials/#configuring-credentials).

# Release Notes


## Version 1.1.30 (January 27, 2023)

- Removed CGI scripts

## Version 1.1.29 (November 25, 2022)

- Fixed plugin dependencies

## Version 1.1.28 (November 15, 2022)

- Updated plugin's global configuration by possibility to use stored credentials. Username and password or secret text (token) can be used for connecting to CloudBees CD/RO

- Added support of CloudBees CD/RO tokens which now can be configured as stored credential (secret text) for main or override configurations;
- Improved handling of override credentials
- Fixed handling of stages in Trigger Release

## Version 1.1.25 (January 14, 2022)

- Added support of folder credentials which now can be used within override credentials functionality
- Fixed Upload Artifact when using agent
- Improved integration of CloudBees CI and CD/RO

## Version 1.1.21 (March 12, 2021)

- Updated dependencies

## Version 1.1.20 (March 10, 2021)

- Updated dependencies

## Version 1.1.19 (February 11, 2021)

- Updated Run And Wait option to allow interruption of the build when Flow runtime was not finished successfully. This enhancement is applied to the following methods:
    - Trigger Release
    - Run Pipeline
    - Run Procedure
    - Publish Application
    - Create and Deploy Application from Deployment Package
- Updated Deploy application by possibility to specify project for environment if it is other than project for application

- Fixed the following procedures for new Jenkins (2.264+) according to Jenkins forms tabular to div changes: Run Procedure, Run Pipeline, Deploy Application, Trigger Release

## Version 1.1.18 (September 14, 2020)

- Updated Publish Artifact by Relative Workspace parameter
- Updated Run And Wait checkInterval by min value
- Updated "Depend on CD job/pipeline outcome" functionality by association of CloudBees CD/RO job/pipeline outcome "Warning" with CloudBees CI build result "Unstable"
- Updated build summary links for Run Pipeline, Publish Artifact, Trigger Release
- Fixed snippet generator UI for pipeline steps with extra parameters (Run Procedure, Trigger Release, Run Pipeline, Deploy Application)
- Event-based build watchers have been improved and now they are also sending build information after the build is finished.
- CloudBees CD/RO Multibranch Pipelines support has been improved.
- Bug fixes and improvements

## Version 1.1.17 (July 17, 2020)

Improvements:

- Updated the following post build actions by Run and Wait option with possibility to depend on CD job or pipeline outcome:
    - Run Pipeline
    - Trigger Release
    - Run Procedure
    - Deploy Application
    - Create and Deploy Application from Deployment Package

## Version 1.1.16 (June 26, 2020)

Improvements:

- Updated Run Procedure by “RunAndWait” option which includes extra dependOnCdJobOutcome and checkInterval sub-options
- Improved error messages in Trigger Pipeline PBA
- Improved error handling in Run Procedure PBA
- Updated build summary of Deploy Application by an extra link to CD application

Bugfixes:

- Fixed an issue when Publish Artifact has been creating a wrong repository structure if the artifact has been published from the Jenkins agent node
- Fixed an error when CD Artifacts URL had a double slash under some conditions
- Deploy Application PBA now has a link to the exact application that has been deployed instead of a link to all applications on customer instance
- Fixed an issue in EC-Jenkins when artifacts URL could not be retrieved for the Report stage of Run And Monitor and Run And Wait under certain conditions
- Fixed release retrieval code
- Fixed an error when "Publish Artifacts" was turning a successful build into failed build under some conditions
- Fixed a bug when parameter "Stages to run" has been ignored and all stages were running

Documentation, help tips and labels:

- Updated plugin documentation by new screenshots and pipeline code snaps
- Parameters label for Trigger release has been changed to be more concrete
- Updated help tips for "Publish Artifacts" and "Create/Deploy Application from Deployment Package" PBAs has been improved and now has the list of supported cases

## Version 1.1.15 (June 1, 2020)

- Added support for new CI Build Detail APIs:
    - New Post Build Action - Associate Build To Release to allow attaching CI Build Data of the independent builds to releases or release runs
    - Run Pipeline now attaches CI Build Data to the triggered pipeline run
    - Trigger Release now attaches CI Build Data to the triggered release pipeline run

- Improved integration with CloudBees CD:
    - CI Build Data infrastructure has been created
    - Event-based watchers have been created to send build data to CloudBees CD/RO automatically if build has been triggered by CloudBees CD.

- Re-branding: renaming from "CloudBees Flow" to "CloudBees CD"

## Version 1.1.14 (May 6, 2020)

- Added support of Configuration as Code
- Updated plugin dependencies

## Version 1.1.13 (Apr 21, 2020)

- All Post Build Actions now have the ability to connect to CloudBees Flow as a user other than the one mentioned in the electricflow Plugin configuration, under "Manage Jenkins".  These credentials which are used to override the connection credential at the level of the PBA only supports global credentials at this time.
- Fixed parameters collision for pipelines with the same name in the following Post Build Actions: Trigger Release, Run Pipeline
- Fixed corrupted artifact uploading in the following Post Build Actions: Publish Artifact
- Updated plugin dependencies

## Version 1.1.12 (Dec 17, 2019)

Migrated plugin documentation from Wiki to GitHub

## Version 1.1.11 (Dec 11, 2019)

Updated "CloudBees Flow - Publish Artifact"
- Added pipeline compatibility
- Fixed support of running on agents

Updated "CloudBees Flow - Create and Deploy Application from Deployment Package"

- Added pipeline compatibility
- Fixed support of running on agents
- Added link to CloudBees Flow job within summary of a build

Added pipeline function aliases for all post build actions.

Added expand environment variable functionality for the following post build actions:
- CloudBees Flow - Call REST API
- CloudBees Flow - Run Procedure
- CloudBees Flow - Deploy Application

Added a sample Jenkinsfile to the plugin repository

## Version 1.1.10 (Sep 26, 2019)

Updated "CloudBees Flow - Call REST API" related functionality:

-   Added support of the new workflow step "CloudBees Flow - Call REST
    API" which is based on the same functionality as corresponded post
    build action. Snippet generator UI is available for the new workflow
    step
-   Result of calling CloudBees Flow REST API (JSON output) now can be
    stored within environment variable available within build and also
    can be returned by the new workflow step within scripted pipelines
-   Fixed URL on summary page of Call REST API jobs

Changed Jenkins baseline version for the plugin to 2.138.4

## Version 1.1.9 (Jun 12, 2019)

-   Added basic unit tests

## Version 1.1.8 (Jun 12, 2019)

-   Re-branding: renaming from "Electric Flow" to "CloudBees Flow"

## Version 1.1.7 (Jun 11, 2019)

-   [Fix security
    issue](https://jenkins.io/security/advisory/2019-06-11/)
    (SECURITY-1420)

## Version 1.1.6 (Jun 11, 2019)

-   [Fix security
    issues](https://jenkins.io/security/advisory/2019-06-11/)
    (SECURITY-1410, SECURITY-1411, SECURITY-1412)

## Version 1.1.5 (Dec 19, 2018)

Support for the following New Post Build Actions have been added:

-   ElectricFlow - Deploy Application
-   ElectricFlow - Trigger Release
-   ElectricFlow - Call REST API
-   ElectricFlow - Run Procedure

Post Build Action "ElectricFlow - Run Pipeline" modified as follows:

-   It can now be run for pipelines without parameters

Post Build Action "ElectricFlow - Publish Artifact" modified as follows:

-   Added support for publishing to both directories and sub-directories
-   Explicit error messages added for build runs

Usability Changes

-   Post Build Action page shows dynamically retrieved values all the
    time
-   Two new buttons "Validate Before Apply" and "Compare Before Apply"
    added in Post Build Action Pages for Deploy Application, Trigger
    Release, Run Procedure and Run Pipeline, to make sure that failure
    to retrieve information is handled gracefully (no stack traces) and
    at the same time, users can understand the field errors before
    saving the configuration.
-   More descriptive Help tips.
-   More verbose messages when Test Connection fails with Electric Flow.
-   More verbose logging on response body for failed Rest API calls.
-   New option called "Override Electric Flow SSL Validation Check"
    introduced for testing connection with Electric Flow, where there is
    a need to test Electric Flow Post Build Actions before doing the SSL
    setup.

## Version 1.1.4 (Nov 22, 2017)

Post Build Action "ElectricFlow - Publish Artifact" modified as follows:

-   Fixed Scenarios where Build Step fails with exceptions
-   Added Support for remoting (build on remote windows machines)

## Version 1.1.3 (May 9, 2017)

Support added for running plugin tasks from jenkins pipeline as per
<https://issues.jenkins-ci.org/browse/JENKINS-44084>.

## Version 1.1.2 (Apr 28, 2017)

More detailed output from plugin on build page results.  
Hierarchy files output added to build page.

## Version 1.1.1 (Apr 27, 2017)

General clean up of code.

## Version 1.0 (Apr 26, 2017)

Initial Release.
