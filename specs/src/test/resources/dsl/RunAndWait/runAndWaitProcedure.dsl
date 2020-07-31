procedure 'runAndWaitProcedure', {
  description = ''
  jobNameTemplate = ''
  projectName = 'pvNativeJenkinsProject01'
  resourceName = ''
  timeLimit = ''
  timeLimitUnits = 'minutes'
  workspaceName = ''

  formalParameter 'procedureOutcome', defaultValue: '', {
    description = ''
    expansionDeferred = '0'
    label = null
    orderIndex = null
    required = '0'
    type = 'entry'
  }

  formalParameter 'sleepTime', defaultValue: '', {
    description = ''
    expansionDeferred = '0'
    label = null
    orderIndex = null
    required = '0'
    type = 'entry'
  }

  step 'testCommandStep', {
    description = ''
    alwaysRun = '0'
    broadcast = '0'
    command = '''import com.electriccloud.client.groovy.ElectricFlow


class Test {

    static def main(){
        println "This is remote Procedure from Flow"
        println "Project: pvNativeJenkinsProject01 / Procedure: nativeJenkinsTestProcedure"
        def command1 = "hostname".execute()
        def command2 = "pwd".execute()
        command1.waitFor()
        command2.waitFor()
        println "Host Name: ${command1.in.text}"
        println "Path to run: ${command2.in.text}"
    }

    def runAndWait(){
        if (!(\'$[procedureOutcome]\' in [\'success\', \'error\', \'warning\'])){
            return
        }
        if (\'$[sleepTime]\'){
            sleep(\'$[sleepTime]\'.toInteger()*1000)
        }
        if (\'$[procedureOutcome]\' == \'success\'){
            println "job result success"
        }
        else if (\'$[procedureOutcome]\' == \'error\') {
            println "job result error"
            throw new Exception("it is a groovy exception")
        }
        else if (\'$[procedureOutcome]\' == \'warning\') {
            println "job result warning"
            ElectricFlow ef = new ElectricFlow()
            ef.setProperty(propertyName: \'/myJob/outcome\', value: \'warning\')
        }
    }
}


Test.main()
def test = new Test()
test.runAndWait()'''
    condition = ''
    errorHandling = 'failProcedure'
    exclusiveMode = 'none'
    logFileName = ''
    parallel = '0'
    postProcessor = ''
    precondition = ''
    projectName = 'pvNativeJenkinsProject01'
    releaseMode = 'none'
    resourceName = ''
    shell = 'ec-groovy'
    subprocedure = null
    subproject = null
    timeLimit = ''
    timeLimitUnits = 'minutes'
    workingDirectory = ''
    workspaceName = ''
  }

  // Custom properties

  property 'ec_customEditorData', {

    // Custom properties

    property 'parameters', {

      // Custom properties

      property 'procedureOutcome', {

        // Custom properties
        formType = 'standard'
      }

      property 'sleepTime', {

        // Custom properties
        formType = 'standard'
      }

      property 'testParam1', {

        // Custom properties
        formType = 'standard'
      }

      property 'testParam2', {

        // Custom properties
        formType = 'standard'
      }
    }
  }
}