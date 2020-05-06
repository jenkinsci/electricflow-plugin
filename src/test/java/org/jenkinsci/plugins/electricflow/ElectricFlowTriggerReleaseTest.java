package org.jenkinsci.plugins.electricflow;

import static org.jenkinsci.plugins.electricflow.test.Utils.createConfigurationInJenkinsRule;
import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ElectricFlowTriggerReleaseTest {

  @ClassRule public static JenkinsRule jenkinsRule = new JenkinsRule();

  @Inject ElectricFlowTriggerRelease.DescriptorImpl descriptor;

  @Before
  public void setUp() {
    jenkinsRule.getInstance().getInjector().injectMembers(this);
  }

  @Test
  public void doConfigRoundTrip() throws Exception {
    // Name is defined in the Utils.checkConfiguration() to skip the checkConnection
    String configurationName =
        org.jenkinsci.plugins.electricflow.Utils.CONFIG_SKIP_CHECK_CONNECTION;
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
      String configName, Credential overrideCredential, String projectName, String releaseName)
      throws Exception {

    ElectricFlowTriggerRelease testPba = new ElectricFlowTriggerRelease();
    testPba.setConfiguration(configName);
    testPba.setProjectName(projectName);
    testPba.setReleaseName(releaseName);
    testPba.setOverrideCredential(overrideCredential);

    ElectricFlowTriggerRelease configRoundTripResult = jenkinsRule.configRoundtrip(testPba);

    assertEquals(testPba.getConfiguration(), configRoundTripResult.getConfiguration());
    //    assertEquals(testPba.getProjectName(), configRoundTripResult.getProjectName());
    //    assertEquals(testPba.getReleaseName(), configRoundTripResult.getReleaseName());
    //    assertEquals(testPba.getOverrideCredential(),
    // configRoundTripResult.getOverrideCredential());
  }
}
