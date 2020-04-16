package org.jenkinsci.plugins.electricflow.factories;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.model.Run;
import hudson.util.Secret;
import org.jenkinsci.plugins.electricflow.*;

public class ElectricFlowClientFactory {

    public static ElectricFlowClient getElectricFlowClient(
            String configurationName,
            Credential overrideCredential,
            EnvReplacer envReplacer) {
        return getElectricFlowClient(
                configurationName,
                overrideCredential,
                envReplacer,
                false
        );
    }

    public static ElectricFlowClient getElectricFlowClient(
            String configurationName,
            Credential overrideCredential,
            EnvReplacer envReplacer,
            boolean ignoreUnresolvedOverrideCredential) {
        return getElectricFlowClient(
                configurationName,
                overrideCredential,
                null,
                envReplacer,
                ignoreUnresolvedOverrideCredential
        );
    }

    public static ElectricFlowClient getElectricFlowClient(
            String configurationName,
            Credential overrideCredential,
            Run run,
            EnvReplacer envReplacer,
            boolean ignoreUnresolvedOverrideCredential) {
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
        } else {
            StandardUsernamePasswordCredentials creds = overrideCredential.getUsernamePasswordBasedOnCredentialId(envReplacer, run);
            if (creds == null) {
                if (ignoreUnresolvedOverrideCredential) {
                    username = cred.getElectricFlowUser();
                    password = Secret.fromString(cred.getElectricFlowPassword())
                            .getPlainText();
                } else {
                    throw new RuntimeException("Override credentials are not found by provided credential id");
                }
            } else {
                username = creds.getUsername();
                password = creds.getPassword().getPlainText();
            }
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
