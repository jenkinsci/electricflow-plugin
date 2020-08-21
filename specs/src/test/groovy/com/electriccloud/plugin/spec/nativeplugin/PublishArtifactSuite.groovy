package com.electriccloud.plugin.spec.nativeplugin

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.core.artifacts.Artifact
import com.electriccloud.plugin.spec.core.artifacts.ArtifactVersion
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsBuildJob
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsJobRunner
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Unroll

class PublishArtifactSuite extends JenkinsHelper {

    public static final String testPbaName = "cloudBeesFlowPublishArtifact"
    private static final String testProjectName = "Specs - electricflow-plugin - $testPbaName"

    private static final String PIPELINE_NAME = "nativeJenkinsPBAExtendedPipelineProject"
    public static final String CI_CONFIG_NAME = "electricflow"

    private static JenkinsJobRunner jjr = JenkinsJobRunner.getInstance()

    @Shared
    String caseId, logMessage

    static def artGroups = [
            correct: 'pv',
            invalid: 'incorrect',
            relWorkspace: 'test'
    ]

    static def artNames = [
            correct: 'PBATests',
            invalid: 'incorrect',
            relWorkspace: "PublishArtifactWorkspace",

    ]

    static def artKeys = [
            correct     : artGroups.correct + ':' + artNames.correct,
            invalid     : 'invalid',
            missingName : artGroups.correct + ':',
            missingGroup: ':' + artNames,
            relWorkspace: "${artGroups.relWorkspace}:${artNames.relWorkspace}"
    ]

    static def repos = [
            correct: 'default',
            invalid: 'incorrect'
    ]

    static def pathes = [
            correct: 'build/libs/gradle-test-build-4.9.jar',
            invalid: 'incorrect'
    ]

    static def logMessages = [
            noFilesFound       : 'No files were found in path',
            noRepository       : 'publishArtifactAPI error [NoSuchRepository]',
            invalidArtifactName: 'error [InvalidArtifactName]: \'artifactName\' must be in the form',
            nonExisting: /basedir .* does not exist/,

    ]

    static ciPipelinesNames = [
            relativeWorkspaces: "PublishArtifactSuite"
    ]

    def doSetupSpec() {
        importJenkinsJob('publishArtifactSuite.xml', ciPipelinesNames.relativeWorkspaces)
        dsl("deleteArtifact(artifactName: \"test:PublishArtifactWorkspace\")")
        dsl("""
artifact 'test:PublishArtifactWorkspace', artifactKey: 'PublishArtifactWorkspace', {
  description = ''
  artifactVersionNameTemplate = ''
  groupId = 'test'
}
""")
    }

    def "C499949. Publish artifact"() {
        given: 'Parameters for the pipeline'

        String artGroup = artGroups.correct
        String artName = artNames.correct
        String repoName = repos.correct
        String path = pathes.correct

        // Find artifact versions that exist at a moment
        Artifact artifact = new Artifact(artGroup, artName)
        String lastArtifactVersionBefore = artifact.getVersions()?.last()?.getVersion()

        def ciPipelineParameters = [
                flowConfigName    : CI_CONFIG_NAME,
                flowArtifactoryKP : artGroup + ':' + artName,
                artifactPath      : path,
                flowRepositoryName: repoName,
                runOnly           : testPbaName
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(PIPELINE_NAME, ciPipelineParameters)

        then: 'Collecting the result objects'
        assert ciJob.isSuccess(): "Pipeline on Jenkins is finished with success."

        artifact.refresh()
        ArtifactVersion lastVersion = artifact.getVersions()?.last()

        if (lastArtifactVersionBefore) {
            String lastArtifactVersionAfter = lastVersion.getVersion()
            assert lastArtifactVersionBefore != lastArtifactVersionAfter
        }

        then: "check the artifact properties"

        assert lastVersion['artifactVersionState'] == 'available'
        assert lastVersion['repositoryName'] == 'default'
    }

    @Issue("NTVEPLUGIN-165")
    @Unroll
    def "testing of relative workspaces"() {
        given: 'Parameters for the pipeline'
        def ciPipelineParameters = [
                configuration    : CI_CONFIG_NAME,
                artifactName: artifactName,
                artifactVersion: artifactVersion,
                artifactPath: artifactPath,
                relativeWorkspace: relativeWorkspace,
                repositoryName: repositoryName,
        ]

        when: 'Run pipeline and collect run properties'
        def (artGroup, artName) = artifactName.split(':')
        Artifact artifact = new Artifact(artGroup, artName)
        // if artifact exists, but artifactVersions don't , getVersions returns empty list
        String lastArtifactVersionBefore = artifact.getVersions() ? artifact.getVersions().last() : ''

        JenkinsBuildJob ciJob = jjr.run(ciPipelinesNames.relativeWorkspaces, ciPipelineParameters)

        then: 'Collecting the result objects'
        assert ciJob.isSuccess(): "Pipeline on Jenkins is finished with success."

        artifact.refresh()
        ArtifactVersion lastVersion = artifact.getVersions()?.last()
        if (lastArtifactVersionBefore) {
            String lastArtifactVersionAfter = lastVersion.getVersion()
            assert lastArtifactVersionBefore != lastArtifactVersionAfter
        }

        def manifest = getManifestOfArtifact("${artifactName}:${artifactVersion}")
        verifyFilesRegardingManifestRecursively(artifactPath.split('/'), manifest)

        where:
        caseId | artifactName          | artifactVersion | artifactPath                         | relativeWorkspace | repositoryName
        '1'    | artKeys.relWorkspace  | '1'             | 'buildArtifact1.log'                 | ''                | repos.correct
        '2'    | artKeys.relWorkspace  | '2'             | 'build/buildArtifact2.log'           | ''                | repos.correct
        '3'    | artKeys.relWorkspace  | '3'             | 'buildArtifact2.log'                 | 'build'           | repos.correct
        '4'    | artKeys.relWorkspace  | '4'             | 'buildArtifact3.log'                 | 'build2/folder2'  | repos.correct
        '5'    | artKeys.relWorkspace  | '5'             | 'folder2/buildArtifact3.log'         | 'build2'          | repos.correct
        '6'    | artKeys.relWorkspace  | '6'             | 'build2/folder2/buildArtifact3.log'  |  ''               | repos.correct
    }

    @Issue("NTVEPLUGIN-165")
    @Unroll
    def "negative testing of relative workspaces"() {
        given: 'Parameters for the pipeline'
        def ciPipelineParameters = [
                configuration    : CI_CONFIG_NAME,
                artifactName: artifactName,
                artifactVersion: artifactVersion,
                artifactPath: artifactPath,
                relativeWorkspace: relativeWorkspace,
                repositoryName: repositoryName,
        ]
        when: 'Run pipeline and collect run properties'
        def (artGroup, artName) = artifactName.split(':')
        Artifact artifact = new Artifact(artGroup, artName)
        def listOfArtifactsBeforeProcedureRun = artifact.getVersions().collect { it.getVersion() }.sort()

        JenkinsBuildJob ciJob = jjr.run(ciPipelinesNames.relativeWorkspaces, ciPipelineParameters)
        artifact.refresh()
        def listOfArtifactsAfterProcedureRun = artifact.getVersions().collect { it.getVersion() }.sort()

        then: 'Collecting the result objects'
        assert !ciJob.isSuccess(): "Pipeline on Jenkins is finished with error"
        assert ciJob.logs =~ logMessage

        assert listOfArtifactsBeforeProcedureRun == listOfArtifactsAfterProcedureRun
        where:
        caseId | artifactName          | artifactVersion | artifactPath                         | relativeWorkspace | repositoryName  | logMessage
        '7'    | artKeys.relWorkspace  | '7'             | 'buildArtifact1.log'                 | pathes.invalid    | repos.correct   | logMessages.nonExisting
        '8'    | artKeys.relWorkspace  | '8'             | 'wrong'                              | 'build'           | repos.correct   | logMessages.noFilesFound

    }

    @Unroll
    def "#caseId. Publish artifact - Negative"() {
        given: 'Parameters for the pipeline'

        def ciPipelineParameters = [
                flowConfigName    : CI_CONFIG_NAME,
                flowArtifactoryKP : artKey,
                artifactPath      : path,
                flowRepositoryName: repoName,
                runOnly           : testPbaName
        ]

        when: 'Run pipeline and collect run properties'
        JenkinsBuildJob ciJob = jjr.run(PIPELINE_NAME, ciPipelineParameters)

        then: 'Collecting the result objects'
        assert ciJob.outcome == 'success': "Pipeline on Jenkins was started."
        assert !ciJob.isSuccess(): "Pipeline on Jenkins is finished with error"
        assert ciJob.consoleLogContains(logMessage)

        where:
        caseId      | artKey               | path           | repoName      | logMessage
        '500310.1'  | artKeys.invalid      | pathes.correct | repos.correct | logMessages.invalidArtifactName
        '500310.2'  | artKeys.correct      | pathes.invalid | repos.correct | logMessages.noFilesFound
        '500310.3'  | artKeys.correct      | pathes.correct | repos.invalid | logMessages.noRepository

        // Dev: incomplete artifact key
        '500310.xx' | artKeys.missingGroup | pathes.correct | repos.correct | logMessages.invalidArtifactName
        '500310.xx' | artKeys.missingName  | pathes.correct | repos.correct | logMessages.invalidArtifactName
    }

    def getManifestOfArtifact(String artifactVersion){
        def dslContent = """
project 'testCli', {

  procedure 'getManifest', {

    formalParameter 'artifactVersion', defaultValue: '', {
      type = 'entry'
    }

    step 'executeCLI', {
      command = 'ectool getManifest \$[artifactVersion]'
      shell = 'bash'
    }

  }
}
"""
        dsl(dslContent)
        def resultXML = runProcedure('testCli', 'getManifest', [artifactVersion: artifactVersion])
        def manifest = new XmlSlurper().parseText(resultXML.logs)
        return manifest
    }

    void verifyFilesRegardingManifestRecursively(def objects, def node){
        if (objects.size() == 1) {
            assert node.file.@name == objects[0]
        }
        else {
            assert node.directory.@name == objects[0]
            verifyFilesRegardingManifestRecursively(objects[1..-1], node.directory)
        }
    }

}
