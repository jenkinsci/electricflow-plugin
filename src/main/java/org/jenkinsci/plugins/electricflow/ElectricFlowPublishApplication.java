// ElectricFlowPublishApplication.java --
//
// ElectricFlowPublishApplication.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import static org.jenkinsci.plugins.electricflow.FileHelper.getPublishArtifactWorkspaceOnMaster;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.DirScanner;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.electricflow.exceptions.FlowRuntimeException;
import org.jenkinsci.plugins.electricflow.exceptions.PluginException;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs.CdJobStatus;
import org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs.GetJobStatusResponseData;
import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class ElectricFlowPublishApplication extends Recorder implements SimpleBuildStep {

  // ~ Static fields/initializers ---------------------------------------------

  public static final String deploymentPackageName = "deployment_package.zip";
  private static final Log log = LogFactory.getLog(ElectricFlowPublishApplication.class);
  private static List<File> zipFiles = new ArrayList<>();
  private static boolean isCutTopLevelDir;

  // ~ Instance fields --------------------------------------------------------

  private final String MANIFEST_NAME = "manifest.json";
  private final String configuration;
  private Credential overrideCredential;
  private RunAndWaitOption runAndWaitOption;
  private String filePath;

  // ~ Constructors -----------------------------------------------------------

  @DataBoundConstructor
  public ElectricFlowPublishApplication(String configuration, String filePath) {
    this.configuration = configuration;
    this.filePath = filePath;
  }

  // ~ Methods ----------------------------------------------------------------

  public static FilePath makeApplicationArchive(
      Run<?, ?> build, TaskListener listener, FilePath workspace, String filePath)
      throws IOException, InterruptedException, PluginException {
    FilePath source = new FilePath(workspace, filePath);
    String sourcePatternRelative = filePath;
    if (source.isDirectory()) {
      workspace = source;
      sourcePatternRelative = "**";
    }

    FilePath publishArtifactWorkspaceOnMaster = getPublishArtifactWorkspaceOnMaster(build);
    publishArtifactWorkspaceOnMaster.mkdirs();
    FilePath archiveFilePath =
        new FilePath(
            publishArtifactWorkspaceOnMaster, ElectricFlowPublishApplication.deploymentPackageName);

    listener
        .getLogger()
        .println(
            "Creating archive "
                + archiveFilePath.getRemote()
                + " from files and directories which match the following pattern "
                + sourcePatternRelative
                + " within workspace "
                + workspace.getRemote());

    int numberOfItemsArchived = 0;
    try (OutputStream outputStream = new FileOutputStream(archiveFilePath.getRemote())) {
      numberOfItemsArchived =
          workspace.zip(outputStream, new DirScanner.Glob(sourcePatternRelative, null));
    }

    if (numberOfItemsArchived < 1) {
      throw new PluginException(
          "No files or directories found during scan by the following pattern: "
              + sourcePatternRelative);
    }

    return archiveFilePath;
  }

  public static String getCurrentTimeStamp() {
    String dateFormat = "yyyy-MM-dd-HH-mm-ss.S";

    return new SimpleDateFormat(dateFormat).format(new Date());
  }

  private static void setZipFiles(List<File> fileList) {
    zipFiles.clear();
    zipFiles.addAll(fileList);
  }

  @Override
  public void perform(
      @Nonnull Run<?, ?> run,
      @Nonnull FilePath workspace,
      @Nonnull Launcher launcher,
      @Nonnull TaskListener taskListener) {
    Result result = runProcess(run, taskListener, workspace);
    if (result != Result.SUCCESS) {
      run.setResult(result);
    }
  }

  private Result runProcess(
      @Nonnull Run<?, ?> run, @Nonnull TaskListener taskListener, @Nonnull FilePath workspace) {
    PrintStream logger = taskListener.getLogger();

    @SuppressWarnings("WrapperTypeMayBePrimitive")
    Integer buildNumber = run.getNumber();

    // do replace
    String newFilePath;

    EnvReplacer env = null;

    try {
      env = new EnvReplacer(run, taskListener);

      newFilePath = env.expandEnv(filePath);
    } catch (IOException | InterruptedException e) {
      logger.println("Unexpected error during expand \"%s\"" + e);
      log.warn(e);
      newFilePath = filePath;
    }

    // artifact version
    String artifactVersion = buildNumber.toString();

    File archive = null;
    try {
      archive =
          new File(makeApplicationArchive(run, taskListener, workspace, newFilePath).getRemote());
    } catch (IOException | InterruptedException | PluginException e) {
      logger.println("Warning: Cannot create archive: " + e.getMessage());
      log.warn("Can't create archive: " + e.getMessage(), e);

      return Result.FAILURE;
    }

    String artifactGroup = "org.ec";
    String artifactKey = getCurrentTimeStamp();

    if (log.isDebugEnabled()) {
      log.debug("ArtifactKey" + artifactKey);
    }

    String artifactName = artifactGroup + ":" + artifactKey;
    String deployResponse;

    try {
      ElectricFlowClient efClient =
          ElectricFlowClientFactory.getElectricFlowClient(
              configuration, overrideCredential, run, env, false);
      List<File> fileList = new ArrayList<>();
      fileList.add(archive);

      efClient.uploadArtifact(
          fileList, archive.getParent(), "default", artifactName, artifactVersion, true);
      deployResponse =
          efClient.deployApplicationPackage(
              artifactGroup,
              artifactKey,
              artifactVersion,
              ElectricFlowPublishApplication.deploymentPackageName);
      logger.println(
          "Flow response on triggering CreateApplicationFromDeploymentPackage: " + deployResponse);

      String summaryHtml =
          getSummaryHtml(efClient, deployResponse, workspace.getRemote(), logger, null);
      SummaryTextAction action = new SummaryTextAction(run, summaryHtml);

      run.addAction(action);
      run.save();

      if (runAndWaitOption != null) {
        int checkInterval = runAndWaitOption.getCheckInterval();

        logger.println(
            "Waiting till CloudBees CD job is completed, checking every "
                + checkInterval
                + " seconds");

        String jobId = JSONObject.fromObject(deployResponse).getString("jobId");
        GetJobStatusResponseData responseData;
        do {
          TimeUnit.SECONDS.sleep(checkInterval);

          responseData = efClient.getCdJobStatus(jobId);
          logger.println(responseData);

          summaryHtml =
              getSummaryHtml(
                  efClient,
                  deployResponse,
                  workspace.getRemote(),
                  logger,
                  responseData);
          action = new SummaryTextAction(run, summaryHtml);
          run.addOrReplaceAction(action);
          run.save();

          if (responseData.getStatus() == CdJobStatus.unknown) {
            throw new PluginException("Unexpected format of CD job status response");
          }
        } while (responseData.getStatus() != CdJobStatus.completed);

        logger.println(
            "CD job completed with " + responseData.getOutcome() + " outcome");

        if (runAndWaitOption.isDependOnCdJobOutcome()) {
          Result ciBuildResult = Utils.getCorrespondedCiBuildResult(responseData.getOutcome());

          if (!ciBuildResult.equals(Result.SUCCESS) && runAndWaitOption.isThrowExceptionIfFailed()) {
            throw new FlowRuntimeException(responseData);
          }

          return ciBuildResult;
        }
      }
    } catch (PluginException
        | IOException
        | NoSuchAlgorithmException
        | InterruptedException
        | KeyManagementException e) {
      logger.println("Warning: Error occurred during application creation: " + e.getMessage());
      log.warn("Error occurred during application creation: " + e.getMessage(), e);

      return Result.FAILURE;
    }

    return Result.SUCCESS;
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

  public RunAndWaitOption getRunAndWaitOption() {
    return runAndWaitOption;
  }

  @DataBoundSetter
  public void setRunAndWaitOption(RunAndWaitOption runAndWaitOption) {
    this.runAndWaitOption = runAndWaitOption;
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

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  private String getSummaryHtml(
      ElectricFlowClient efClient,
      String deployResponse,
      String workspaceDir,
      PrintStream logger,
      GetJobStatusResponseData getJobStatusResponseData) {
    String url = efClient.getElectricFlowUrl() + "/flow/#applications";
    String jobId = JSONObject.fromObject(deployResponse).getString("jobId");
    String jobUrl = efClient.getElectricFlowUrl() + "/commander/link/jobDetails/jobs/" + jobId;
    String summaryText =
        "<h3>CloudBees CD Create/Deploy Application from Deployment Package</h3>"
            + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
            + "  <tr>\n"
            + "    <td>Application URL:</td>\n"
            + "    <td><a href='"
            + HtmlUtils.encodeForHtml(url)
            + "'>"
            + HtmlUtils.encodeForHtml(url)
            + "</a></td>   \n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>CloudBees CD Job:</td>\n"
            + "    <td><a href='"
            + HtmlUtils.encodeForHtml(jobUrl)
            + "'>link to createApplicationFromDeploymentPackage job</a></td>   \n"
            + "  </tr>\n";

    if (!zipFiles.isEmpty()) {
      StringBuilder strBuilder = new StringBuilder(summaryText);

      strBuilder.append(
          "  <tr>\n"
              + "    <td><b>Deployment Package Details:</b></td>\n"
              + "    <td></td>    \n"
              + "  </tr>\n");

      String jsonContent = "";

      for (File file : zipFiles) {
        String fileName = file.getPath();

        if (isCutTopLevelDir) {
          fileName = FileHelper.cutTopLevelDir(file.getPath());
        }

        if (fileName.endsWith(MANIFEST_NAME)) {
          String deploymentPathInWorkspace = "/";
          if (this.filePath != null && !this.filePath.equals("/")) {
            deploymentPathInWorkspace = this.filePath;
          }
          String manifestPath = FileHelper.buildPath(workspaceDir, deploymentPathInWorkspace, fileName);

          try {
            byte[] encoded = Files.readAllBytes(Paths.get(manifestPath));

            jsonContent = new String(encoded, StandardCharsets.UTF_8);
          } catch (IOException e) {
            logger.println("Warning: Error occurred during read manifest file. " + e.getMessage());
            log.warn(e.getMessage(), e);
          }

          continue;
        }

        strBuilder
            .append("  <tr>\n" + "    <td>&nbsp;&nbsp;&nbsp;&nbsp;")
            .append(HtmlUtils.encodeForHtml(fileName))
            .append("</td>\n" + "    <td>")
            .append("</td>    \n" + "  </tr>\n");
      }

      if (!jsonContent.isEmpty()) {
        strBuilder
            .append("  <tr>\n" + "    <td>&nbsp;&nbsp;&nbsp;&nbsp;")
            .append(HtmlUtils.encodeForHtml(MANIFEST_NAME))
            .append("</td>\n" + "    <td>")
            .append("<pre>")
            .append(HtmlUtils.encodeForHtml(jsonContent))
            .append("</pre>")
            .append("</td>    \n" + "  </tr>\n");
      }

      summaryText = strBuilder.toString();
    }
    if (getJobStatusResponseData != null) {
      summaryText =
          summaryText
              + "  <tr>\n"
              + "    <td>CD Job Status:</td>\n"
              + "    <td>\n"
              + HtmlUtils.encodeForHtml(getJobStatusResponseData.getStatus().name())
              + "    </td>\n"
              + "  </tr>\n";
      summaryText =
          summaryText
              + "  <tr>\n"
              + "    <td>CD Job Outcome:</td>\n"
              + "    <td>\n"
              + HtmlUtils.encodeForHtml(getJobStatusResponseData.getOutcome().name())
              + "    </td>\n"
              + "  </tr>\n";
    }

    summaryText = summaryText + "</table>";

    return summaryText;
  }

  // ~ Inner Classes ----------------------------------------------------------

  /**
   * Descriptor for {@link BuildStepDescriptor}. Used as a singleton. The class is marked as public
   * so that it can be accessed from views.
   *
   * <p>
   *
   * <p>See * .jelly for the actual HTML fragment for the configuration screen.
   */
  @Symbol("cloudBeesFlowCreateAndDeployAppFromJenkinsPackage")
  @Extension // This indicates to Jenkins that this is an implementation of
  // an extension point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    /**
     * In order to load the persisted global configuration, you have to call load() in the
     * constructor.
     */
    public DescriptorImpl() {
      load();
    }

    // ~ Methods ------------------------------------------------------------

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

    public ListBoxModel doFillConfigurationItems(@AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      return Utils.fillConfigurationItems();
    }

    public ListBoxModel doFillCredentialIdItems(@AncestorInPath Item item) {
      return Credential.DescriptorImpl.doFillCredentialIdItems(item);
    }

    /**
     * This human readable name is used in the configuration screen.
     *
     * @return this human readable name is used in the configuration screen.
     */
    @Override
    public String getDisplayName() {
      return "CloudBees CD - Create/Deploy Application from Deployment Package";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {

      // Indicates that this builder can be used with all kinds of
      // project types
      return true;
    }
  }
}
