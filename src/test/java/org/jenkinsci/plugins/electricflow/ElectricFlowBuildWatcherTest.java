package org.jenkinsci.plugins.electricflow;

import static org.mockito.Mockito.mockStatic;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import java.util.Collections;
import java.util.Objects;
import org.jenkinsci.plugins.electricflow.action.CloudBeesCDPBABuildDetails;
import org.jenkinsci.plugins.electricflow.event.ElectricFlowBuildWatcher;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@WithJenkins
class ElectricFlowBuildWatcherTest {

    private static JenkinsRule jenkinsRule;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
    }

    @Test
    void testSendCiBuildDataWithOverriddenCredentials() throws Exception {

        Folder folder = jenkinsRule.jenkins.createProject(Folder.class, "folder");
        WorkflowJob job = folder.createProject(WorkflowJob.class, "workflow");

        // Folder credentials
        Iterable<CredentialsStore> stores = CredentialsProvider.lookupStores(folder);
        CredentialsStore folderStore = null;
        for (CredentialsStore s : stores) {
            if (s.getProvider() instanceof FolderCredentialsProvider && s.getContext() == folder) {
                folderStore = s;
                break;
            }
        }
        assert folderStore != null;
        String folderCredId = "fcreds";
        StandardUsernamePasswordCredentials folderCred = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, folderCredId, folderCredId, "folder-user", "folder-password");
        folderStore.addCredentials(Domain.global(), folderCred);
        folderStore.save();

        // Add global configuration
        jenkinsRule
                .jenkins
                .getExtensionList(ElectricFlowGlobalConfiguration.class)
                .get(0)
                .setConfigurations(Collections.singletonList(new Configuration(
                        "local-cd",
                        "http://localhost:8080",
                        "global-user",
                        Secret.fromString("global-password"),
                        "electricFlowApiVersion",
                        true,
                        false,
                        null,
                        null)));

        // No overrideCredentials provided
        Run<WorkflowJob, WorkflowRun> wRun1 =
                Objects.requireNonNull(job.scheduleBuild2(0)).get();
        CloudBeesCDPBABuildDetails.applyToRuntime(
                wRun1,
                "local-cd",
                null,
                "flowRuntimeId",
                "flowRuntimeStateId",
                "projectName",
                "releaseName",
                "stageName",
                CIBuildDetail.BuildTriggerSource.CI,
                CIBuildDetail.BuildAssociationType.TRIGGERED_BY_CI);

        try (MockedStatic<ElectricFlowClientFactory> factoryMock =
                mockStatic(ElectricFlowClientFactory.class, Mockito.CALLS_REAL_METHODS)) {
            ElectricFlowBuildWatcher electricFlowBuildWatcher = jenkinsRule
                    .jenkins
                    .getExtensionList(ElectricFlowBuildWatcher.class)
                    .get(0);
            electricFlowBuildWatcher.sendBuildDetailsToInstanceImproved(wRun1, TaskListener.NULL);
            factoryMock.verify(() -> ElectricFlowClientFactory.getElectricFlowClient("local-cd", null, wRun1, null));
        }

        // overrideCredentials provided, make sure the run context is passed in
        Run<WorkflowJob, WorkflowRun> wRun2 =
                Objects.requireNonNull(job.scheduleBuild2(0)).get();
        Credential flowFolderCred = new Credential(folderCredId);
        CloudBeesCDPBABuildDetails.applyToRuntime(
                wRun2,
                "local-cd",
                flowFolderCred,
                "flowRuntimeId",
                "flowRuntimeStateId",
                "projectName",
                "releaseName",
                "stageName",
                CIBuildDetail.BuildTriggerSource.CI,
                CIBuildDetail.BuildAssociationType.TRIGGERED_BY_CI);

        try (MockedStatic<ElectricFlowClientFactory> factoryMock =
                mockStatic(ElectricFlowClientFactory.class, Mockito.CALLS_REAL_METHODS)) {
            ElectricFlowBuildWatcher electricFlowBuildWatcher = jenkinsRule
                    .jenkins
                    .getExtensionList(ElectricFlowBuildWatcher.class)
                    .get(0);
            electricFlowBuildWatcher.sendBuildDetailsToInstanceImproved(wRun2, TaskListener.NULL);
            factoryMock.verify(
                    () -> ElectricFlowClientFactory.getElectricFlowClient("local-cd", flowFolderCred, wRun2, null));
        }
    }
}
