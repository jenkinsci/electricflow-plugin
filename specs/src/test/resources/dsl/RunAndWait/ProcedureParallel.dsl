def testProjectName = args.projectName
project testProjectName

procedure 'RunAndWaitParallel', {
  description = ''
  jobNameTemplate = ''
  projectName = testProjectName
  resourceName = ''
  timeLimit = ''
  timeLimitUnits = 'minutes'
  workspaceName = ''

  formalParameter 'buildParameters', defaultValue: '', {
    description = ''
    expansionDeferred = '0'
    label = null
    orderIndex = null
    required = '0'
    type = 'entry'
  }

  formalParameter 'jenkinsEnableParallelMode', defaultValue: '', {
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

  step 'RunAndWait1', {
    description = ''
    alwaysRun = '0'
    broadcast = '0'
    command = null
    condition = ''
    errorHandling = 'failProcedure'
    exclusiveMode = 'none'
    logFileName = null
    parallel = '1'
    postProcessor = null
    precondition = ''
    projectName = testProjectName
    releaseMode = 'none'
    resourceName = ''
    shell = null
    subprocedure = 'RunAndWait'
    subproject = '/plugins/EC-Jenkins/project'
    timeLimit = ''
    timeLimitUnits = 'minutes'
    workingDirectory = null
    workspaceName = ''
    actualParameter 'config_name', '$[configName]'
    actualParameter 'job_name', '$[jobName]'
    actualParameter 'parameters', '$[buildParameters]'
    actualParameter 'jenkins_enable_parallel_mode', '$[jenkinsEnableParallelMode]'
  }

  step 'RunAndWait2', {
    description = ''
    alwaysRun = '0'
    broadcast = '0'
    command = null
    condition = ''
    errorHandling = 'failProcedure'
    exclusiveMode = 'none'
    logFileName = null
    parallel = '1'
    postProcessor = null
    precondition = ''
    projectName = testProjectName
    releaseMode = 'none'
    resourceName = ''
    shell = null
    subprocedure = 'RunAndWait'
    subproject = '/plugins/EC-Jenkins/project'
    timeLimit = ''
    timeLimitUnits = 'minutes'
    workingDirectory = null
    workspaceName = ''
    actualParameter 'config_name', '$[configName]'
    actualParameter 'job_name', '$[jobName]'
    actualParameter 'parameters', '$[buildParameters]'
    actualParameter 'jenkins_enable_parallel_mode', '$[jenkinsEnableParallelMode]'
  }

  step 'RunAndWait3', {
    description = ''
    alwaysRun = '0'
    broadcast = '0'
    command = null
    condition = ''
    errorHandling = 'failProcedure'
    exclusiveMode = 'none'
    logFileName = null
    parallel = '1'
    postProcessor = null
    precondition = ''
    projectName = testProjectName
    releaseMode = 'none'
    resourceName = ''
    shell = null
    subprocedure = 'RunAndWait'
    subproject = '/plugins/EC-Jenkins/project'
    timeLimit = ''
    timeLimitUnits = 'minutes'
    workingDirectory = null
    workspaceName = ''
    actualParameter 'config_name', '$[configName]'
    actualParameter 'job_name', '$[jobName]'
    actualParameter 'parameters', '$[buildParameters]'
    actualParameter 'jenkins_enable_parallel_mode', '$[jenkinsEnableParallelMode]'
  }    

  // Custom properties

  property 'ec_customEditorData', {

    // Custom properties

    property 'parameters', {

      // Custom properties

      property 'buildParameters', {

        // Custom properties
        formType = 'standard'
      }

      property 'jenkinsEnableParallelMode', {

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
    }
  }
}
