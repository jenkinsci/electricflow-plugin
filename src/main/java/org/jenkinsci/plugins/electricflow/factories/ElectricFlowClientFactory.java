package org.jenkinsci.plugins.electricflow.factories;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
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
import org.jenkinsci.plugins.electricflow.exceptions.PluginException;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

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
    Configuration config = Utils.getConfigurationByName(configurationName);

    if (config == null) {
      throw new RuntimeException("Cannot find CloudBees CD configuration " + configurationName);
    }

    String electricFlowUrl = config.getElectricFlowUrl();
    boolean ignoreSslConnectionErrors = config.getIgnoreSslConnectionErrors();
    String electricFlowApiVersion = config.getElectricFlowApiVersion();
    String apiVersion = electricFlowApiVersion != null ? electricFlowApiVersion : "";

    if (overrideCredential == null) {
      if (config.getCredsType() != null && config.getCredsType().equals("storedCreds")) {
        StandardCredentials creds = credentialHandler.getStandardCredentialsById(config.getOverrideCredential().getCredentialId(envReplacer));
        if (creds == null) {
          throw new RuntimeException("Override credentials are not found by provided credential id");
        } else {
          return ElectricFlowClientFactory.getElectricFlowClient(electricFlowUrl, creds, apiVersion, ignoreSslConnectionErrors);
        }
      } else {
        String username = config.getElectricFlowUser();
        String password = config.getElectricFlowPassword().getPlainText();
        return new ElectricFlowClient(
                electricFlowUrl, username, password, apiVersion, ignoreSslConnectionErrors);
      }
    } else {
      StandardCredentials creds = credentialHandler.getStandardCredentialsById(overrideCredential.getCredentialId(envReplacer));
      if (creds == null) {
        throw new RuntimeException("Override credentials are not found by provided credential id");
      } else {
        return ElectricFlowClientFactory.getElectricFlowClient(electricFlowUrl, creds, apiVersion, ignoreSslConnectionErrors);
      }
    }
  }

  public static ElectricFlowClient getElectricFlowClient(
          String url,
          StandardCredentials creds,
          String apiVersion,
          boolean ignoreSslConnectionErrors)  {

    if (creds instanceof StringCredentials) {
      String secret = ((StringCredentials) creds).getSecret().getPlainText();
      return new ElectricFlowClient(
              url, secret, apiVersion, ignoreSslConnectionErrors);
    }

    if (creds instanceof UsernamePasswordCredentials) {
      String username = ((UsernamePasswordCredentials) creds).getUsername();
      String password = ((UsernamePasswordCredentials) creds).getPassword().getPlainText();
      return new ElectricFlowClient(
              url, username, password, apiVersion, ignoreSslConnectionErrors);
    }

    throw new RuntimeException("Unexpected type of creds");
  }
}

