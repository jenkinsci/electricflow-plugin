package org.jenkinsci.plugins.electricflow.factories;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.model.Item;
import hudson.model.Run;
import org.jenkinsci.plugins.electricflow.Configuration;
import org.jenkinsci.plugins.electricflow.Credential;
import org.jenkinsci.plugins.electricflow.ElectricFlowClient;
import org.jenkinsci.plugins.electricflow.EnvReplacer;
import org.jenkinsci.plugins.electricflow.Utils;
import org.jenkinsci.plugins.electricflow.credentials.CredentialHandler;
import org.jenkinsci.plugins.electricflow.credentials.ItemCredentialHandler;
import org.jenkinsci.plugins.electricflow.credentials.RunCredentialHandler;

public class ElectricFlowClientFactory {

  public static ElectricFlowClient getElectricFlowClient(
      String configurationName, Credential overrideCredential, Run run, EnvReplacer envReplacer) {
    return getElectricFlowClient(
        configurationName, overrideCredential, new RunCredentialHandler(run), envReplacer);
  }

  public static ElectricFlowClient getElectricFlowClient(
      String configurationName, Credential overrideCredential, Item item, EnvReplacer envReplacer) {
    return getElectricFlowClient(
        configurationName, overrideCredential, new ItemCredentialHandler(item), envReplacer);
  }

  public static ElectricFlowClient getElectricFlowClient(
      String configurationName,
      Credential overrideCredential,
      CredentialHandler credentialHandler,
      EnvReplacer envReplacer) {
    Configuration cred = Utils.getConfigurationByName(configurationName);

    if (cred == null) {
      throw new RuntimeException("Cannot find CloudBees CD configuration " + configurationName);
    }

    String electricFlowUrl = cred.getElectricFlowUrl();
    boolean ignoreSslConnectionErrors = cred.getIgnoreSslConnectionErrors();
    String electricFlowApiVersion = cred.getElectricFlowApiVersion();
    String apiVersion = electricFlowApiVersion != null ? electricFlowApiVersion : "";

    String username;
    String password;
    if (overrideCredential == null) {
      username = cred.getElectricFlowUser();
      password = cred.getElectricFlowPassword().getPlainText();
    } else {
      String credentialIdResolved = overrideCredential.getCredentialId(envReplacer);
      StandardUsernamePasswordCredentials creds =
          credentialHandler.getStandardUsernamePasswordCredentialsById(credentialIdResolved);
      if (creds == null) {
        throw new RuntimeException("Override credentials are not found by provided credential id");
      } else {
        username = creds.getUsername();
        password = creds.getPassword().getPlainText();
      }
    }

    return new ElectricFlowClient(
        electricFlowUrl, username, password, apiVersion, ignoreSslConnectionErrors);
  }
}
