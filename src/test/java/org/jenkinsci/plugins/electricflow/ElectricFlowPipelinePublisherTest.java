package org.jenkinsci.plugins.electricflow;

import static org.jenkinsci.plugins.electricflow.test.Utils.createConfigurationInJenkinsRule;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ElectricFlowPipelinePublisherTest {

    @Inject
    ElectricFlowPipelinePublisher.DescriptorImpl descriptor;

    private static JenkinsRule jenkinsRule;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
    }

    @BeforeEach
    void setUp() {
        jenkinsRule.getInstance().getInjector().injectMembers(this);
    }

    @Test
    void doConfigRoundTrip() throws Exception {
        // Name is defined in the Utils.checkConfiguration() to skip the checkConnection
        String configurationName = org.jenkinsci.plugins.electricflow.Utils.CONFIG_SKIP_CHECK_CONNECTION;
        Credential overrideCredential = new Credential("credential");
        String projectName = "Project name";
        String pipelineName = "Pipeline name";

        // Configuration should exist to be listed in a form
        createConfigurationInJenkinsRule(jenkinsRule, configurationName);

        // All fields (with override)
        doConfigRoundTrip(configurationName, overrideCredential, projectName, pipelineName);

        // No override
        doConfigRoundTrip(configurationName, null, projectName, pipelineName);
    }

    private void doConfigRoundTrip(
            String configName, Credential overrideCredential, String projectName, String pipelineName)
            throws Exception {

        ElectricFlowPipelinePublisher testPba = new ElectricFlowPipelinePublisher();
        testPba.setConfiguration(configName);
        testPba.setProjectName(projectName);
        testPba.setPipelineName(pipelineName);
        testPba.setOverrideCredential(overrideCredential);

        ElectricFlowPipelinePublisher configRoundTripResult = jenkinsRule.configRoundtrip(testPba);

        assertEquals(testPba.getConfiguration(), configRoundTripResult.getConfiguration());
        //    assertEquals(testPba.getProjectName(), configRoundTripResult.getProjectName());
        //    assertEquals(testPba.getReleaseName(), configRoundTripResult.getReleaseName());
        //    assertEquals(testPba.getOverrideCredential(),
        // configRoundTripResult.getOverrideCredential());
    }
}
