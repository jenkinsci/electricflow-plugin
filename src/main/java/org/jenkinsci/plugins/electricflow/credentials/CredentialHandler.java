package org.jenkinsci.plugins.electricflow.credentials;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

public interface CredentialHandler {
  StandardUsernamePasswordCredentials getStandardUsernamePasswordCredentialsById(
      String credentialsId);
}
