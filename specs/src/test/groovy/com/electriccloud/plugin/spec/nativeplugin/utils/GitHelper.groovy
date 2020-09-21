package com.electriccloud.plugin.spec.nativeplugin.utils


// TODO: this class should be rewritten
class GitHelper {

    private String gitUsername
    private String gitPassword
    private static boolean GitFolderExist = false
    private commitCounter = 1

    GitHelper(){
        this.gitUsername = System.getenv('GIT_USER') ?: ''
        this.gitPassword = System.getenv('GIT_PASSWORD') ?: ''
    }

    // TODO: change git console commands to EC-Git plugin when it will be added to CD installer
    def createGitUserConfig(String repositoryFolder, String email = "tcommitter@example.com", String username = "Test Committer"){
        if (GitFolderExist) {
            return
        }
        def commands = [
                "git config --local user.email '${email}'",
                "git config --local user.name '${username}'",
        ]
        for (command in commands){
            this.executeGitCommand(command, repositoryFolder)
        }
        GitFolderExist = true
    }

    // TODO: change git console commands to EC-Git plugin when it will be added to CD installer
    String pullAndCheckoutToBranch(String gitRepository= "https://github.com/cbpluginstest/jenkinsfile-multibranch-test",
                                   String destDirectory="/tmp", String remoteBranch="build/parametrizedQA"){
        def repositoryFolder = "${destDirectory}/${gitRepository.split('/')[-1] - '.git'}"
        if (GitFolderExist){
            return repositoryFolder
        }
        def urlWithPassword = gitRepository.replace("://", "://${gitUsername}:${gitPassword}@")
        def commands = [
                ["git clone $gitRepository", destDirectory],
                ["git remote set-url origin ${urlWithPassword}", repositoryFolder],
                ["git fetch", repositoryFolder],
                ["git checkout $remoteBranch", repositoryFolder],
        ]
        for (command in commands){
            this.executeGitCommand(command[0], command[1])
        }
        return repositoryFolder
    }

    // TODO: change git console commands to EC-Git plugin when it will be added to CD installer
    String addNewChangeToFile(String fileName = "filesForCommit.txt", String newLine = "changes", String repositoryFolder){
        def date = new Date()
        File fileForCommits = new File("${repositoryFolder}/${fileName}")

        String currTime = date.format("dd/MM/yyyy-HH:mm:ss")
        fileForCommits.append(newLine + currTime + "\n")
        def commitMessage = "commit${commitCounter++}-${currTime}"

        def commands = [
                "git add ${fileName}",
                "git commit -m ${commitMessage}"
        ]
        for (command in commands){
            this.executeGitCommand(command, repositoryFolder)
        }
        return commitMessage + '\n'
    }

    String replaceTypeLineInJenkinsFile(String jenkisFileName="Jenkinsfile", String newType = "pipeline", String repositoryFolder) {
        File jenkinsFile = new File("${repositoryFolder}/${jenkisFileName}")
        String oldText = new File("${repositoryFolder}/${jenkisFileName}").text

        def date = new Date()
        String currTime = date.format("dd/MM/yyyy-HH:mm:ss")
        def commitMessage = "changed_type_to_${newType}_${currTime}"

        jenkinsFile.text = jenkinsFile.text.replaceAll("name: 'type', defaultValue: '.*'", "name: 'type', defaultValue: '${newType}'")
        def commands = [
                "git add ${jenkisFileName}",
                "git commit -m ${commitMessage}"
        ]

        if (oldText != jenkinsFile.text) {
            for (command in commands) {
                this.executeGitCommand(command, repositoryFolder)
            }
        }
        else {
            return
        }
        return commitMessage + '\n'
    }

    def gitPushToRemoteRepository(String remoteBranchName="build/parametrizedQA", String repositoryFolder){
        def commands = [
                "git push origin ${remoteBranchName}",
        ]
        for (command in commands){
            executeGitCommand(command, repositoryFolder)
        }
    }

    // TODO: change git console commands to EC-Git plugin when it will be added to CD installer
    def executeGitCommand(String command, String directory){
        println("execute command: ${command} in folder ${directory}")
        def proc = command.execute(null, new File(directory))
        def sout = new StringBuilder()
        def serr = new StringBuilder()
        proc.consumeProcessOutput(sout, serr)
        proc.waitForOrKill(15000)
        println "out> $sout err> $serr"
    }

}
