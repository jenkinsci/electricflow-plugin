package org.jenkinsci.plugins.electricflow.credentials;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.model.Run;
import java.util.Collections;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

public class RunCredentialHandler implements CredentialHandler {

    private Run run;

    public RunCredentialHandler(Run run) {
        this.run = run;
    }

    @Override
    public StandardCredentials getStandardCredentialsById(String credentialsId) {
        if (credentialsId == null) {
            return null;
        }

        StandardCredentials credentials = CredentialsProvider.findCredentialById(
                credentialsId, StandardUsernamePasswordCredentials.class, run, Collections.emptyList());

        if (credentials != null) {
            return credentials;
        }

        return CredentialsProvider.findCredentialById(
                credentialsId, StringCredentials.class, run, Collections.emptyList());
    }
}
