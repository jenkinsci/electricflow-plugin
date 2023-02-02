package org.jenkinsci.plugins.electricflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

public class ConfigurationAsCodeTest {
  @Rule public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

  @Test
  @ConfiguredWithCode("casc/configuration-as-code.yml")
  public void shouldBeAbleToAcceptConfiguration() {
    final ElectricFlowGlobalConfiguration electricFlowGlobalConfiguration =
        ExtensionList.lookupSingleton(ElectricFlowGlobalConfiguration.class);
    assertNotNull(electricFlowGlobalConfiguration);

    final List<Configuration> configurations = electricFlowGlobalConfiguration.getConfigurations();
    assertThat(configurations, Matchers.iterableWithSize(1));

    final Configuration configuration = configurations.get(0);
    assertEquals("this is the first configuration", configuration.getConfigurationName());
    assertEquals("test", configuration.getElectricFlowUser());
    assertThat(configuration.getElectricFlowPassword(), Matchers.notNullValue());
    assertEquals("https://test-url", configuration.getElectricFlowUrl());
    assertEquals("/rest/v1.0", configuration.getElectricFlowApiVersion());
    assertTrue(configuration.getIgnoreSslConnectionErrors());
  }

  @Test
  @ConfiguredWithCode("casc/configuration-as-code-cred-id.yml")
  public void shouldBeAbleToAcceptConfigurationWithCredId() {
    final ElectricFlowGlobalConfiguration electricFlowGlobalConfiguration =
            ExtensionList.lookupSingleton(ElectricFlowGlobalConfiguration.class);
    assertNotNull(electricFlowGlobalConfiguration);

    final List<Configuration> configurations = electricFlowGlobalConfiguration.getConfigurations();
    assertThat(configurations, Matchers.iterableWithSize(1));

    final Configuration configuration = configurations.get(0);
    assertEquals("this is the second configuration", configuration.getConfigurationName());
    assertEquals("cred-id-1", configuration.getCredentialId());
    assertEquals("cred-id-1", configuration.getOverrideCredential().getCredentialId());
    assertEquals("storedCreds", configuration.getCredsType());
    assertEquals("https://test-url", configuration.getElectricFlowUrl());
    assertEquals("/rest/v1.0", configuration.getElectricFlowApiVersion());
    assertTrue(configuration.getIgnoreSslConnectionErrors());
  }
}
