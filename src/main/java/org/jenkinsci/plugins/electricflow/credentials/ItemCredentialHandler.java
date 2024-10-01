package org.jenkinsci.plugins.electricflow.credentials;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import hudson.model.Item;
import hudson.security.ACL;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

public class ItemCredentialHandler implements CredentialHandler {

    private Item item;

    public ItemCredentialHandler(Item item) {
        this.item = item;
    }

    @Override
    public StandardCredentials getStandardCredentialsById(String credentialsId) {
        if (credentialsId == null) {
            return null;
        }

        StandardCredentials credentials = CredentialsMatchers.firstOrNull(
                lookupCredentials(
                        StandardUsernamePasswordCredentials.class,
                        item,
                        ACL.SYSTEM,
                        new SchemeRequirement("http"),
                        new SchemeRequirement("https")),
                CredentialsMatchers.withId(credentialsId));

        if (credentials != null) {
            return credentials;
        }

        return CredentialsMatchers.firstOrNull(
                lookupCredentials(
                        StringCredentials.class,
                        item,
                        ACL.SYSTEM,
                        new SchemeRequirement("http"),
                        new SchemeRequirement("https")),
                CredentialsMatchers.withId(credentialsId));
    }
}
