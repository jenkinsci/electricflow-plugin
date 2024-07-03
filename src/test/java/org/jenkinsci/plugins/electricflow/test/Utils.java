package org.jenkinsci.plugins.electricflow.test;

import hudson.util.Secret;
import org.jenkinsci.plugins.electricflow.Configuration;
import org.jenkinsci.plugins.electricflow.ElectricFlowGlobalConfiguration;
import org.jvnet.hudson.test.JenkinsRule;

public class Utils {

  public static void createConfigurationInJenkinsRule(JenkinsRule jenkinsRule, String configName) {
    ElectricFlowGlobalConfiguration electricFlowGlobalConfiguration =
        (ElectricFlowGlobalConfiguration)
            jenkinsRule
                .getInstance()
                .getDescriptorByName(
                    "org.jenkinsci.plugins.electricflow.ElectricFlowGlobalConfiguration");

    Configuration configuration =
        new Configuration(
            configName, "localhost", "user", Secret.fromString("password"), "/rest/path", true, false, null, null);

    electricFlowGlobalConfiguration.getConfigurations().add(configuration);
    electricFlowGlobalConfiguration.save();
  }
}
