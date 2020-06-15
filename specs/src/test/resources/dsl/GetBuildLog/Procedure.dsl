def testProjectName = args.projectName
project testProjectName

procedure 'GetBuildLog', {
  description = ''
  jobNameTemplate = ''
  projectName = testProjectName
  resourceName = ''
  timeLimit = ''
  timeLimitUnits = 'minutes'
  workspaceName = ''

  formalParameter 'buildNumber', defaultValue: '', {
    description = ''
    expansionDeferred = '0'
    label = null
    orderIndex = null
    required = '0'
    type = 'entry'
  }

  formalParameter 'configName', defaultValue: '', {
    description = ''
    expansionDeferred = '0'
    label = null
    orderIndex = null
    required = '1'
    type = 'entry'
  }

  formalParameter 'jobName', defaultValue: '', {
    description = ''
    expansionDeferred = '0'
    label = null
    orderIndex = null
    required = '1'
    type = 'entry'
  }

  formalParameter 'propertyPath', defaultValue: '', {
    description = ''
    expansionDeferred = '0'
    label = null
    orderIndex = null
    required = '0'
    type = 'entry'
  }

  step 'GetBuildLog', {
    description = ''
    alwaysRun = '0'
    broadcast = '0'
    command = null
    condition = ''
    errorHandling = 'failProcedure'
    exclusiveMode = 'none'
    logFileName = null
    parallel = '0'
    postProcessor = null
    precondition = ''
    projectName = testProjectName
    releaseMode = 'none'
    resourceName = ''
    shell = null
    subprocedure = 'GetBuildLog'
    subproject = '/plugins/EC-Jenkins/project'
    timeLimit = ''
    timeLimitUnits = 'minutes'
    workingDirectory = null
    workspaceName = ''
    actualParameter 'build_number', '$[buildNumber]'
    actualParameter 'config_name', '$[configName]'
    actualParameter 'job_name', '$[jobName]'
    actualParameter 'result_outpp', '$[propertyPath]'
  }

  // Custom properties

  property 'ec_customEditorData', {

    // Custom properties

    property 'parameters', {

      // Custom properties

      property 'buildNumber', {

        // Custom properties
        formType = 'standard'
      }

      property 'configName', {

        // Custom properties
        formType = 'standard'
      }

      property 'jobName', {

        // Custom properties
        formType = 'standard'
      }

      property 'propertyPath', {

        // Custom properties
        formType = 'standard'
      }
    }
  }
}
