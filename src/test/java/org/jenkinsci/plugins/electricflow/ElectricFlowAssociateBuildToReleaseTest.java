package org.jenkinsci.plugins.electricflow;

import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;
import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ElectricFlowAssociateBuildToReleaseTest {

  @ClassRule public static JenkinsRule jenkinsRule = new JenkinsRule();

  @Inject ElectricFlowAssociateBuildToRelease.DescriptorImpl descriptor;

  @Before
  public void setUp() {
    jenkinsRule.getInstance().getInjector().injectMembers(this);
  }

  @Test
  public void configRoundTrip() throws Exception {
    // Configuration name
    // Name is defined in the Utils.checkConfiguration to skip the checkConnection
    String configurationName = "__SKIP_CHECK_CONNECTION__";

    // Override
    Credential overrideCredential = new Credential("credetial");

    // Project name
    String projectName = "Project name";

    // Release name
    String releaseName = "Release name";

    // All fields
    configRoundTripManual(
        configurationName,
        overrideCredential,
        projectName,
        releaseName
    );

    // No override
    configRoundTripManual(
        configurationName,
        null,
        projectName,
        releaseName
    );

  }

  private void configRoundTripManual (
      String configName, Credential overrideCredential, String projectName, String releaseName)
      throws Exception {

    FreeStyleProject p = jenkinsRule.createFreeStyleProject();
    ElectricFlowAssociateBuildToRelease before = new ElectricFlowAssociateBuildToRelease();
    before.setConfiguration(configName);
    before.setProjectName(projectName);
    before.setReleaseName(releaseName);
    before.setOverrideCredential(overrideCredential);

    p.getPublishersList().add(before);
    jenkinsRule.submit(
        jenkinsRule.createWebClient().getPage(p,"configure").getFormByName("config")
    );

    ElectricFlowAssociateBuildToRelease after = p.getPublishersList()
        .get(ElectricFlowAssociateBuildToRelease.class);

    jenkinsRule.assertEqualBeans(
        before, after, "configuration,overrideCredential,projectName,releaseName"
    );
  }

  private void configRoundTrip(
      String configName, Credential overrideCredential, String projectName, String releaseName)
      throws Exception {

    ElectricFlowAssociateBuildToRelease testPba =
        new ElectricFlowAssociateBuildToRelease();
    testPba.setConfiguration(configName);
    testPba.setProjectName(projectName);
    testPba.setReleaseName(releaseName);
    testPba.setOverrideCredential(overrideCredential);

    ElectricFlowAssociateBuildToRelease configRoundTripResult =
        jenkinsRule.configRoundtrip(testPba);

    assertEquals(testPba.getConfiguration(), configRoundTripResult.getConfiguration());
    assertEquals(testPba.getProjectName(), configRoundTripResult.getProjectName());
    assertEquals(testPba.getReleaseName(), configRoundTripResult.getReleaseName());
    assertEquals(testPba.getOverrideCredential(), configRoundTripResult.getOverrideCredential());
  }
}
