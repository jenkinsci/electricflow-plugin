package org.jenkinsci.plugins.electricflow.factories;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.util.Secret;
import org.jenkinsci.plugins.electricflow.Configuration;
import org.jenkinsci.plugins.electricflow.Credential;
import org.jenkinsci.plugins.electricflow.ElectricFlowClient;
import org.jenkinsci.plugins.electricflow.Utils;
import org.jenkinsci.plugins.electricflow.models.CredentialOption;

public class ElectricFlowClientFactory {
    public static ElectricFlowClient getElectricFlowClient(
            String configurationName,
            Credential overrideCredential) {
        Configuration cred = Utils.getConfigurationByName(configurationName);

        if (cred == null) {
            throw new RuntimeException("Cannot find CloudBees Flow configuration " + configurationName);
        }

        String electricFlowUrl = cred.getElectricFlowUrl();
        boolean ignoreSslConnectionErrors = cred.getIgnoreSslConnectionErrors();
        String electricFlowApiVersion = cred.getElectricFlowApiVersion();
        String apiVersion = electricFlowApiVersion != null
                ? electricFlowApiVersion
                : "";

        String username;
        String password;
        if (overrideCredential == null) {
            username = cred.getElectricFlowUser();
            password = Secret.fromString(cred.getElectricFlowPassword())
                    .getPlainText();
        } else if (overrideCredential.getCredentialOption() == CredentialOption.userNameAndPassword) {
            username = overrideCredential.getUsername();
            password = overrideCredential.getPassword().getPlainText();
        } else {
            StandardUsernamePasswordCredentials creds = overrideCredential.getUsernamePasswordBasedOnCredentialId();
            if (creds == null) {
                throw new RuntimeException("Override credentials are not found by provided credential id");
            }
            username = creds.getUsername();
            password = creds.getPassword().getPlainText();
        }

        return new ElectricFlowClient(
                electricFlowUrl,
                username,
                password,
                apiVersion,
                ignoreSslConnectionErrors
        );

    }
}
