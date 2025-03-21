package org.jenkinsci.plugins.electricflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.FreeStyleProject;
import hudson.util.ListBoxModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CredentialsTest {

    private static JenkinsRule jenkinsRule;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
    }

    @Test
    void testFillCredentialsItems() throws Exception {

        // When no credentials, assert that there is at least one value, the empty value
        Folder folder = jenkinsRule.jenkins.createProject(Folder.class, "folder");
        FreeStyleProject job = folder.createProject(FreeStyleProject.class, "freestyle");

        assertThat(Credential.DescriptorImpl.doFillCredentialIdItems(null), hasSize(1));
        assertThat(Credential.DescriptorImpl.doFillCredentialIdItems(folder), hasSize(1));
        assertThat(Credential.DescriptorImpl.doFillCredentialIdItems(job), hasSize(1));

        // System Credentials
        String systemCredId = "screds";
        SystemCredentialsProvider.getInstance()
                .getCredentials()
                .add(new UsernamePasswordCredentialsImpl(
                        CredentialsScope.SYSTEM, systemCredId, systemCredId, "user", "password"));
        // Root credentials
        String globalCredId = "gcreds";
        SystemCredentialsProvider.getInstance()
                .getCredentials()
                .add(new UsernamePasswordCredentialsImpl(
                        CredentialsScope.GLOBAL, globalCredId, globalCredId, "user", "password"));
        // Folder credentials
        Iterable<CredentialsStore> stores = CredentialsProvider.lookupStores(folder);
        CredentialsStore folderStore = null;
        for (CredentialsStore s : stores) {
            if (s.getProvider() instanceof FolderCredentialsProvider && s.getContext() == folder) {
                folderStore = s;
                break;
            }
        }
        assert folderStore != null;
        String folderCredId = "fcreds";
        StandardUsernamePasswordCredentials folderCred = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, folderCredId, folderCredId, "user", "password");
        folderStore.addCredentials(Domain.global(), folderCred);
        folderStore.save();

        // From Root context
        // Assert we can see root credentials and system scoped, but not folder credentials
        ListBoxModel options = Credential.DescriptorImpl.doFillCredentialIdItems(null);
        assertThat(options, hasSize(3));
        assertThat(
                options.stream().map(option -> option.value).toList(),
                containsInAnyOrder("", globalCredId, systemCredId));

        // From Folder context
        // Assert we can see root credentials and folder credentials, but not system scoped
        options = Credential.DescriptorImpl.doFillCredentialIdItems(folder);
        assertThat(options, hasSize(3));
        assertThat(
                options.stream().map(option -> option.value).toList(),
                containsInAnyOrder("", globalCredId, folderCredId));

        // From Item context
        // Assert we can see root credentials and folder credentials, but not system scoped
        options = Credential.DescriptorImpl.doFillCredentialIdItems(job);
        assertThat(options, hasSize(3));
        assertThat(
                options.stream().map(option -> option.value).toList(),
                containsInAnyOrder("", globalCredId, folderCredId));
    }
}
