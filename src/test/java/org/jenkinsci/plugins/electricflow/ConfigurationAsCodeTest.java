package org.jenkinsci.plugins.electricflow;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
import io.jenkins.plugins.casc.model.CNode;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigurationAsCodeTest {
    @Rule
    public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("casc/configuration-as-code.yml")
    public void shouldBeAbleToAcceptConfiguration() {
        final ElectricFlowGlobalConfiguration electricFlowGlobalConfiguration = ExtensionList.lookupSingleton(ElectricFlowGlobalConfiguration.class);
        assertNotNull(electricFlowGlobalConfiguration);

        final List<Configuration> configurations = electricFlowGlobalConfiguration.getConfigurations();
        assertThat(configurations, Matchers.iterableWithSize(1));

        final Configuration configuration = configurations.get(0);
        assertEquals("this is the first configuration", configuration.getConfigurationName());
        assertEquals("test", configuration.getElectricFlowUser());
        assertThat(configuration.getElectricFlowPassword(), Matchers.notNullValue());
        assertEquals("https://test-url/", configuration.getElectricFlowUrl());
        assertEquals("/rest/v1.0", configuration.getElectricFlowApiVersion());
        assertTrue(configuration.getIgnoreSslConnectionErrors());
    }
}
