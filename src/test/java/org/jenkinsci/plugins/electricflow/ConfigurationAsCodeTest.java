package org.jenkinsci.plugins.electricflow;

import static io.jenkins.plugins.casc.misc.Util.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import hudson.ExtensionList;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;

import java.util.List;

import io.jenkins.plugins.casc.model.CNode;
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

  @Test
  @ConfiguredWithCode("casc/configuration-as-code.yml")
  public void shouldBeAbleToExportConfiguration() throws Exception {
    ConfiguratorRegistry registry = ConfiguratorRegistry.get();
    ConfigurationContext context = new ConfigurationContext(registry);
    CNode yourAttribute = getUnclassifiedRoot(context).get("electricflow");

    String exported = toYamlString(yourAttribute);

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    JsonNode exportedConfiguration = mapper.readTree(exported)
            .get("configurations").get(0);

    if (exportedConfiguration.hasNonNull("electricFlowPassword")) {
      ((ObjectNode)exportedConfiguration).put("electricFlowPassword", "xxx");
    }

    assertEquals("this is the first configuration", exportedConfiguration.get("configurationName").asText());
    assertEquals("test", exportedConfiguration.get("electricFlowUser").asText());
    assertThat(exportedConfiguration.get("electricFlowPassword").asText(), Matchers.notNullValue());
    assertEquals("https://test-url", exportedConfiguration.get("electricFlowUrl").asText());
    assertEquals("/rest/v1.0", exportedConfiguration.get("electricFlowApiVersion").asText());
    assertTrue(exportedConfiguration.get("ignoreSslConnectionErrors").asBoolean());
  }

  @Test
  @ConfiguredWithCode("casc/configuration-as-code-cred-id.yml")
  public void shouldBeAbleToExportConfigurationWithCredId() throws Exception {
    ConfiguratorRegistry registry = ConfiguratorRegistry.get();
    ConfigurationContext context = new ConfigurationContext(registry);
    CNode yourAttribute = getUnclassifiedRoot(context).get("electricflow");

    String exported = toYamlString(yourAttribute);

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    JsonNode exportedConfiguration = mapper.readTree(exported)
            .get("configurations").get(0);

    if (exportedConfiguration.hasNonNull("electricFlowPassword")) {
      ((ObjectNode)exportedConfiguration).put("electricFlowPassword", "xxx");
    }

    assertEquals("this is the second configuration", exportedConfiguration.get("configurationName").asText());
    assertEquals("cred-id-1", exportedConfiguration.get("credentialId").asText());
    assertEquals("storedCreds", exportedConfiguration.get("credsType").asText());
    assertEquals("https://test-url", exportedConfiguration.get("electricFlowUrl").asText());
    assertEquals("/rest/v1.0", exportedConfiguration.get("electricFlowApiVersion").asText());
    assertTrue(exportedConfiguration.get("ignoreSslConnectionErrors").asBoolean());
  }
}
