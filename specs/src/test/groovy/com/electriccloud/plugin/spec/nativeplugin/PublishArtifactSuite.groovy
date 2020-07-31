package com.electriccloud.plugin.spec.nativeplugin

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.core.artifacts.Artifact
import com.electriccloud.plugin.spec.core.artifacts.ArtifactVersion
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsBuildJob
import com.electriccloud.plugin.spec.nativeplugin.utils.JenkinsJobRunner
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
            invalid: 'incorrect'
    ]

    static def artNames = [
            correct: 'PBATests',
            invalid: 'incorrect'
    ]

    static def artKeys = [
            correct     : artGroups.correct + ':' + artNames.correct,
            invalid     : 'invalid',
            missingName : artGroups.correct + ':',
            missingGroup: ':' + artNames
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

    ]

    def doSetupSpec() {
        // Do project import here
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

}
