// ElectricFlowSetJenkinsBuildDetails.java --
//
// ElectricFlowSetJenkinsBuildDetails.java is part of ElectricCommander.
//
// Copyright (c) 2005-2020 CloudBees.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import static org.jenkinsci.plugins.electricflow.Utils.formatJsonOutput;
import static org.jenkinsci.plugins.electricflow.Utils.getValidationComparisonHeaderRow;
import static org.jenkinsci.plugins.electricflow.Utils.getValidationComparisonRow;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.getSelectItemValue;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.isSelectItemValidationWrapper;

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
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.jenkinsci.plugins.electricflow.models.JenkinsBuildDetail;
import org.jenkinsci.plugins.electricflow.models.JenkinsBuildDetail.BuildTriggerSource;
import org.jenkinsci.plugins.electricflow.models.JenkinsBuildDetail.JenkinsBuildAssociationType;
import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

// import hudson.model.Run.ArtifactList

public class ElectricFlowAssociateBuildToRelease extends Recorder implements SimpleBuildStep {

  private static final Log log = LogFactory.getLog(ElectricFlowAssociateBuildToRelease.class);

  private String configuration;
  private Credential overrideCredential;
  private String projectName;
  private String releaseName;

  @DataBoundConstructor
  public ElectricFlowAssociateBuildToRelease() {
  }

  @Override
  public void perform(
      @Nonnull Run<?, ?> run,
      @Nonnull FilePath filePath,
      @Nonnull Launcher launcher,
      @Nonnull TaskListener taskListener) {

    try {
      CloudBeesFlowBuildData cloudBeesFlowBuildData = new CloudBeesFlowBuildData(run);
      EnvReplacer env = new EnvReplacer(run, taskListener);
      ElectricFlowClient efClient =
          ElectricFlowClientFactory.getElectricFlowClient(configuration, overrideCredential, env);
      PrintStream logger = taskListener.getLogger();

      // Calling the actual logic and saving the result
      JSONObject result = setJenkinsBuildDetails(efClient, cloudBeesFlowBuildData, logger);
      JSONObject resultBuildDetailInfo = result.getJSONObject("jenkinsBuildDetailInfo");


      // Setting the summary
      Release release = efClient.getRelease(configuration, projectName, releaseName);

      Map<String, String> args = new LinkedHashMap<>();
      args.put("releaseName", releaseName);
      args.put("buildName", cloudBeesFlowBuildData.getDisplayName());
      args.put("releaseId", resultBuildDetailInfo.getString("releaseId"));
      args.put("flowRuntimeId", release.getFlowRuntimeId());

      // Adding text to the summary page
      run.addAction(new SummaryTextAction(
          run, getSummaryHtml(efClient, args, logger)
      ));

      run.setResult(Result.SUCCESS);
      run.save();
    } catch (Exception ex) {
      taskListener.getLogger().println("Failed to associate build to release: " + ex.toString());
      ex.printStackTrace();
      run.setResult(Result.FAILURE);
    }
  }

  private JSONObject setJenkinsBuildDetails(
      ElectricFlowClient efClient,
      CloudBeesFlowBuildData cloudBeesFlowBuildData,
      PrintStream logger) throws IOException {

    logger.println("JENKINS VERSION: " + Jenkins.VERSION);
    logger.println("Project name: " + projectName + ", Release name: " + releaseName);

    JenkinsBuildDetail detail = new JenkinsBuildDetail()
        .setJenkinsData(cloudBeesFlowBuildData)
        .setProjectName(projectName)
        .setReleaseName(releaseName)
        .setAssociationType(JenkinsBuildAssociationType.ATTACHED)
        .setBuildTriggerSource(BuildTriggerSource.JENKINS);

    try {
      detail.validate();
    } catch (RuntimeException ex){
      logger.println("[ERROR] Can't fill the JenkinsBuildDetail: " + ex.getMessage());
      logger.println(Arrays.toString(ex.getStackTrace()));
    }

    logger.println("JSON: " + formatJsonOutput(detail.toJsonObject().toString()));

    logger.println("Preparing to attach build...");
    JSONObject result = efClient.setJenkinsBuildDetails(detail);

    logger.println("Create jenkinsBuildDetails result: " + formatJsonOutput(result.toString()));
    return result;
  }

  private String getSummaryHtml( ElectricFlowClient electricFlowClient,
      Map<String, String> args, PrintStream logger ) {

    String releaseName = args.get("releaseName");
    String releaseId = args.get("releaseId");
    String flowRuntimeId = args.get("flowRuntimeId");

    String path = String.format(
        "/flow/#pipeline-run/%s/%s/release/%s", releaseId, flowRuntimeId, releaseId
    );

    String releaseRunLink = electricFlowClient.getElectricFlowUrl() + path;
    logger.println(String.format("INFO: link to the release: %s", releaseRunLink));

    return "<h3>CloudBees Flow - Associate Build To Release</h3>"
        + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
        + "  <tr>\n"
        + "    <td>Build details were attached to the release </td>\n"
        + "    <td>"
        + "<a target='_blank' href='"
        + HtmlUtils.encodeForHtml(releaseRunLink)
        + "'>"
        + HtmlUtils.encodeForHtml(releaseName)
        + "</a>"
        + "</td>\n"
        + "  </tr>"
        + "</table>";
  }

  public String getConfiguration() {
    return configuration;
  }

  @DataBoundSetter
  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  public Credential getOverrideCredential() {
    return overrideCredential;
  }

  @DataBoundSetter
  public void setOverrideCredential(Credential overrideCredential) {
    this.overrideCredential = overrideCredential;
  }

  public String getStoredConfiguration() {
    return configuration;
  }

  public String getProjectName() {
    return projectName;
  }

  @DataBoundSetter
  public void setProjectName(String projectName) {
    this.projectName = getSelectItemValue(projectName);
  }

  public String getStoredProjectName() {
    return projectName;
  }

  public String getReleaseName() {
    return releaseName;
  }

  @DataBoundSetter
  public void setReleaseName(String releaseName) {
    this.releaseName = getSelectItemValue(releaseName);
  }

  public String getStoredReleaseName() {
    return releaseName;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  @Symbol("cloudBeesFlowAssociateBuildToRelease")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    public DescriptorImpl() {
      load();
    }

    // ~ Methods ------------------------------------------------------------

    public FormValidation doCheckConfiguration(
        @QueryParameter String value,
        @QueryParameter boolean validationTrigger,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      return Utils.validateConfiguration(value);
    }

    public FormValidation doCheckProjectName(
        @QueryParameter String value,
        @QueryParameter boolean validationTrigger,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      if (isSelectItemValidationWrapper(value)) {
        return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
      }
      return Utils.validateValueOnEmpty(value, "Project name");
    }

    public FormValidation doCheckReleaseName(
        @QueryParameter String value,
        @QueryParameter boolean validationTrigger,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      if (isSelectItemValidationWrapper(value)) {
        return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
      }
      return Utils.validateValueOnEmpty(value, "Release name");
    }

    public FormValidation doCheckParameters(
        @QueryParameter String value,
        @QueryParameter boolean validationTrigger,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      if (isSelectItemValidationWrapper(value)) {
        return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
      }
      return FormValidation.ok();
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

    public ListBoxModel doFillProjectNameItems(
        @QueryParameter String configuration, @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      return Utils.getProjects(configuration);
    }

    public ListBoxModel doFillReleaseNameItems(
        @QueryParameter String projectName,
        @QueryParameter String configuration,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      try {
        ListBoxModel m = new ListBoxModel();

        m.add("Select release", "");

        if (!configuration.isEmpty()
            && !projectName.isEmpty()
            && SelectFieldUtils.checkAllSelectItemsAreNotValidationWrappers(projectName)) {

          ElectricFlowClient client = new ElectricFlowClient(configuration);

          List<String> releasesList = client.getReleases(configuration, projectName);

          for (String release : releasesList) {
            m.add(release);
          }
        }

        return m;
      } catch (Exception e) {
        if (Utils.isEflowAvailable(configuration)) {
          log.error(
              "Error when fetching values for this parameter - release. Error message: "
                  + e.getMessage(),
              e);
          return SelectFieldUtils.getListBoxModelOnException("Select release");
        } else {
          return SelectFieldUtils.getListBoxModelOnWrongConf("Select release");
        }
      }
    }

    @Override
    public String getDisplayName() {
      return "CloudBees Flow - Associate Build To Release";
    }

    @Override
    public String getId() {
      return "electricFlowAssociateBuildToRelease";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    public FormValidation doShowOldValues(
        @QueryParameter("configuration") final String configuration,
        @QueryParameter("projectName") final String projectName,
        @QueryParameter("releaseName") final String releaseName,
        @QueryParameter("storedConfiguration") final String storedConfiguration,
        @QueryParameter("storedProjectName") final String storedProjectName,
        @QueryParameter("storedReleaseName") final String storedReleaseName,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      String configurationValue = configuration;
      String projectNameValue = getSelectItemValue(projectName);
      String releaseNameValue = getSelectItemValue(releaseName);

      String comparisonTable =
          "<table>"
              + getValidationComparisonHeaderRow()
              + getValidationComparisonRow("Configuration", storedConfiguration, configurationValue)
              + getValidationComparisonRow("Project Name", storedProjectName, projectNameValue)
              + getValidationComparisonRow("Release Name", storedReleaseName, releaseNameValue)
              + "</table>";

      if (configurationValue.equals(storedConfiguration)
          && projectNameValue.equals(storedProjectName)
          && releaseNameValue.equals(storedReleaseName)) {
        return FormValidation.okWithMarkup("No changes detected:<br>" + comparisonTable);
      } else {
        return FormValidation.warningWithMarkup("Changes detected:<br>" + comparisonTable);
      }
    }
  }
}
