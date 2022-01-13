package org.jenkinsci.plugins.electricflow.credential;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import hudson.model.Item;
import hudson.security.ACL;

public class ItemCredentialHandler implements CredentialHandler {

  private Item item;

  public ItemCredentialHandler(Item item) {
    this.item = item;
  }

  @Override
  public StandardUsernamePasswordCredentials getStandardUsernamePasswordCredentialsById(
      String credentialsId) {
    if (credentialsId == null) {
      return null;
    }

    return CredentialsMatchers.firstOrNull(
        lookupCredentials(
            StandardUsernamePasswordCredentials.class,
            item,
            ACL.SYSTEM,
            new SchemeRequirement("http"),
            new SchemeRequirement("https")),
        CredentialsMatchers.withId(credentialsId));
  }
}
