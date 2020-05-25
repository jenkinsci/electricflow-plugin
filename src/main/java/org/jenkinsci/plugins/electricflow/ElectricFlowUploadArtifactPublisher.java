// ElectricFlowUploadArtifactPublisher.java --
//
// ElectricFlowUploadArtifactPublisher.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.RelativePath;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.io.PrintStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.electricflow.extension.ArtifactUploadData;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class ElectricFlowUploadArtifactPublisher extends Recorder implements SimpleBuildStep {

  // ~ Static fields/initializers ---------------------------------------------

  public static final String FLOW_ARTIFACT_REPOSITORY = "Flow Artifact Repository";
  private static final Log log = LogFactory.getLog(ElectricFlowUploadArtifactPublisher.class);

  // ~ Instance fields --------------------------------------------------------
  private final String configuration;
  private final String repositoryName;
  private Credential overrideCredential;
  private String artifactName;
  private String artifactVersion;
  private String filePath;

  // ~ Constructors -----------------------------------------------------------

  // Fields in config.jelly must match the parameter names in the
  // "DataBoundConstructor"
  @DataBoundConstructor
  public ElectricFlowUploadArtifactPublisher(
      String repositoryName,
      String artifactName,
      String artifactVersion,
      String filePath,
      String configuration) {
    this.repositoryName = repositoryName;
    this.artifactName = artifactName;
    this.artifactVersion = artifactVersion;
    this.filePath = filePath;
    this.configuration = configuration;
  }

  @Override
  public void perform(
      @Nonnull Run<?, ?> run,
      @Nonnull FilePath workspace,
      @Nonnull Launcher launcher,
      @Nonnull TaskListener taskListener)
      throws InterruptedException, IOException {
    boolean isSuccess = runProcess(run, taskListener, workspace);
    if (!isSuccess) {
      run.setResult(Result.FAILURE);
    }
  }

  private boolean runProcess(
      @Nonnull Run<?, ?> run, @Nonnull TaskListener taskListener, @Nonnull FilePath workspace) {
    PrintStream logger = Utils.getLogger(null, taskListener);

    try {

      if (log.isDebugEnabled()) {
        log.debug("Publishing artifact...");
      }

      if (log.isDebugEnabled()) {
        log.debug("Workspace directory: " + workspace);
      }

      // Expanding the variables
      EnvReplacer env = new EnvReplacer(run, taskListener);
      String newFilePath = env.expandEnv(filePath);
      String newArtifactVersion = env.expandEnv(artifactVersion);
      String newArtifactName = env.expandEnv(artifactName);
      String artifactVersionName = newArtifactName + ":" + newArtifactVersion;

      if (log.isDebugEnabled()) {
        log.debug("Workspace directory: " + newFilePath);
      }

      // Uploading artifact
      ElectricFlowClient efClient =
          ElectricFlowClientFactory.getElectricFlowClient(
              configuration, overrideCredential, run, env, false);
      String result =
          efClient.uploadArtifact(
              run,
              taskListener,
              repositoryName,
              newArtifactName,
              newArtifactVersion,
              newFilePath,
              true,
              workspace);

      if (!"Artifact-Published-OK".equals(result)) {
        logger.println("Upload result: " + result);

        return false;
      }

      String efArtifactUrl =
          efClient.getElectricFlowUrl()
              + "/commander/link/artifactVersionDetails/artifactVersions/"
              + Utils.encodeURL(newArtifactName + ":" + newArtifactVersion)
              + "?s=Artifacts&ss=Artifacts";

      String repository = repositoryName.isEmpty() ? "default" : repositoryName;

      String summaryHtml = getSummaryHtml(newArtifactVersion, repository, efArtifactUrl);

      ArtifactUploadSummaryTextAction action =
          new ArtifactUploadSummaryTextAction(run, summaryHtml);

      ArtifactUploadData artifactUploadData = new ArtifactUploadData();
      artifactUploadData.setArtifactName(newArtifactName);
      artifactUploadData.setArtifactUrl(efArtifactUrl);
      artifactUploadData.setArtifactVersion(newArtifactVersion);
      artifactUploadData.setArtifactVersionName(artifactVersionName);
      artifactUploadData.setRepositoryName(repository);
      artifactUploadData.setRepositoryType(FLOW_ARTIFACT_REPOSITORY);
      artifactUploadData.setFilePath(newFilePath);

      action.setArtifactUploadData(artifactUploadData);

      logger.println("++++++++++++++++++++++++++++++++++++++++++++");
      logger.println("Artifact Name: " + artifactUploadData.getArtifactName());
      logger.println("Artifact Version: " + artifactUploadData.getArtifactVersion());
      logger.println("Artifact Version Name: " + artifactUploadData.getArtifactVersionName());
      logger.println("Artifact Url: " + artifactUploadData.getArtifactUrl());
      logger.println("Repository Name: " + artifactUploadData.getRepositoryName());
      logger.println("Repository Type: " + artifactUploadData.getRepositoryType());
      logger.println("File path: " + artifactUploadData.getFilePath());
      logger.println("++++++++++++++++++++++++++++++++++++++++++++");

      run.addAction(action);
      run.save();
      logger.println("Upload result: " + result);
    } catch (NoSuchAlgorithmException
        | KeyManagementException
        | InterruptedException
        | IOException e) {
      logger.println(e.getMessage());
      log.error(e.getMessage(), e);

      return false;
    }

    return true;
  }

  public String getArtifactName() {
    return artifactName;
  }

  public String getArtifactVersion() {
    return artifactVersion;
  }

  /**
   * We'll use this from the {@code config.jelly}.
   *
   * @return we'll use this from the {@code config.jelly}.
   */
  public String getConfiguration() {
    return configuration;
  }

  public Credential getOverrideCredential() {
    return overrideCredential;
  }

  @DataBoundSetter
  public void setOverrideCredential(Credential overrideCredential) {
    this.overrideCredential = overrideCredential;
  }

  // Overridden for better type safety.
  // If your plugin doesn't really define any property on Descriptor,
  // you don't have to do this.
  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public String getFilePath() {
    return filePath;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  private String getSummaryHtml(String newArtifactVersion, String repository, String efUrl) {

    return "<h3>CloudBees CD Publish Artifact</h3>"
        + "<table cellspacing=\"2\" cellpadding=\"4\">\n"
        + "  <tr>\n"
        + "    <td>Artifact URL:</td>\n"
        + "    <td><a href ='"
        + HtmlUtils.encodeForHtml(efUrl)
        + "'>"
        + HtmlUtils.encodeForHtml(efUrl)
        + "</a></td> \n"
        + "  </tr>\n"
        + "  <tr>\n"
        + "    <td>Artifact Name:</td>\n"
        + "    <td><a href ='"
        + HtmlUtils.encodeForHtml(efUrl)
        + "'>"
        + HtmlUtils.encodeForHtml(artifactName)
        + "</a></td> \n"
        + "  </tr>\n"
        + "  <tr>\n"
        + "    <td>Artifact Version:</td>\n"
        + "    <td>"
        + HtmlUtils.encodeForHtml(newArtifactVersion)
        + "</td> \n"
        + "  </tr>\n"
        + "  <tr>\n"
        + "    <td>Repository Name:</td>\n"
        + "    <td>"
        + HtmlUtils.encodeForHtml(repository)
        + "</td> \n"
        + "  </tr>\n"
        + "</table>";
  }

  // ~ Inner Classes ----------------------------------------------------------

  /** The class is marked as public so that it can be accessed from views. */
  @Symbol("cloudBeesFlowPublishArtifact")
  @Extension // This indicates to Jenkins that this is an implementation of
  // an extension point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    // ~ Static fields/initializers -----------------------------------------

    private static final Log log = LogFactory.getLog(DescriptorImpl.class);

    // ~ Instance fields ----------------------------------------------------

    /**
     * To persist global configuration information, simply store it in a field and call save().
     *
     * <p>If you don't want fields to be persisted, use {@code transient}.
     */
    private String electricFlowUrl;

    private String electricFlowUser;
    private String electricFlowPassword;

    // ~ Constructors -------------------------------------------------------

    /**
     * In order to load the persisted global configuration, you have to call load() in the
     * constructor.
     */
    public DescriptorImpl() {
      load();
    }

    // ~ Methods ------------------------------------------------------------

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      electricFlowUrl = formData.getString("electricFlowUrl");
      electricFlowUser = formData.getString("electricFlowUser");
      electricFlowPassword = formData.getString("electricFlowPassword");

      save();

      return super.configure(req, formData);
    }

    public FormValidation doCheckArtifactName(
        @QueryParameter String value, @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      return Utils.validateValueOnEmpty(value, "Artifact name");
    }

    public FormValidation doCheckArtifactVersion(
        @QueryParameter String value, @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      return Utils.validateValueOnEmpty(value, "Artifact version");
    }

    public FormValidation doCheckConfiguration(
        @QueryParameter String value, @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      return Utils.validateValueOnEmpty(value, "Configuration");
    }

    public FormValidation doCheckFilePath(@QueryParameter String value, @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      return Utils.validateValueOnEmpty(value, "File path");
    }

    public FormValidation doCheckRepositoryName(
        @QueryParameter String value, @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      return Utils.validateValueOnEmpty(value, "Repository name");
    }

    public ListBoxModel doFillConfigurationItems(@AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      return Utils.fillConfigurationItems();
    }

    public ListBoxModel doFillCredentialIdItems(@AncestorInPath Item item) {
      return Credential.DescriptorImpl.doFillCredentialIdItems(item);
    }

    public ListBoxModel doFillRepositoryNameItems(
        @QueryParameter String configuration,
        @QueryParameter boolean overrideCredential,
        @QueryParameter @RelativePath("overrideCredential") String credentialId,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      ListBoxModel m = new ListBoxModel();

      m.add("Select repository", "");

      if (configuration.isEmpty()) {
        return m;
      }

      try {
        Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
        ElectricFlowClient efClient =
            ElectricFlowClientFactory.getElectricFlowClient(
                configuration, overrideCredentialObj, null, true);
        List<String> repositories;

        repositories = efClient.getArtifactRepositories();

        for (String repo : repositories) {
          m.add(repo, repo);
        }
      } catch (Exception e) {
        log.warn("Error retrieving repository list: " + e.getMessage(), e);

        return m;
      }

      return m;
    }

    public Configuration getConfigurationByName(String name) {
      return Utils.getConfigurationByName(name);
    }

    /**
     * This human readable name is used in the configuration screen.
     *
     * @return this human readable name is used in the configuration screen.
     */
    @Override
    public String getDisplayName() {
      return "CloudBees CD - Publish Artifact";
    }

    public String getElectricFlowPassword() {
      return electricFlowPassword;
    }

    public String getElectricFlowUrl() {
      return electricFlowUrl;
    }

    public String getElectricFlowUser() {
      return electricFlowUser;
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {

      // Indicates that this builder can be used with all kinds of
      // project types
      return true;
    }
  }
}