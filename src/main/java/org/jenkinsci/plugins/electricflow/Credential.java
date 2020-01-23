package org.jenkinsci.plugins.electricflow;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.electricflow.models.CredentialOption;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Collections;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

public class Credential
        extends AbstractDescribableImpl<Credential> {

    private final CredentialOption credentialOption;
    private final String username;
    private final Secret password;
    private final String credentialIdUsernameAndPassword;

    @DataBoundConstructor
    public Credential(CredentialOption credentialOption, String username, Secret password, String credentialIdUsernameAndPassword) {
        this.credentialOption = credentialOption;
        this.username = username;
        this.password = password;
        this.credentialIdUsernameAndPassword = credentialIdUsernameAndPassword;
    }

    public CredentialOption getCredentialOption() {
        return credentialOption;
    }

    public String getUsername() {
        return username;
    }

    public Secret getPassword() {
        return password;
    }

    public String getCredentialIdUsernameAndPassword(EnvReplacer envReplacer) {
        return envReplacer == null ? credentialIdUsernameAndPassword : envReplacer.expandEnv(credentialIdUsernameAndPassword);
    }

    public StandardUsernamePasswordCredentials getUsernamePasswordBasedOnCredentialId(EnvReplacer envReplacer) {
        return getStandardUsernamePasswordCredentialsById(getCredentialIdUsernameAndPassword(envReplacer));
    }

    public boolean isCredentialOption(String credentialOptionStr) {
        return this.credentialOption == CredentialOption.valueOf(credentialOptionStr);
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<Credential> {

        @Override
        public String getDisplayName() {
            return "Credential";
        }

        public static ListBoxModel doFillCredentialIdUsernameAndPasswordItems(Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }

            return new StandardUsernameListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM,
                            Jenkins.get(),
                            StandardUsernamePasswordCredentials.class,
                            Collections.<DomainRequirement>emptyList(),
                            CredentialsMatchers.always()
                    );
        }
    }

    private static StandardUsernamePasswordCredentials getStandardUsernamePasswordCredentialsById(String credentialsId) {
        if (credentialsId == null) {
            return null;
        }

        return CredentialsMatchers.firstOrNull(
                lookupCredentials(StandardUsernamePasswordCredentials.class,
                        Jenkins.getInstanceOrNull(),
                        ACL.SYSTEM,
                        new SchemeRequirement("http"),
                        new SchemeRequirement("https")),
                CredentialsMatchers.withId(credentialsId)
        );
    }
}
