package org.jenkinsci.plugins.electricflow.credentials;

import com.cloudbees.plugins.credentials.common.StandardCredentials;

public interface CredentialHandler {
  StandardCredentials getStandardCredentialsById(
      String credentialsId);
}
