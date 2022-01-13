package org.jenkinsci.plugins.electricflow.credential;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

public interface CredentialHandler {
  StandardUsernamePasswordCredentials getStandardUsernamePasswordCredentialsById(
      String credentialsId);
}
