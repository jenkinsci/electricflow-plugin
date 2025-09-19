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
class ElectricFlowAssociateBuildToReleaseTest {

    @Inject
    ElectricFlowAssociateBuildToRelease.DescriptorImpl descriptor;

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
        String releaseName = "Release name";

        // Configuration should exist to be listed in a form
        createConfigurationInJenkinsRule(jenkinsRule, configurationName);

        // All fields
        doConfigRoundTrip(configurationName, overrideCredential, projectName, releaseName);

        // No override
        doConfigRoundTrip(configurationName, null, projectName, releaseName);
    }

    private void doConfigRoundTrip(
            String configName, Credential overrideCredential, String projectName, String releaseName) throws Exception {

        ElectricFlowAssociateBuildToRelease testPba = new ElectricFlowAssociateBuildToRelease();
        testPba.setConfiguration(configName);
        testPba.setProjectName(projectName);
        testPba.setReleaseName(releaseName);
        testPba.setOverrideCredential(overrideCredential);

        ElectricFlowAssociateBuildToRelease configRoundTripResult = jenkinsRule.configRoundtrip(testPba);

        assertEquals(testPba.getConfiguration(), configRoundTripResult.getConfiguration());
        //    assertEquals(testPba.getProjectName(), configRoundTripResult.getProjectName());
        //    assertEquals(testPba.getReleaseName(), configRoundTripResult.getReleaseName());
        //    assertEquals(testPba.getOverrideCredential(),
        // configRoundTripResult.getOverrideCredential());
    }
}
