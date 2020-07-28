release 'TriggerReleaseRunAndWait', {
  description = ''
  disableMultipleActiveRuns = '0'
  plannedEndDate = '2020-08-05'
  plannedStartDate = '2020-07-22'
  projectName = 'pvNativeJenkinsProject01'
  timeZone = null

  pipeline 'pipeline_TriggerReleaseRunAndWait', {
    disableMultipleActiveRuns = '0'
    disableRestart = '0'
    enabled = '1'
    overrideWorkspace = '0'
    pipelineRunNameTemplate = null
    projectName = 'pvNativeJenkinsProject01'
    releaseName = 'TriggerReleaseRunAndWait'
    skipStageMode = 'ENABLED'
    templatePipelineName = null
    templatePipelineProjectName = null
    type = null
    workspaceName = null

    formalParameter 'procedureOutcome', defaultValue: null, {
      expansionDeferred = '0'
      label = null
      orderIndex = '1'
      required = '1'
      type = 'entry'
    }

    formalParameter 'sleepTime', defaultValue: null, {
      expansionDeferred = '0'
      label = null
      orderIndex = '2'
      required = '1'
      type = 'entry'
    }

    formalParameter 'ec_stagesToRun', defaultValue: null, {
      expansionDeferred = '1'
      label = null
      orderIndex = null
      required = '0'
      type = null
    }

    stage 'Stage 1', {
      colorCode = null
      completionType = 'auto'
      condition = null
      duration = null
      parallelToPrevious = null
      pipelineName = 'pipeline_TriggerReleaseRunAndWait'
      plannedEndDate = null
      plannedStartDate = null
      precondition = null
      projectName = 'pvNativeJenkinsProject01'
      resourceName = null
      waitForPlannedStartDate = '0'

      gate 'PRE', {
        condition = null
        precondition = null
        projectName = 'pvNativeJenkinsProject01'
      }

      gate 'POST', {
        condition = null
        precondition = null
        projectName = 'pvNativeJenkinsProject01'
      }

      task 'RunProcedure', {
        description = ''
        actionLabelText = null
        actualParameter = [
          'procedureOutcome': '$[procedureOutcome]',
          'sleepTime': '$[sleepTime]',
        ]
        advancedMode = '0'
        afterLastRetry = null
        allowOutOfOrderRun = '0'
        allowSkip = null
        alwaysRun = '0'
        ciConfigurationName = null
        ciJobFolder = null
        ciJobName = null
        condition = null
        customLabel = null
        deployerExpression = null
        deployerRunType = null
        disableFailure = null
        duration = null
        emailConfigName = null
        enabled = '1'
        environmentName = null
        environmentProjectName = null
        environmentTemplateName = null
        environmentTemplateProjectName = null
        errorHandling = 'stopOnError'
        gateCondition = null
        gateType = null
        groupName = null
        groupRunType = null
        insertRollingDeployManualStep = '0'
        instruction = null
        notificationEnabled = null
        notificationTemplate = null
        parallelToPrevious = null
        plannedEndDate = null
        plannedStartDate = null
        precondition = null
        projectName = 'pvNativeJenkinsProject01'
        requiredApprovalsCount = null
        resourceName = ''
        retryCount = null
        retryInterval = null
        retryType = null
        rollingDeployEnabled = null
        rollingDeployManualStepCondition = null
        skippable = '0'
        snapshotName = null
        stageSummaryParameters = null
        startingStage = null
        subErrorHandling = null
        subapplication = null
        subpipeline = null
        subpluginKey = null
        subprocedure = 'runAndWaitProcedure'
        subprocess = null
        subproject = 'pvNativeJenkinsProject01'
        subrelease = null
        subreleasePipeline = null
        subreleasePipelineProject = null
        subreleaseSuffix = null
        subservice = null
        subworkflowDefinition = null
        subworkflowStartingState = null
        taskProcessType = null
        taskType = 'PROCEDURE'
        triggerType = null
        useApproverAcl = '0'
        waitForPlannedStartDate = '0'
      }
    }

    // Custom properties

    property 'ec_counters', {

      // Custom properties
      pipelineCounter = '1'
    }
  }
}