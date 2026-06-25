package org.jenkinsci.plugins.electricflow;

import static org.jenkinsci.plugins.electricflow.test.Utils.createConfigurationInJenkinsRule;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.Collections;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.MockedStatic;

/**
 * Regression test for SECO-5550 / the behavior change introduced in PR #408.
 *
 * <p>PR #408 moved {@code run.addAction(new SummaryTextAction(...))} inside the try block that wraps
 * {@code attachCIBuildDetails}. As a side effect, whenever attaching the CI build details fails
 * (e.g. a 403 AccessDenied when the CD user lacks modify privilege on the runtime), the build
 * summary was silently suppressed. This test pins the corrected behavior: the summary must always be
 * attached even when {@code attachCIBuildDetails} throws.
 */
public class ElectricFlowPipelinePublisherSummaryTest {

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void summaryIsAddedEvenWhenAttachCIBuildDetailsFails() throws Exception {
        String configurationName = Utils.CONFIG_SKIP_CHECK_CONNECTION;
        createConfigurationInJenkinsRule(jenkinsRule, configurationName);

        // A pipeline run response that the publisher can parse for flowRuntimeId / projectName.
        String pipelineRunResponse = new JSONObject()
                .element(
                        "flowRuntime",
                        new JSONObject()
                                .element("pipelineId", "pipeline-id")
                                .element("flowRuntimeId", "flow-runtime-id")
                                .element("projectName", "CloudBees"))
                .toString();

        ElectricFlowClient mockClient = mock(ElectricFlowClient.class);
        when(mockClient.getElectricFlowUrl()).thenReturn("http://localhost");
        when(mockClient.getPipelineId(anyString(), anyString())).thenReturn("pipeline-id");
        when(mockClient.getPipelineFormalParameters(anyString())).thenReturn(Collections.emptyList());
        when(mockClient.runPipeline(any(), any(), any(), any(), any(), any())).thenReturn(pipelineRunResponse);
        // Simulate the 403 AccessDenied that CD/RO returns when the user lacks modify privilege.
        when(mockClient.attachCIBuildDetails(any()))
                .thenThrow(new RuntimeException(
                        "Failed : HTTP error code : 403, Forbidden, does not have modify privilege on flowRuntime"));

        ElectricFlowPipelinePublisher publisher = new ElectricFlowPipelinePublisher();
        publisher.setConfiguration(configurationName);
        publisher.setProjectName("CloudBees");
        publisher.setPipelineName("JenkinsTestPipeline");

        // An empty build gives us a real Run (with a workspace) to drive perform() on the test
        // thread, so the static factory mock below stays in effect for the publisher's logic.
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        FreeStyleBuild build = jenkinsRule.buildAndAssertSuccess(project);

        try (MockedStatic<ElectricFlowClientFactory> factoryMock = mockStatic(ElectricFlowClientFactory.class)) {
            factoryMock
                    .when(() ->
                            ElectricFlowClientFactory.getElectricFlowClient(anyString(), any(), any(Run.class), any()))
                    .thenReturn(mockClient);

            publisher.perform(build, build.getWorkspace(), jenkinsRule.createLocalLauncher(), TaskListener.NULL);
        }

        // The attach failed, but the build must not be marked as a failure because of it ...
        assertEquals(Result.SUCCESS, build.getResult());
        // ... and the summary must still be present on the run.
        assertNotNull(
                "SummaryTextAction must be added even when attachCIBuildDetails throws",
                build.getAction(SummaryTextAction.class));
    }
}
