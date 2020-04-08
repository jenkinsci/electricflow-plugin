// ElectricFlowSetJenkinsBuildDetails.java --
//
// ElectricFlowSetJenkinsBuildDetails.java is part of ElectricCommander.
//
// Copyright (c) 2005-2020 CloudBees.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import static net.sf.json.JSONObject.fromObject;
import static org.jenkinsci.plugins.electricflow.Utils.expandParameters;
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
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

// import hudson.model.Run.ArtifactList

public class ElectricFlowSetJenkinsBuildDetails extends Recorder implements SimpleBuildStep {

  private static final Log log = LogFactory.getLog(ElectricFlowSetJenkinsBuildDetails.class);

  private String configuration;
  private Credential overrideCredential;
  private String projectName;
  private String releaseName;

  @DataBoundConstructor
  public ElectricFlowSetJenkinsBuildDetails() {
  }

  @Override
  public void perform(
      @Nonnull Run<?, ?> run,
      @Nonnull FilePath filePath,
      @Nonnull Launcher launcher,
      @Nonnull TaskListener taskListener) {

    boolean isSuccess = runProcedure(run, taskListener);

    if (!isSuccess) {
      run.setResult(Result.FAILURE);
    }
  }

  private boolean runProcedure(@Nonnull Run<?, ?> run, @Nonnull TaskListener taskListener) {
    PrintStream logger = taskListener.getLogger();

    CloudBeesFlowBuildData cloudBeesFlowBuildData = new CloudBeesFlowBuildData(run);
    JSONObject json = cloudBeesFlowBuildData.toJsonObject();

    logger.println("JSON: " + json.toString());
    logger.println("JENKINS VERSION: " + Jenkins.VERSION);
    logger.println("Project name: " + projectName + ", Release name: " + releaseName);

    throw new RuntimeException("Not implemented");

//    try {
//      logger.println("Preparing to run procedure...");
//
//      EnvReplacer env = new EnvReplacer(run, taskListener);
//      expandParameters(parameter, env, "value");
//
//      ElectricFlowClient efClient =
//          ElectricFlowClientFactory.getElectricFlowClient(configuration, overrideCredential, env);
//
//      String result = efClient.runProcedure(projectName, releaseName, parameter);
//
//      Map<String, String> args = new HashMap<>();
//
//      args.put("procedureName", releaseName);
//      args.put("result", result);
//
//      String summaryHtml = getSummaryHtml(efClient, parameter, args);
//      SummaryTextAction action = new SummaryTextAction(run, summaryHtml);
//
//      run.addAction(action);
//      run.save();
//      logger.println("Run procedure result: " + formatJsonOutput(result));
//    } catch (Exception e) {
//      logger.println(e.getMessage());
//      log.error(e.getMessage(), e);
//      return false;
//    }
//
//    return true;
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

  private String getSummaryHtml(
      ElectricFlowClient configuration, JSONArray parameters, Map<String, String> args) {
    throw new RuntimeException("Not implemented");

//    String result = args.get("result");
//    String procedureName = args.get("releaseName");
//    String jobId = fromObject(result).getString("jobId");
//    String jobUrl = configuration.getElectricFlowUrl() + "/commander/link/jobDetails/jobs/" + jobId;
//    String summaryText =
//        "<h3>CloudBees Flow Run Procedure</h3>"
//            + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
//            + "  <tr>\n"
//            + "    <td>Procedure Name:</td>\n"
//            + "    <td><a href='"
//            + HtmlUtils.encodeForHtml(jobUrl)
//            + "'>"
//            + HtmlUtils.encodeForHtml(procedureName)
//            + "</a></td>   \n"
//            + "  </tr>";
//
//    summaryText = Utils.getParametersHTML(parameters, summaryText, "actualParameterName", "value");
//    summaryText = summaryText + "</table>";
//
//    return summaryText;
  }

  @Symbol("cloudBeesFlowSetJenkinsBuildDetails")
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
      return "CloudBees Flow - Set Jenkins Build Details";
    }

    @Override
    public String getId() {
      return "electricFlowSetJenkinsBuildDetails";
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
