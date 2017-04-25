
// ElectricFlowUploadArtifactPublisher.java --
//
// ElectricFlowUploadArtifactPublisher.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflowintegration;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

/**
 * Sample {@link Builder}.
 *
 * <p>When the user configures the project and enables this builder, {@link
 * DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new {@link
 * ElectricFlowUploadArtifactPublisher} is created. The created instance is
 * persisted to the project configuration XML by using XStream, so this allows
 * you to use instance fields to remember the configuration.</p>
 *
 * <p>When a build is performed, the {@link #perform} method will be invoked.
 * </p>
 */
public class ElectricFlowUploadArtifactPublisher
    extends Publisher
{

    //~ Static fields/initializers ---------------------------------------------

    private static final Log log = LogFactory.getLog(
            ElectricFlowUploadArtifactPublisher.class);

    //~ Instance fields --------------------------------------------------------

    private final String credential;
    private final String repositoryName;
    private String       artifactName;
    private String       artifactVersion;
    private String       filePath;

    //~ Constructors -----------------------------------------------------------

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor public ElectricFlowUploadArtifactPublisher(
            String repositoryName,
            String artifactName,
            String artifactVersion,
            String filePath,
            String credential)
    {
        this.repositoryName  = repositoryName;
        this.artifactName    = artifactName;
        this.artifactVersion = artifactVersion;
        this.filePath        = filePath;
        this.credential      = credential;
    }

    //~ Methods ----------------------------------------------------------------

    @Override public boolean perform(
            AbstractBuild build,
            Launcher      launcher,
            BuildListener listener)
    {

        try {

            if (log.isDebugEnabled()) {
                log.debug("Publishing artifact...");
            }

            String   workspaceDir;
            FilePath workspace    = build.getWorkspace();

            if (workspace != null) {
                workspaceDir = workspace.getRemote();
            }
            else {
                log.warn("Workspace should not be null");

                return false;
            }

            if (log.isDebugEnabled()) {
                log.debug("Workspace directory: " + workspaceDir);
            }

            Integer buildNumber = build.getNumber();

            // let's do a replacements
            // $WORKSPACE_DIR will be replaced with current workspace path.
            // $BUILD_NUMBER will be replaced with current build number.
            String newFilePath        = filePath;
            String newArtifactVersion = artifactVersion;
            String newArtifactName    = artifactName;

            newFilePath        = newFilePath.replace("$BUILD_NUMBER", buildNumber.toString());
            newArtifactVersion = newArtifactVersion.replace("$BUILD_NUMBER", buildNumber.toString());
            newArtifactName    = newArtifactName.replace("$BUILD_NUMBER", buildNumber.toString());

            if (log.isDebugEnabled()) {
                log.debug("Workspace directory: " + newFilePath);
            }

            // end of replacements
            ElectricFlowConfigurationManager efCM            =
                new ElectricFlowConfigurationManager();
            Configuration                    cred            =
                efCM.getCredentialByName(this.credential);
            String                           electricFlowUrl =
                cred.getElectricFlowUrl();
            String                           userName        =
                cred.getElectricFlowUser();
            String                           userPassword    =
                cred.getElectricFlowPassword();
            ElectricFlowClient               efClient        =
                new ElectricFlowClient(electricFlowUrl, userName, userPassword, workspaceDir);
            String                           result          =
                efClient.uploadArtifact(repositoryName, newArtifactName, newArtifactVersion, newFilePath, false);

            listener.getLogger()
                    .println("Upload result: " + result);
        }
        catch (NoSuchAlgorithmException | KeyManagementException | IOException e) {
            listener.getLogger()
                    .println(e.getMessage());
            log.error(e.getMessage(), e);

            return false;
        }

        return true;
    }

    public String getArtifactName()
    {
        return artifactName;
    }

    public String getArtifactVersion()
    {
        return artifactVersion;
    }

    /**
     * We'll use this from the {@code config.jelly}.
     *
     * @return  we'll use this from the {@code config.jelly}.
     */
    public String getCredential()
    {
        return credential;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getFilePath()
    {
        return filePath;
    }

    @Override public Action getProjectAction(AbstractProject<?, ?> project)
    {
        return new ElectricFlowProjectAction(project);
    }

    public String getRepositoryName()
    {
        return repositoryName;
    }

    @Override public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.NONE;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of
               // an extension point.
    public static final class DescriptorImpl
        extends BuildStepDescriptor<Publisher>
    {

        //~ Static fields/initializers -----------------------------------------

        private static final Log log = LogFactory.getLog(DescriptorImpl.class);

        //~ Instance fields ----------------------------------------------------

        /**
         * To persist global configuration information, simply store it in a
         * field and call save().
         *
         * <p>If you don't want fields to be persisted, use {@code transient}.
         * </p>
         */
        private String electricFlowUrl;
        private String electricFlowUser;
        private String electricFlowPassword;

        //~ Constructors -------------------------------------------------------

        /**
         * In order to load the persisted global configuration, you have to call
         * load() in the constructor.
         */
        public DescriptorImpl()
        {
            load();
        }

        //~ Methods ------------------------------------------------------------

        @Override public boolean configure(
                StaplerRequest req,
                JSONObject     formData)
            throws FormException
        {

            // TODO ask Dmitriy To persist global configuration information, set
            // that to properties and call save(). useFrench =
            // formData.getBoolean("useFrench"); ^Can also use
            // req.bindJSON(this, formData); (easier when there are many fields;
            // need set* methods for this, like setUseFrench)
            electricFlowUrl      = formData.getString("electricFlowUrl");
            electricFlowUser     = formData.getString("electricFlowUser");
            electricFlowPassword = formData.getString("electricFlowPassword");

            save();

            return super.configure(req, formData);
        }

        public ListBoxModel doFillCredentialItems()
        {
            return Utils.fillCredentialItems();
        }


        public FormValidation doCheckCredential(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Credential");
        }

        public FormValidation doCheckFilePath(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "File path");
        }

        public FormValidation doCheckArtifactName(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Artifact name");
        }

        public FormValidation doCheckArtifactVersion(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Artifact version");
        }

        public FormValidation doCheckRepositoryName(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Repository name");
        }


        public ListBoxModel doFillRepositoryNameItems(
                @QueryParameter String credential)
        {
            ListBoxModel m = new ListBoxModel();

            m.add("Select repository", "");

            if (credential.isEmpty()) {
                return m;
            }

            try {
                ElectricFlowConfigurationManager efCM         =
                    new ElectricFlowConfigurationManager();
                Configuration                    cred         =
                    efCM.getCredentialByName(credential);
                ElectricFlowClient               efClient     =
                    new ElectricFlowClient(cred.getElectricFlowUrl(),
                        cred.getElectricFlowUser(),
                        cred.getElectricFlowPassword());
                List<String>                     repositories;

                repositories = efClient.getArtifactRepositories();

                for (String repo : repositories) {
                    m.add(repo, repo);
                }
            }
            catch (Exception e) {
                log.warn("Error retrieving repository list: "
                        + e.getMessage(), e);

                return m;
            }

            return m;
        }

        public Configuration getCredentialByName(String name)
        {
            return Utils.getCredentialByName(name);
        }

        /**
         * This human readable name is used in the configuration screen.
         *
         * @return  this human readable name is used in the configuration
         *          screen.
         */
        @Override public String getDisplayName()
        {
            return "ElectricFlow - Publish Artifact";
        }

        public String getElectricFlowPassword()
        {
            return electricFlowPassword;
        }

        /**
         * This method returns true if the global configuration says we should
         * speak French.
         *
         * <p>The method name is bit awkward because global.jelly calls this
         * method to determine the initial state of the checkbox by the naming
         * convention.</p>
         *
         * @return  this method returns true if the global configuration says we
         *          should speak French.
         */
        public String getElectricFlowUrl()
        {
            return electricFlowUrl;
        }

        public String getElectricFlowUser()
        {
            return electricFlowUser;
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param   aClass  value This parameter receives the value that the
         *                  user has typed.
         *
         * @return  Indicates the outcome of the validation. This is sent to the
         *          browser.
         *
         *          <p>Note that returning {@link FormValidation#error(String)}
         *          does not prevent the form from being saved. It just means
         *          that a message will be displayed to the user.</p>
         */
        @Override public boolean isApplicable(
                Class<? extends AbstractProject> aClass)
        {

            // Indicates that this builder can be used with all kinds of
            // project types
            return true;
        }
    }
}
