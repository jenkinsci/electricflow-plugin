package org.jenkinsci.plugins.electricflow;

import static org.jenkinsci.plugins.electricflow.test.Utils.createConfigurationInJenkinsRule;
import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ElectricFlowPipelinePublisherTest {

  @ClassRule public static JenkinsRule jenkinsRule = new JenkinsRule();

  @Inject ElectricFlowPipelinePublisher.DescriptorImpl descriptor;

  @Before
  public void setUp() {
    jenkinsRule.getInstance().getInjector().injectMembers(this);
  }

  @Test
  public void doConfigRoundTrip() throws Exception {
    // Name is defined in the Utils.checkConfiguration() to skip the checkConnection
    String configurationName = "__SKIP_CHECK_CONNECTION__";
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
