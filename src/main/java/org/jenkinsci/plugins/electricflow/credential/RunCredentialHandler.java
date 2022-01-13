package org.jenkinsci.plugins.electricflow.credential;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.model.Run;
import java.util.Collections;

public class RunCredentialHandler implements CredentialHandler {

  Run run;

  public RunCredentialHandler(Run run) {
    this.run = run;
  }

  @Override
  public StandardUsernamePasswordCredentials getStandardUsernamePasswordCredentialsById(
      String credentialsId) {
    if (credentialsId == null) {
      return null;
    }

    return CredentialsProvider.findCredentialById(
        credentialsId, StandardUsernamePasswordCredentials.class, run, Collections.emptyList());
  }
}
