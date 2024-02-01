# CloudBees CD/RO plugin

CloudBees Continuous Deployment and Release Orchestration (CD/RO), formerly CloudBees CD, is 
an enterprise-grade DevOps release automation platform that simplifies 
provisioning, building, and releasing multi-tiered applications. 
Our model-driven approach for managing environments and applications 
allows teams to collaborate and coordinate across multiple pipelines and 
releases within hybrid infrastructures in an efficient, predictable, 
and auditable way.

# Plugin overview

The CloudBees CD plugin allows you to integrate CloudBees CD/RO features and APIs directly in your Jenkins instances. From running pipelines that compile your applications to deploying them, the CloudBees CD plugin helps reduce the inherent complexity of continuous deployment and release orchestration. For more information on the capabilities of this plugin, refer to [Plugin features](#plugin-features). 

# CloudBees CD/RO version dependencies

Starting with CloudBees CD/RO v2023.02.0, you must upgrade to CloudBees CD plugin v1.1.30 or later. Failure to do so will result in failed plugin procedures. Plugin versions v1.1.30 or later are backwards compatible with previous CloudBees CD/RO releases.

# Plugin features

The CloudBees CD plugin integrates the following features into your Jenkins instances to interact with CloudBees CD/RO:
-   [Connect to your CloudBees CD server](#connecting-to-your-cloudbees-cd-server)
-   [Call CloudBees CD REST APIs](#calling-cloudbees-cd-rest-api)  
-   [Create applications from deployment packages](#creating-and-deploying-applications-from-deployment-packages-to-cloudbees-cd) 
-   [Deploy applications](#deploying-applications-using-cloudbees-cd)
-   [Publish artifacts](#publishing-artifacts-to-cloudbees-cd)
-   [Run pipelines](#running-pipelines-in-cloudbees-cd)
-   [Run procedures](#running-procedures-in-cloudbees-cd)
-   [Trigger releases](#triggering-releases-in-cloudbees-cd)

# Connecting to your CloudBees CD server

To use and integrate with CloudBees CD/RO, you must
create a connection configuration in Jenkins to store your CloudBees CD/RO server information. Depending on the number of servers or environments you are integrating, you can create one or multiple connection configurations to connect with and call CloudBees CD/RO APIs.

To create and configure your connection:
1. Login into your Jenkins instance and navigate to **Manage Jenkins** > **Configure System**.
2. Under **Configurations**, find the **CloudBees CD** section and select **Add**. 
3. Specify the following information for your configuration:
   - **Configuration Name:** Name for this configuration.

   - **Server URL**: Your CloudBees CD/RO server URL. 

   - **REST API Version**: CloudBees CD/RO server REST API version.
4. Select **Save** in the Jenkins UI to confirm your configuration.
5. If you are redirected to another page, navigate back to **Manage Jenkins > Configure System**, and under **CloudBees CD > Configurations**, find the configuration you just created.
6. Add your **Credentials Type**:
    -   **Username and Password**: CloudBees CD/RO username and password for your connection.

    - **Stored Credential**: Used to configure authentication via stored credentials, which can be a username/password or secret text that supports an SSO session ID token.
    > **_TIP:_**  To configure a connection using an SSO session ID token, refer to [Configuring an SSO session ID token for CloudBees CD](#configuring-an-sso-session-id-token-for-cloudbees-cd).

7. Both **Do not send build details to CloudBees CD** and **Override CloudBees CD SSL Validation Check** are **optional** settings. You can select their **?** icons and read the descriptions to decide if you want to use these features.   
8. Select **Test Connection** to ensure your credential is working correctly. If you receive a ``Success`` message, your configuration is ready to use. If you receive an error code, ensure your **Server URL** is correct. If it is, typically there was an error in the credential configuration and the configuration should be reconfigured.

## Configuring an SSO session ID token for CloudBees CD
The CloudBees CD plugin allows you to authenticate actions using an SSO session ID token. Before starting:
- You must have a generated CloudBees SSO session ID token. If you have not generated a CloudBees SSO session ID token, refer to [Generate an SSO session ID token](https://docs.cloudbees.com/docs/cloudbees-cd/latest/intro/sign-in-cd#_generate_a_sso_session_id_token) for help.
- You must have already set up a **CloudBees CD** configuration and saved it to Jenkins. If not, follow the steps in [Connecting to your CloudBees CD server](#connecting-to-your-cloudbees-cd-server) until **Credentials Type**. 

The following steps describe how to configure a CloudBees SSO session ID token within a CloudBees CD configuration.

1. Navigate to **Manage Jenkins** > **Configure System**. Under **CloudBees CD** > **Configurations**, find the configuration to which you want to add an SSO session ID token. 
2. Under **Credentials Type**, select **Stored Credential**. A new entry field opens.
3. For **Stored Credential**, select **Add** and **Jenkins**. A new window opens for the **Jenkins Credential Provider**.
4. By default, the **Domain** field is set to *Global credentials (unrestricted)* and is unchangeable. 
5. For **Kind**, select **Secret text**. 
6. Select the **Scope** you want to use your token for. 
7. In the **Secret** field, enter your CloudBees SSO session ID token. 
8. The **ID** field is **optional**. If you want to provide an **ID**, select the **?** icon and read the message to ensure you understand the purpose and requirements of this field.
9. The **Description** field is **optional**. However, it is suggested to provide one to help you keep track of the token configuration in your **Credentials**  profile.

> **_NOTE:_** Ensure the information for your credential is correct before moving to the next step. If your credential is configured with incorrect information, you cannot edit it, and must delete the credential and reconfigure a new one. 

10. Select **Add** to save the configuration. You are returned to the system configurations page.
> **_TIP:_**  Check your profile to ensure your credential was added and manage it. 
11. Select **Test Connection** to ensure your credential is working correctly. If you receive a ``Success`` message, your configuration is ready to use. If you receive an error code, ensure your **Server URL** is correct. If it is, typically there was an error in the credential configuration, and the configuration should be reconfigured.  

# Supported Post Build Actions

The CloudBees CD plugin enables you to perform post-build actions for your Jenkins jobs. These actions can be executed separately or combined sequentially. 

> **_NOTE:_** To manage **Post-build Actions**, navigate to your Jenkins job's **Configuration** > **Post-build Actions**. 

## Calling CloudBees CD REST API

The CloudBees CD plugin allows you to make calls to CloudBees CD/RO's REST API for post-build actions and as pipeline steps. For more information on using the CloudBees CD/RO REST API, refer to [Use the CloudBees CD/RO RESTful API](https://docs.cloudbees.com/docs/cloudbees-cd-api-rest/latest/).  

To configure calls to CloudBees CD/RO's REST API:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD - Call REST API`. 
4. Complete the fields with your configuration details. Select the **?** icon to view field information in a new dialog.
    > **_IMPORTANT:_** For API parameters that include the phrase, "`request=<API-NAME>` must appear in your query string" in their description, you must include `?request=<API-NAME>` in the **Path URL** field. For example:
    >
    >* `/objects?request=sendReportingData`: Where `?` designates a query string, `request` and `sendReportingData` are required by the `POST object` API. 
    
    > **_TIP:_** To reference CloudBees CD/RO REST API resources and descriptions, you can use CloudBees CD/RO's Swagger UI. To access the Swagger UI, navigate to `https://<cloudbees-cd-server_hostname>/rest/doc/v1.0/`, where `<cloudbees-cd-server_hostname>` is the host name or IP address of your CloudBees CD/RO server.
    >
    > For more information on the Swagger UI, refer to [Access the Swagger UI](https://docs.cloudbees.com/docs/cloudbees-cd-api-rest/latest/#_access_the_swagger_ui).


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

## Creating and deploying applications from Deployment Packages to CloudBees CD

Using deployment packages generated from your Jenkins CI builds, you can create and deploy Java, .NET, or any other
application to any environment in CloudBees CD/RO. 

> **_IMPORTANT:_** To create and deploy applications from a deployment package, the deployment package must contain a JSON manifest file and the artifacts to deploy.
> 
> Sample `manifest.json` files can be found in the [CloudBees CD plug repo](https://github.com/electric-cloud/DeploymentPackageManager/tree/master/SampleManifests).

To set up this post-build action:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD - Create/Deploy Application from Deployment Package`.
4. Complete the fields with your configuration details. Select the **?** icon to view field information in a new dialog.
5. To apply the configuration to your job, select **Save** in the Jenkins UI.

### Pipeline script example to create and deploy an application from a deployment package

``` syntaxhighlighter-pre
node {
    cloudBeesFlowCreateAndDeployAppFromJenkinsPackage configuration: 'CdConfiguration', filePath: 'CdProject/target/'
}
```

## Deploying applications using CloudBees CD

The CloudBees CD plugin enables you to deploy applications.

To set up this post-build action:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD - Deploy Application`.
4. Complete the fields with your configuration details. Select the **?** icon to view field information in a new dialog.
5. To apply the configuration to your job, select **Save** in the Jenkins UI.

### Pipeline script example to deploy an application

``` syntaxhighlighter-pre
node{
   cloudBeesFlowDeployApplication applicationName: 'DemoApplication', applicationProcessName: 'RunCommand', configuration: 'CdConfiguration', deployParameters: '{"runProcess":{"applicationName":"DemoApplication","applicationProcessName":"RunCommand","parameter":[{"actualParameterName":"Parameter1","value":"value1"},{"actualParameterName":"Parameter2","value":"value2"}]}}', environmentName: 'CdEnvironment', projectName: 'CloudBees'
}
```
## Publishing artifacts to CloudBees CD

The CloudBees CD plugin allows you to publish application artifacts generated as part of your Jenkins CI jobs directly to CloudBees CD/RO. 

To set up this post-build action:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD - Publish Artifact`.
4. Complete the fields with your configuration details. Select the **?** icon to view field information in a new dialog.
5. To apply the configuration to your job, select **Save** in the Jenkins UI.


### Pipeline script example tp publish artifacts

``` syntaxhighlighter-pre
node {
    cloudBeesFlowPublishArtifact artifactName: 'application:jpetstore', artifactVersion: '1.0', configuration: 'CdConfiguration', filePath: 'CdProject/target/jpetstore.war', repositoryName: 'default'
}
```
## Running pipelines in CloudBees CD

The CloudBees plugin allows you to run pipelines in CloudBees CD.

To set up this post-build action:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD - Run Pipeline`.
4. Complete the fields with your configuration details. Select the **?** icon to view field information in a new dialog.
5. To apply the configuration to your job, select **Save** in the Jenkins UI.

### Pipeline script example to run a pipeline

``` syntaxhighlighter-pre
node{
    cloudBeesFlowRunPipeline addParam: '{"pipeline":{"pipelineName":"CdPipeline","parameters":[{"parameterName":"PipelineParam","parameterValue":"185"}]}}', configuration: 'CdConfiguration', pipelineName: 'CdPipeline', projectName: 'CloudBees'
}
```

## Running procedures in CloudBees CD

The CloudBees CD plugin allows you to run procedures in CloudBees CD.

To set up this post-build action:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD - Run Procedure`.
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
## Triggering releases in CloudBees CD

This integration allows you to trigger a release in CloudBees CD/RO.

To set up this post-build action:
1. Navigate to your job's **Configuration** > **Post-build Actions** menu.
2. Select **Add post-build action**.
3. In the filter field, enter `CloudBees CD - Trigger Release`.
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
## Adding credentials to a new CloudBees CD configuration  
[BEE-27725] When creating a new CloudBees CD configuration, you cannot add a new credential using the **Add** button.
### Workarounds
You can use the following workarounds instead of the **Add** button to help you add a credential:
- The **Add** button works for existing configurations. Create and save your configuration without the credential. You can then return to the configuration and add the credential using the **Add** button.
- Add your credentials as described in Jenkins' [Configuring credentials](https://www.jenkins.io/doc/book/using/using-credentials/#configuring-credentials).

# Release Notes

## Version 1.1.34 (January 26, 2024)

- Internal improvements.
- Improved tool tips and documentation for *Calling CloudBees CD REST API* procedure.

## Version 1.1.33 (October 18, 2023)

- Internal improvements. 

## Version 1.1.32 (May 05, 2023)

- Added option to only run specified stages.
- Updated UI snippet generator to generate scripts that support running only specified stages.
- Improved Post Build Actions to support running only specified stages.
- Fixed issue where credentials could not be added to new Cloudbees CD configuration.
- Fixed issue with **Test Connection** failing when using token authentication. 
- Fixed issue with **Compare Before Apply** not comparing stage options that were changed. 
- Fixed issue with **Run Pipeline**, where when configuring a **Starting Stage**, an error was returned for fetching pipeline stages to run.
- Fixed issue when running **Run Pipeline** post build actions a `NullPointerException` was returned causing the associated build step to fail. 

## Version 1.1.31 (February 14, 2023)

- Added CloudBees CD/RO version dependencies section
- Fixed configuration as code issue with credentials ID.
- Updated connection configurations to support SSO.

## Version 1.1.30 (January 27, 2023)

- Removed dependencies on platform CGI scripts.

## Version 1.1.29 (November 25, 2022)

- Fixed plugin dependencies.

## Version 1.1.28 (November 15, 2022)

- Updated the plugin global configuration to support using stored credentials. *Username and password* or *Secret text (token)* can be used for connecting to CloudBees CD.
- Added support for Cloudbees CD tokens, which now can be configured default stored credentials (secret text) or to override configurations.
- Improved handling of override credentials.
- Fixed handling of stages in **Trigger Release**.

## Version 1.1.25 (January 14, 2022)

- Added support of folder credentials which now can be used within override credentials functionality.
- Fixed **Upload Artifact** when using agent.
- Improved integration of CloudBees CI and CD.

## Version 1.1.21 (March 12, 2021)

- Updated dependencies.

## Version 1.1.20 (March 10, 2021)

- Updated dependencies.

## Version 1.1.19 (February 11, 2021)

- Improved the **Run And Wait** option to support interrupting builds when Flow runtime was not finished successfully. This enhancement is applied to the following procedures:
    - **Trigger Release**
    - **Run Pipeline**
    - **Run Procedure**
    - **Publish Application**
    - **Create and Deploy Application from Deployment Package**
- Updated **Deploy application** to support specifying an environment project if it is different from the application project.
- Fixed the following procedures for new Jenkins (2.264+) according to Jenkins forms `tabular to div` changes: **Run Procedure**, **Run Pipeline**, **Deploy Application**, and **Trigger Release**.

## Version 1.1.18 (September 14, 2020)

- Updated **Publish Artifact** with **Relative Workspace** parameter.
- **Updated Run And Wait** parameter **checkInterval** with *minimum value*.
- Updated **Depend on CD job/pipeline outcome** functionality by association of CloudBees CD job/pipeline outcome _Warning_ with CloudBees CI build result _Unstable_.
- Updated build summary links for **Run Pipeline**, **Publish Artifact**, **Trigger Release**.
- Fixed snippet generator UI for pipeline steps with extra parameters, including **Run Procedure**, **Trigger Release**, **Run Pipeline**, **Deploy Application**).
- Event-based build watchers were improved and can now send build information after builds are finished.
- CloudBees CD **Multibranch Pipelines** support has been improved.
- Bug fixes and improvements.

## Version 1.1.17 (July 17, 2020)

- Improvements:

    - Updated the following post build actions with a **Run and Wait** option with possibility to depend on CD job or pipeline outcome:
        - **Run Pipeline**
        - **Trigger Release**
        - **Run Procedure**
        - **Deploy Application**
        - **Create and Deploy Application from Deployment Package**

## Version 1.1.16 (June 26, 2020)

- Improvements:
    - Improved **Run Procedure** with a **RunAndWait** option that includes additional **dependOnCdJobOutcome** and **checkInterval** sub-options.
    - Improved **Trigger Pipeline** error messages.
    - Improved **Run Procedure** error handling.
    - Improved build summary of **Deploy Application** with links to CD applications.

- Bugfixes:
    - Fixed an issue where **Publish Artifact** to created incorrect repository structures if artifacts were published from Jenkins agent nodes.
    - Fixed an issue cased by double slashes in **CD Artifacts URL**.
    - **Deploy Application** PBA now has a link to the exact application deployed instead of a link to all applications on customer instance.
    - Fixed an issue in EC-Jenkins when artifact URLs could not be retrieved for the **Report** stage of **Run And Monitor** and **Run And Wait** under certain conditions.
    - Fixed release retrieval code
    - Fixed issue where **Publish Artifacts** was failing successful builds under some conditions.
    - Fixed a bug when parameter **Stages to run** was ignored and all stages were running.

- Documentation, help tips and labels:
    - Updated plugin documentation with new screenshots and pipeline code samples.
    - Updated parameter labels for **Trigger release** to improve usability.
    - Updated help tips and added supported use cases for **Publish Artifacts** and **Create/Deploy Application from Deployment Package** to improve usability.

## Version 1.1.15 (June 1, 2020)

- Added support for new CI Build Detail APIs:
    - Added new Post Build Action **Associate Build To Release** to allow attaching CI Build Data of the independent builds to releases or release runs.
    - **Run Pipeline** now attaches CI Build Data to the triggered pipeline run.
    - **Trigger Release** now attaches CI Build Data to the triggered release pipeline run.

- Improved integration with CloudBees CD:
    - CI Build Data infrastructure has been created.
    - Created event-based watchers to send build data to CloudBees CD automatically if build is triggered by CloudBees CD.

- Renamed from "CloudBees Flow" to "CloudBees CD".

## Version 1.1.14 (May 6, 2020)

- Added support of Configuration as Code.
- Updated plugin dependencies.

## Version 1.1.13 (Apr 21, 2020)

- All Post Build Actions can now connect to CloudBees Flow as a user other than the one mentioned in the `electricflow` Plugin configuration, under **Manage Jenkins**.  These credentials which are used to override the connection credential at the level of the PBA only supports global credentials at this time.
- Fixed parameters collision for pipelines with the same name in the **Trigger Release** and  **Run Pipeline** Post Build Actions.
- Fixed corrupted artifact uploading in the **Publish Artifact** Post Build Action.
- Updated plugin dependencies.

## Version 1.1.12 (Dec 17, 2019)

Migrated plugin documentation from Wiki to GitHub.

## Version 1.1.11 (Dec 11, 2019)

Updated CloudBees Flow - **Publish Artifact**:
- Added pipeline compatibility.
- Fixed support of running on agents.

- Updated CloudBees Flow - **Create and Deploy Application from Deployment Package**:
    - Added pipeline compatibility.
    - Fixed support of running on agents.
    - Added link to CloudBees Flow job within summary of a build.

- Added pipeline function aliases for all post build actions.

- Added expand environment variable functionality for the following post build actions:
    - CloudBees Flow - **Call REST API**
    - CloudBees Flow - **Run Procedure**
    - CloudBees Flow - **Deploy Application**

- Added a sample [Jenkinsfile](https://github.com/jenkinsci/electricflow-plugin/blob/master/Jenkinsfile) to the plugin repository.

## Version 1.1.10 (Sep 26, 2019)

- Updated **CloudBees Flow - Call REST API** related functionality:

    -   Added support for a new workflow step, **CloudBees Flow - Call REST
        API**, which is based on the same functionality as corresponded post
        build action. Additionally, a snippet generator UI is available for the new workflow
        step.
    -   Result of calling CloudBees Flow REST API (JSON output) now can be
        stored within environment variables available within builds. They can
        also be returned by the new workflow step within scripted pipelines.
    -   Fixed URL on summary page of **Call REST API** jobs.

- Changed Jenkins baseline version for the plugin to 2.138.4.

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

- Support for the following **New Post Build Actions** have been added:

    -   ElectricFlow - Deploy Application
    -   ElectricFlow - Trigger Release
    -   ElectricFlow - Call REST API
    -   ElectricFlow - Run Procedure

- Post Build Action **ElectricFlow - Run Pipeline** modified as follows:

    -   It can now be run for pipelines without parameters.

- Post Build Action **ElectricFlow - Publish Artifact** modified as follows:

    -   Added support for publishing to both directories and subdirectories.
    -   Explicit error messages added for build runs.

- Usability changes:

    -   **Post Build Action** page shows dynamically retrieved values all the
        time.
    -   Two new buttons **Validate Before Apply** and **Compare Before Apply**
        added in **Post Build Action Pages for Deploy Applicatio**, **Trigger
        Release**, **Run Procedure** and **Run Pipeline**. This is to help ensure failures
        to retrieve information is handled gracefully (no stack traces), and users can understand the field errors before
        saving the configuration.
    -   More descriptive Help tips.
    -   More verbose messages when Test Connection fails with Electric Flow.
    -   More verbose logging on response body for failed Rest API calls.
    -   New option called **Override Electric Flow SSL Validation Check"** was
        introduced for testing connection with Electric Flow, where there is
        a need to test Electric Flow Post Build Actions before doing the SSL
        setup.

## Version 1.1.4 (Nov 22, 2017)

Post Build Action "ElectricFlow - Publish Artifact" modified as follows:

-   Fixed Scenarios where Build Step fails with exceptions.
-   Added Support for remoting (build on remote Windows machines).

## Version 1.1.3 (May 9, 2017)

Support added for running plugin tasks from jenkins pipeline as per
<https://issues.jenkins-ci.org/browse/JENKINS-44084>.

## Version 1.1.2 (Apr 28, 2017)

- More detailed output from plugin on build page results.
- Hierarchy files output added to build page.

## Version 1.1.1 (Apr 27, 2017)

General clean up of code.

## Version 1.0 (Apr 26, 2017)

Initial Release.
