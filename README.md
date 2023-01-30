# CloudBees CD/RO Native Jenkins plugin
CloudBees CD/RO application release orchestration.

# Overview

CloudBees CD/RO is an enterprise-grade DevOps Release Automation platform
that simplifies provisioning, build and release of multi-tiered
applications. Our model-driven approach to managing environments and
applications allows teams to collaborate on and coordinate multiple
pipelines and releases across hybrid infrastructure in an efficient,
predictable and auditable way. 

# CloudBees CD/RO version dependencies

Starting with CloudBees CD/RO v2023.01.0, you must upgrade to CloudBees CD/RO Native Jenkins plugin v1.1.30. Failure to do so will result in failed plugin procedures. v1.1.30 is also backwards compatible with previous CloudBees CD/RO releases.     


# Features

With the CloudBees CD/RO plugin you can:

-   Trigger a release in CloudBees CD/RO
-   Trigger a pipeline in CloudBees CD/RO
-   Deploy an application in CloudBees CD/RO
-   Publish an artifact from Jenkins into the CloudBees CD/RO artifact
    repository
-   Run a Procedure in CloudBees CD/RO
-   Call a REST API to invoke any action in CloudBees CD/RO
-   Create Application in CloudBees CD/RO from Deployment Package

# Connection Configurations

In order to use and integrate with CloudBees CD/RO, you would have to
create a connection configuration in Jenkins, that stores connection
information of the CloudBees CD/RO server you are connecting to. You can
create one or more connection configurations depending on the number of
Servers or Environments you are integrating with.

Navigate to Manage Jenkins / Configure System and go to CloudBees CD/RO
section. One or more configurations can be created to connect to and
call APIs into CloudBees CD/RO system. For each configuration, the
following attributes need to be specified:

-   Configuration Name: Specify the name to store this configuration,
    which is used to connect to the CloudBees CD/RO Server.

-   Server URL: CloudBees CD/RO Server URL

-   REST API Version: CloudBees CD/RO Server Rest API Version
-   Credentials Type:
    -   Username and Password: CloudBees CD/RO username and passport for connection
    -   Stored Credential: Used to configure authentication via stored credentials, which can be username/password or secret text, which supports an SSO session ID token
-   Do not send build details to CloudBees CD/RO:
    By default, if the build has been triggered by CloudBees CD/RO using CI configuration build details will be sent.
    Use this setting if you do not want to be sending build details to this CloudBees CD/RO instance.

-   Override CloudBees CD/RO SSL Validation Check: By default SSL
    Validation Check will be performed. Choose this setting to override
    the check. If you do not want to override this check, perform the
    SSL certificate setup required in Jenkins and CloudBees CD/RO as per
    the CloudBees CD/RO Server documentation.

![](docs/images/Configuration.png)

## Configuring an SSO session ID token
The CloudBees CD/RO Native Jenkins plugin allows you to authenticate actions using an SSO session ID token. 
> **_TIP:_**  If you have not generated a CloudBees SSO session ID token, refer to [Generate an SSO session ID token](https://docs.cloudbees.com/docs/cloudbees-cd/latest/intro/sign-in-cd#_generate_a_sso_session_id_token) for help. 

The following steps will help you configure a CloudBees SSO session ID token. To do so, you must have a CloudBees SSO session ID token generated. 

1. Login to your Jenkins instance, and navigate to **Manage Jenkins** > **Configure System**.
2. Under **CloudBees CD/RO** > **Configurations**, select **Add**.
3. In the new configuration, enter a **Configuration Name** for your SSO session ID token, your **Server URL**, and select the **REST API Version**. 
4. Select **Save** to confirm the new configuration.
5. If you are redirected to another page, navigate back to **Manage Jenkins** > **Configure System** and find your SSO session ID token's configuration. 
6. Under **Credentials Type**, select **Stored Credential** and a new entry field will appear.
7. For **Stored Credential**, select **Add** and **Jenkins**. A new window should appear for the **Jenkins Credential Provider**.
8. By default, the **Domain** field is set to *Global credentials (unrestricted)* and is unchangeable. 
9. For **Kind**, select **Secret text**. 
10. Select the **Scope** you want to use your token for. 
11. In the **Secret** field, enter your CloudBees SSO session ID token. 
12. The **ID** field is **optional**. If you want to provide and **ID**, select the **?** icon and read the message to ensure you understand the purpose and requirements of this field.
13. The **Description** field is **optional**. However, it is suggested to provide one  to help you keep track of the token configuration as it used as the **Credentials** > **Name** in your profile.

> **_NOTE:_**  Ensure the information for your credential is correct before moving to the next step. If you add the credential with incorrect information, you cannot edit it and will have to delete the incorrect credential and reconfigure a new one. 

14. Select **Add** to save the configuration. You will be returned to the system configurations page.
    > **_TIP:_**  You can check in your profile to ensure your credential was added and manage it. 
15. Select **Test Connection** to ensure your credential is working correctly. If you receive a ``Success`` message, your SSO session ID token is ready to use. If you receive an error code, ensure your **Server URL** is correct. If it is, typically there was an error in the credential configuration and the configuration should be reconfigured with a new credential.  


# Supported Post Build Actions

Following post build actions are available in CloudBees CD/RO
Plugin. These actions can be executed separately or combined
sequentially.

## Create Application from Deployment Package to CloudBees CD/RO

This integration allows you to create and deploy Java, .NET or any other
application to any environment in CloudBees CD/RO. Deployment package
would be generated as part of your Jenkins CI build, and contain a
Manifest file and artifacts to be deployed. 

Sample manifest.json file can be found
at [https://github.com/electric-cloud/DeploymentPackageManager/tree/master/SampleManifests](https://github.com/electric-cloud/DeploymentPackageManager/tree/master/SampleManifests). 

This post build action has following parameters:

-   Configuration: Name of the CloudBees CD/RO configuration

-   Override Credential: Connect to CloudBees CD/RO as a User other than the one mentioned in the electricflow Plugin Connection Configuration

-   Deployment Package Path: Location or path for the deployment package
    to be published to CloudBees CD/RO. For e.g., MyProject/target

![](docs/images/CreateDeplApp.png)

**Create and Deploy Application from Deployment Package (Pipeline Script)**

``` syntaxhighlighter-pre
node {
    cloudBeesFlowCreateAndDeployAppFromJenkinsPackage configuration: 'CdConfiguration', filePath: 'CdProject/target/'
}
```

## Publish Artifact to CloudBees CD/RO

This integration allows you to publish the artifact for your application
to CloudBees CD/RO The Artifact will be generated as part of your
Jenkins CI build. 

This post build action takes the following parameters:

-   Configuration: Name of the CloudBees CD/RO configuration

-   Override Credential: Connect to CloudBees CD/RO as a User other than the one mentioned in the electricflow Plugin Connection Configuration

-   Relative Workspace: Specify the relative workspace (relative to workspace root) for artifact path.

-   Artifact Path: Location or path for the artifact files to be
    published to CloudBees CD/RO. For
    e.g., MyProject/\*\*/\*-$BUILD\_NUMBER.war

-   Artifact Name: Name of the application artifact using the format
    \<group\_id\>:\<artifact\_key\>. For e.g., "com.example:helloworld"

-   Artifact Version: Version of the application artifact. For e.g., you
    can specify 1.0 or 1.0-$BUILD\_TAG that is based on Jenkins
    environment variable

-   CloudBees CD/RO Repository Name: Name of the CloudBees CD/RO
    Repository

![](docs/images/PublishArtifact.png)

**Publish Artifact (Pipeline Script)**

``` syntaxhighlighter-pre
node {
    cloudBeesFlowPublishArtifact artifactName: 'application:jpetstore', artifactVersion: '1.0', configuration: 'CdConfiguration', filePath: 'CdProject/target/jpetstore.war', repositoryName: 'default'
}
```

## Run Pipeline in CloudBees CD/RO

This integration allows you to run a pipeline in CloudBees CD/RO.

This post build action takes the following parameters:

- Configuration: Name of the CloudBees CD/RO configuration

- Override Credential: Connect to CloudBees CD/RO as a User other than the one mentioned in the electricflow Plugin Connection Configuration

- Project Name: Name of the CloudBees CD/RO project

- Pipeline Name: Name of the CloudBees CD/RO pipeline

- (Optional) Pipeline Parameters

  -   Parameter name will be displayed as Label

  -   Parameter value to be specified will be displayed as text input
    field

![](docs/images/RunPipeline.png)

**Run Pipeline Example (Pipeline Script)**

``` syntaxhighlighter-pre
node{
    cloudBeesFlowRunPipeline addParam: '{"pipeline":{"pipelineName":"CdPipeline","parameters":[{"parameterName":"PipelineParam","parameterValue":"185"}]}}', configuration: 'CdConfiguration', pipelineName: 'CdPipeline', projectName: 'CloudBees'
}
```

## Call REST API of CloudBees CD/RO

This integration allows you to call the CloudBees CD/RO REST API.
Available as Post Build Action and Pipeline Step as well.

This post build action takes the following parameters:

-   Configuration: Specify the name of the CloudBees CD/RO configuration.

-   Override Credential: Connect to CloudBees CD/RO as a User other than the one mentioned in the electricflow Plugin Connection Configuration

-   URL Path: Specify the URL Path for the REST API

-   HTTP Method: Specify the HTTP Method

-   Parameters: Specify the parameters for the REST API. Parameters are
    transformed into JSON object and used within body of HTTP request.

-   Body: Specify the body for the REST API. This parameter is not used
    if 'Parameters' are provided.

-   Environment variable name for storing result: If provided, result of
    calling CloudBees REST API (JSON output) will be stored within
    provided environment variable available within build.

![](docs/images/image2019-9-27_16-51-9.png)

![](docs/images/image2019-9-27_16-58-46.png)

**Call REST API Example Pipeline Step \#1 (Scripted Pipeline)**

``` syntaxhighlighter-pre
node{
    stage('Test') {
        def result = cloudBeesFlowCallRestApi body: '', configuration: 'CdConfiguration', envVarNameForResult: 'CALL_REST_API_CREATE_PROJECT_RESULT', httpMethod: 'POST', parameters: [[key: 'projectName', value: 'CD-TEST-Jenkins-1.00.00.01'], [key: 'description', value: 'Native Jenkins Test Project']], urlPath: '/projects'
        echo "result : $result"
        echo "CALL_REST_API_CREATE_PROJECT_RESULT environment variable: $CALL_REST_API_CREATE_PROJECT_RESULT"
    }
}
```

**Call REST API Example Pipeline Step \#2 (Declarative Pipeline)**

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

## Deploy Application using CloudBees CD/RO

This integration allows you to deploy an application using CloudBees CD/RO.

This post build action takes the following parameters:

- Configuration: Specify the name of the CloudBees CD/RO configuration

- Override Credential: Connect to CloudBees CD/RO as a User other than the one mentioned in the electricflow Plugin Connection Configuration

- Project Name: Specify the CloudBees CD/RO project name

- Application Name: Specify the CloudBees CD/RO application name

- Application Process Name: Specify the CloudBees CD/RO application process
name

- (Optional) Environment Project Name: Specify the CloudBees CD/RO environment project name if it is different than project for application

- Environment Name: Specify the CloudBees CD/RO environment name

- (Optional) Deploy Parameters

  -   Parameter name will be displayed as Label

  -   Parameter value to be specified will be displayed as text input
    field

![](docs/images/DeployApplication.png)

**Deploy Application Example (Pipeline Script)**

``` syntaxhighlighter-pre
node{
   cloudBeesFlowDeployApplication applicationName: 'DemoApplication', applicationProcessName: 'RunCommand', configuration: 'CdConfiguration', deployParameters: '{"runProcess":{"applicationName":"DemoApplication","applicationProcessName":"RunCommand","parameter":[{"actualParameterName":"Parameter1","value":"value1"},{"actualParameterName":"Parameter2","value":"value2"}]}}', environmentName: 'CdEnvironment', projectName: 'CloudBees'
}
```

## Trigger Release in CloudBees CD/RO

This Integration allows you to trigger a release in CloudBees CD/RO.

This post build action has following parameters:

- Configuration: Specify the name of the CloudBees CD/RO configuration

- Override Credential: Connect to CloudBees CD/RO as a User other than the one mentioned in the electricflow Plugin Connection Configuration

- Project Name: Specify the CloudBees CD/RO project name

- Release Name: Specify the CloudBees CD/RO release name

- Starting Stage: Specify starting stage to run in the CloudBees CD/RO release pipeline

  -   Parameter is required if ‘Stages to run’ is not used.

- Stages to run: Specify stages to run in the CloudBees CD/RO release pipeline

  -   Parameter is required if ‘Starting Stage’ is not used.
  -   Parameter is ignored if ‘Starting Stage’ is used.

- (Optional) Pipeline parameters: Specify parameters for the CloudBees CD/RO pipeline

  -   Parameter name will be displayed as Label

  -   Parameter value to be specified should go in the text input field

![](docs/images/TriggerRelease.png)

**Trigger Release Example (Pipeline Script)**

``` syntaxhighlighter-pre
node{
    cloudBeesFlowTriggerRelease configuration: 'CdConfiguration', parameters: '{"release":{"releaseName":"CdRelease1.1.5","stages":[{"stageName":"Stage 1","stageValue":false},{"stageName":"Stage 2","stageValue":true}],"pipelineName":"pipeline_CdRelease1.1.5","parameters":[{"parameterName":"ReleaseParam","parameterValue":"test"}]}}', projectName: 'CloudBees', releaseName: 'CdRelease1.1.5', startingStage: ''
}
```

Details for this build will be attached to the Release Run (if supported by CloudBees CD/RO server).

## Run Procedure in CloudBees CD/RO

This Integration allows you run a procedure in CloudBees CD/RO.

This post build action has the following parameters:

- Configuration: Specify the name of the CloudBees CD/RO configuration

- Override Credential: Connect to CloudBees CD/RO as a User other than the one mentioned in the electricflow Plugin Connection Configuration

- Wait for CD Job Completed: Wait till launched CD job is completed

  - Depend on CD Job Outcome: Mark CI build as failed if CD Job outcome is error or unknown
  
  - Check Interval: Specify the CloudBees CD/RO procedure name

- Project Name: Specify the CloudBees CD/RO project name

- Procedure Name: Specify the CloudBees CD/RO procedure name

- (Optional) Procedure Parameters

  -   Parameter name will be displayed as Label

  -   Parameter value to be specified should go in the text input field

![](docs/images/RunProcedure.png)

**Run Procedure Example (Pipeline Script)**

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


**Example of the Run Procedure call with failed result handling (pipeline script)**

Note: This script relies on the Pipeline Stage API improvements and requires Jenkins 2.138.4 or newer. 
Other required plugin versions are noted here: [https://www.jenkins.io/blog/2019/07/05/jenkins-pipeline-stage-result-visualization-improvements/](https://www.jenkins.io/blog/2019/07/05/jenkins-pipeline-stage-result-visualization-improvements/)

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

# Known issues
## Adding credentials to a new CloudBees CD/RO configuration  
[BEE-27725] When creating a new CloudBees CD/RO configuration, you cannot add a new credential using the "Add" button.
### Workarounds
The following workarounds may be used instead of the "Add" button to help you add a credential:
- The "Add" button works for existing configurations. So, create your configuration without the credential and then edit the configuration.
- Add your credentials as described in Jenkins' [Configuring credentials](https://www.jenkins.io/doc/book/using/using-credentials/#configuring-credentials).


# Release Notes

## Version 1.1.30 (January 27, 2023)

- Removed CGI scripts

## Version 1.1.29 (November 25, 2022)

- Fixed plugin dependencies

## Version 1.1.28 (November 15, 2022)

- Updated plugin's global configuration by possibility to use stored credentials. Username and password or secret text (token) can be used for connecting to CloudBees CD/RO
- Added support of Cloudbees CD/RO tokens which now can be configured as stored credential (secret text) for main or override configurations;
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
- Updated Deploy application by possibility to specify project for environment if it is othe than project for application
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
- Fixed an issue in EC-Jenkins when artifacts could URL could not be retrieved for the Report stage of Run And Monitor and Run And Wait under certain conditions
- The release retrieval code has been fixed
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
    
  - Improved integration with CloudBees CD/RO:
    - CI Build Data infrastructure has been created
    - Event-based watchers have been created to send build data to CloudBees CD/RO automatically if build has been triggered by CloudBees CD/RO.
    
  - Re-branding: renaming from "CloudBees Flow" to "CloudBees CD/RO"

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
  - Added link to CloubBees Flow job within summary of a build
  
Added pipeline function aliases for all post build actions.

Added expand environment variable functionality for the following post build actions:
  - CloudBees Flow - Call REST API
  - CloudBees Flow - Run Procedure
  - CloudBees Flow - Deploy Application

Added simple Jenkinsfile to the plugin repository

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

