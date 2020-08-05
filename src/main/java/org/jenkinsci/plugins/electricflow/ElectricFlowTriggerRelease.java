// ElectricFlowTriggerRelease.java --
//
// ElectricFlowTriggerRelease.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import static org.jenkinsci.plugins.electricflow.Utils.addParametersToJsonAndPreserveStored;
import static org.jenkinsci.plugins.electricflow.Utils.formatJsonOutput;
import static org.jenkinsci.plugins.electricflow.Utils.getParametersHTML;
import static org.jenkinsci.plugins.electricflow.Utils.getParamsMap;
import static org.jenkinsci.plugins.electricflow.Utils.getValidationComparisonHeaderRow;
import static org.jenkinsci.plugins.electricflow.Utils.getValidationComparisonRow;
import static org.jenkinsci.plugins.electricflow.Utils.getValidationComparisonRowsForExtraParameters;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.checkAnySelectItemsIsValidationWrappers;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.getSelectItemValue;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.isSelectItemValidationWrapper;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;
import org.jenkinsci.plugins.electricflow.exceptions.PluginException;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildAssociationType;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildTriggerSource;
import org.jenkinsci.plugins.electricflow.models.ReleaseRunParameters;
import org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs.CdPipelineStatus;
import org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs.GetPipelineRuntimeDetailsResponseData;
import org.jenkinsci.plugins.electricflow.ui.FieldValidationStatus;
import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectItemValidationWrapper;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class ElectricFlowTriggerRelease extends Recorder implements SimpleBuildStep {

  // ~ Static fields/initializers ---------------------------------------------

  private static final Log log = LogFactory.getLog(ElectricFlowTriggerRelease.class);

  // ~ Instance fields --------------------------------------------------------

  private String configuration;
  private Credential overrideCredential;
  private RunAndWaitOption runAndWaitOption;
  private String projectName;
  private String releaseName;
  private String startingStage;
  private String parameters;

  // ~ Constructors -----------------------------------------------------------

  @DataBoundConstructor
  public ElectricFlowTriggerRelease() {}

  // ~ Methods ----------------------------------------------------------------

  public void perform(
      @Nonnull Run<?, ?> run,
      @Nonnull FilePath filePath,
      @Nonnull Launcher launcher,
      @Nonnull TaskListener taskListener) {
    executeStepAction(run, taskListener);
  }

  private JSONObject executeStepAction(@Nonnull Run<?, ?> run, @Nonnull TaskListener taskListener) {
    PrintStream logger = taskListener.getLogger();

    try {
      EnvReplacer env = new EnvReplacer(run, taskListener);
      ElectricFlowClient efClient =
          ElectricFlowClientFactory.getElectricFlowClient(
              configuration, overrideCredential, run, env, false);

      ReleaseRunParameters releaseRunParameters =
          new ReleaseRunParameters(env, parameters, startingStage);

      logger.println("Preparing to triggerRelease...");

      JSONObject releaseResult = doTriggerRelease(releaseRunParameters, efClient);
      JSONObject flowRuntime = releaseResult.getJSONObject("flowRuntime");

      String summaryHtml =
          getSummaryHtml(efClient.getElectricFlowUrl(), flowRuntime, releaseRunParameters, null);
      SummaryTextAction action = new SummaryTextAction(run, summaryHtml);

      try {
        CloudBeesFlowBuildData cbfdb = new CloudBeesFlowBuildData(run);
        sendCIBuildDetails(efClient, cbfdb, flowRuntime.getString("flowRuntimeId"), logger);
      } catch (RuntimeException ex) {
        // Sending errors will be handled by method, so we have only CloudBeesFlowBuildData here
        logger.println("Failed to build CIBuildData for a run");
      }

      run.addAction(action);
      run.save();

      logger.println("TriggerRelease  result: " + formatJsonOutput(releaseResult.toString()));

      if (runAndWaitOption != null) {
        int checkInterval = runAndWaitOption.getCheckInterval();

        logger.println(
            "Waiting till CloudBees CD pipeline is completed, checking every "
                + checkInterval
                + " seconds");

        GetPipelineRuntimeDetailsResponseData getPipelineRuntimeDetailsResponseData;
        do {
          TimeUnit.SECONDS.sleep(checkInterval);

          getPipelineRuntimeDetailsResponseData =
              efClient.getCdPipelineRuntimeDetails(flowRuntime.getString("flowRuntimeId"));
          logger.println(getPipelineRuntimeDetailsResponseData);

          summaryHtml =
              getSummaryHtml(
                  efClient.getElectricFlowUrl(),
                  flowRuntime,
                  releaseRunParameters,
                  getPipelineRuntimeDetailsResponseData);

          action = new SummaryTextAction(run, summaryHtml);
          run.addOrReplaceAction(action);
          run.save();
        } while (!getPipelineRuntimeDetailsResponseData.isCompleted());

        if (runAndWaitOption.isDependOnCdJobOutcome()) {
          if (getPipelineRuntimeDetailsResponseData.getStatus() != CdPipelineStatus.success
              && getPipelineRuntimeDetailsResponseData.getStatus() != CdPipelineStatus.warning) {
            throw new PluginException(
                "CD pipeline completed with "
                    + getPipelineRuntimeDetailsResponseData.getStatus()
                    + " status");
          }
        }
      }

      return releaseResult;

    } catch (IOException | InterruptedException | PluginException e) {
      logger.println(e.getMessage());
      log.error(e.getMessage(), e);
      run.setResult(Result.FAILURE);
    }

    return null;
  }

  private JSONObject doTriggerRelease(
      @Nonnull ReleaseRunParameters releaseRunParameters, @Nonnull ElectricFlowClient efClient)
      throws IOException {

    String releaseResult =
        efClient.runRelease(
            projectName,
            releaseName,
            releaseRunParameters.getStagesToRun(),
            startingStage,
            releaseRunParameters.getPipelineParameters());

    return JSONObject.fromObject(releaseResult);
  }

  private void sendCIBuildDetails(
      ElectricFlowClient efClient,
      CloudBeesFlowBuildData cbfdb,
      String flowRuntimeId,
      PrintStream logger) {
    try {
      if (log.isDebugEnabled()) {
        logger.println("CBF Data: " + cbfdb.toJsonObject().toString());
      }

      logger.println("About to call setCIBuildDetails after triggering a CD Release");

      JSONObject associateResult =
          efClient.attachCIBuildDetails(
              new CIBuildDetail(cbfdb, projectName)
                  .setFlowRuntimeId(flowRuntimeId)
                  .setAssociationType(BuildAssociationType.TRIGGERED_BY_CI)
                  .setBuildTriggerSource(BuildTriggerSource.CI));

      if (log.isDebugEnabled()) {
        logger.println("Return from efClient: " + associateResult.toString());
      }

    } catch (RuntimeException | IOException ex) {
      log.info("Can't attach CIBuildData to the pipeline run: " + ex.getMessage());
    }
  }

  public String getConfiguration() {
    return configuration;
  }

  @DataBoundSetter
  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  public String getStoredConfiguration() {
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

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public String getParameters() {
    return parameters;
  }

  @DataBoundSetter
  public void setParameters(String parameters) {
    this.parameters = getSelectItemValue(parameters);
  }

  public String getStoredParameters() {
    return parameters;
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

  public String getStartingStage() {
    return startingStage;
  }

  @DataBoundSetter
  public void setStartingStage(String startingStage) {
    this.startingStage = getSelectItemValue(startingStage);
  }

  public String getStoredStartingStage() {
    return startingStage;
  }

  public boolean getValidationTrigger() {
    return true;
  }

  @DataBoundSetter
  public void setValidationTrigger(String validationTrigger) {}

  private String getSummaryHtml(
      String flowUrl,
      JSONObject flowRuntime,
      ReleaseRunParameters releaseParameters,
      GetPipelineRuntimeDetailsResponseData getPipelineRuntimeDetailsResponseData) {

    JSONArray pipelineParameters = releaseParameters.getPipelineParameters();
    List<String> stagesToRun = releaseParameters.getStagesToRun();

    String pipelineId = flowRuntime.getString("pipelineId");
    String flowRuntimeId = flowRuntime.getString("flowRuntimeId");
    String pipelineName = flowRuntime.getString("pipelineName");

    String urlPipeline = flowUrl + "/flow/#pipeline-run/" + pipelineId + "/" + flowRuntimeId;
    String urlRelease = flowUrl + "/flow/#releases";

    String summaryText =
        "<h3>CloudBees CD Trigger Release</h3>"
            + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
            + "  <tr>\n"
            + "    <td>Release Name:</td>\n"
            + "    <td><a href='"
            + HtmlUtils.encodeForHtml(urlRelease)
            + "'>"
            + HtmlUtils.encodeForHtml(releaseName)
            + "</a></td>   \n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>Pipeline URL:</td>\n"
            + "    <td><a href='"
            + HtmlUtils.encodeForHtml(urlPipeline)
            + "'>"
            + HtmlUtils.encodeForHtml(urlPipeline)
            + "</a></td>   \n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>Pipeline Name:</td>\n"
            + "    <td><a href='"
            + HtmlUtils.encodeForHtml(urlPipeline)
            + "'>"
            + HtmlUtils.encodeForHtml(pipelineName)
            + "</a></td>   \n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>Project Name:</td>\n"
            + "    <td>"
            + HtmlUtils.encodeForHtml(projectName)
            + "</td>    \n"
            + "  </tr>";

    if (!startingStage.isEmpty()) {
      summaryText =
          summaryText
              + "  <tr>\n"
              + "    <td>Starting stage:</td>\n"
              + "    <td>"
              + HtmlUtils.encodeForHtml(startingStage)
              + "</td>    \n"
              + "  </tr>";
    }

    if (!stagesToRun.isEmpty()) {
      summaryText = getParametersHTML(stagesToRun, summaryText);
    }

    summaryText =
        getParametersHTML(pipelineParameters, summaryText, "parameterName", "parameterValue");

    if (getPipelineRuntimeDetailsResponseData != null) {
      summaryText =
          summaryText
              + "  <tr>\n"
              + "    <td>CD Pipeline Completed:</td>\n"
              + "    <td>\n"
              + getPipelineRuntimeDetailsResponseData.isCompleted()
              + "    </td>\n"
              + "  </tr>\n";
      summaryText =
          summaryText
              + "  <tr>\n"
              + "    <td>CD Pipeline Status:</td>\n"
              + "    <td>\n"
              + HtmlUtils.encodeForHtml(getPipelineRuntimeDetailsResponseData.getStatus().name())
              + "    </td>\n"
              + "  </tr>\n";
    }
    summaryText = summaryText + "</table>";

    return summaryText;
  }

  @Symbol("cloudBeesFlowTriggerRelease")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    public DescriptorImpl() {
      load();
    }

    // ~ Methods ------------------------------------------------------------

    static Map<String, String> getStagesToRunMapFromParams(String deployParameters) {
      Map<String, String> paramsMap = new HashMap<>();

      if (deployParameters == null || deployParameters.isEmpty() || deployParameters.equals("{}")) {
        return paramsMap;
      }

      JSONObject json = JSONObject.fromObject(deployParameters);

      if (!json.containsKey("release") || !json.getJSONObject("release").containsKey("stages")) {
        return paramsMap;
      }

      return getParamsMap(
          JSONArray.fromObject(json.getJSONObject("release").getString("stages")),
          "stageName",
          "stageValue");
    }

    static Map<String, String> getPipelineParamsMapFromParams(String deployParameters) {
      Map<String, String> paramsMap = new HashMap<>();

      if (deployParameters == null || deployParameters.isEmpty() || deployParameters.equals("{}")) {
        return paramsMap;
      }

      JSONObject json = JSONObject.fromObject(deployParameters);

      if (!json.containsKey("release")
          || !json.getJSONObject("release").containsKey("parameters")) {
        return paramsMap;
      }

      return getParamsMap(
          JSONArray.fromObject(json.getJSONObject("release").getString("parameters")),
          "parameterName",
          "parameterValue");
    }

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

    public FormValidation doCheckStartingStage(
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

    public ListBoxModel doFillParametersItems(
        @QueryParameter String configuration,
        @QueryParameter boolean overrideCredential,
        @QueryParameter @RelativePath("overrideCredential") String credentialId,
        @QueryParameter String projectName,
        @QueryParameter String releaseName,
        @QueryParameter String parameters,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      try {
        ListBoxModel m = new ListBoxModel();

        if (projectName.isEmpty()
            || releaseName.isEmpty()
            || configuration.isEmpty()
            || checkAnySelectItemsIsValidationWrappers(projectName, releaseName)) {
          m.add("{}");

          return m;
        }

        Map<String, String> storedStagesToRun = new HashMap<>();
        Map<String, String> storedPipelineParams = new HashMap<>();

        String parametersValue = getSelectItemValue(parameters);

        // During reload if at least one value filled, return old values
        if (!parametersValue.isEmpty() && !"{}".equals(parametersValue)) {
          JSONObject json = JSONObject.fromObject(parametersValue);
          JSONObject jsonArray = json.getJSONObject("release");

          if (releaseName.equals(jsonArray.getString("releaseName"))) {
            storedStagesToRun = getStagesToRunMapFromParams(parametersValue);
            storedPipelineParams = getPipelineParamsMapFromParams(parametersValue);
          }
        }

        if (!configuration.isEmpty() && !releaseName.isEmpty()) {
          Credential overrideCredentialObj =
              overrideCredential ? new Credential(credentialId) : null;
          ElectricFlowClient client =
              ElectricFlowClientFactory.getElectricFlowClient(
                  configuration, overrideCredentialObj, null, true);
          Release release = client.getRelease(configuration, projectName, releaseName);
          List<String> stages = release.getStartStages();
          List<String> pipelineParameters = release.getPipelineParameters();
          JSONObject main =
              JSONObject.fromObject(
                  "{'release':{'releaseName':'"
                      + releaseName
                      + "','stages':[], pipelineName:'"
                      + release.getPipelineName()
                      + "', 'parameters':[]}}");
          JSONArray stagesArray = main.getJSONObject("release").getJSONArray("stages");

          addParametersToJsonAndPreserveStored(
              stages, stagesArray, "stageName", "stageValue", storedStagesToRun);

          JSONArray parametersArray = main.getJSONObject("release").getJSONArray("parameters");

          addParametersToJsonAndPreserveStored(
              pipelineParameters,
              parametersArray,
              "parameterName",
              "parameterValue",
              storedPipelineParams);
          m.add(main.toString());
        }

        if (m.isEmpty()) {
          m.add("{}");
        }

        return m;
      } catch (Exception e) {
        ListBoxModel m = new ListBoxModel();
        SelectItemValidationWrapper selectItemValidationWrapper;

        Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
        if (Utils.isEflowAvailable(configuration, overrideCredentialObj)) {
          log.error("Error when fetching set of parameters. Error message: " + e.getMessage(), e);
          selectItemValidationWrapper =
              new SelectItemValidationWrapper(
                  FieldValidationStatus.ERROR,
                  "Error when fetching set of parameters. Check the Jenkins logs for more details.",
                  "{}");
        } else {
          selectItemValidationWrapper =
              new SelectItemValidationWrapper(
                  FieldValidationStatus.ERROR,
                  "Error when fetching set of deploy parameters. Connection to CloudBees CD Server Failed. Please fix connection information and reload this page.",
                  "{}");
        }
        m.add(selectItemValidationWrapper.getJsonStr());
        return m;
      }
    }

    public ListBoxModel doFillProjectNameItems(
        @QueryParameter String configuration,
        @QueryParameter boolean overrideCredential,
        @QueryParameter @RelativePath("overrideCredential") String credentialId,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
      return Utils.getProjects(configuration, overrideCredentialObj);
    }

    public ListBoxModel doFillReleaseNameItems(
        @QueryParameter String projectName,
        @QueryParameter String configuration,
        @QueryParameter boolean overrideCredential,
        @QueryParameter @RelativePath("overrideCredential") String credentialId,
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

          Credential overrideCredentialObj =
              overrideCredential ? new Credential(credentialId) : null;
          ElectricFlowClient client =
              ElectricFlowClientFactory.getElectricFlowClient(
                  configuration, overrideCredentialObj, null, true);

          // List<String> releasesList = client.getReleases(configuration, projectName);
          List<String> releasesList = client.getReleaseNames(configuration, projectName);

          for (String release : releasesList) {
            m.add(release);
          }
        }

        return m;
      } catch (Exception e) {
        Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
        if (Utils.isEflowAvailable(configuration, overrideCredentialObj)) {
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

    public ListBoxModel doFillStartingStageItems(
        @QueryParameter String configuration,
        @QueryParameter boolean overrideCredential,
        @QueryParameter @RelativePath("overrideCredential") String credentialId,
        @QueryParameter String projectName,
        @QueryParameter String releaseName,
        @AncestorInPath Item item)
        throws Exception {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      try {
        ListBoxModel m = new ListBoxModel();

        m.add("Select starting stage", "");

        if (projectName.isEmpty()
            || releaseName.isEmpty()
            || configuration.isEmpty()
            || checkAnySelectItemsIsValidationWrappers(projectName, releaseName)) {
          return m;
        }

        Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
        ElectricFlowClient client =
            ElectricFlowClientFactory.getElectricFlowClient(
                configuration, overrideCredentialObj, null, true);

        Release release = client.getRelease(configuration, projectName, releaseName);

        if (release == null) {
          return m;
        }

        List<String> startStages = release.getStartStages();

        for (String state : startStages) {
          m.add(state);
        }

        return m;
      } catch (Exception e) {
        Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
        if (Utils.isEflowAvailable(configuration, overrideCredentialObj)) {
          log.error(
              "Error when fetching values for this parameter - starting stage. Error message: "
                  + e.getMessage(),
              e);
          return SelectFieldUtils.getListBoxModelOnException("Select starting stage");
        } else {
          return SelectFieldUtils.getListBoxModelOnWrongConf("Select starting stage");
        }
      }
    }

    @Override
    public String getDisplayName() {
      return "CloudBees CD - Trigger Release";
    }

    @Override
    public String getId() {
      return "electricFlowTriggerRelease";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    public FormValidation doShowOldValues(
        @QueryParameter("configuration") final String configuration,
        @QueryParameter("projectName") final String projectName,
        @QueryParameter("releaseName") final String releaseName,
        @QueryParameter("startingStage") final String startingStage,
        @QueryParameter("parameters") final String parameters,
        @QueryParameter("storedConfiguration") final String storedConfiguration,
        @QueryParameter("storedProjectName") final String storedProjectName,
        @QueryParameter("storedReleaseName") final String storedReleaseName,
        @QueryParameter("storedStartingStage") final String storedStartingStage,
        @QueryParameter("storedParameters") final String storedParameters,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      String configurationValue = configuration;
      String projectNameValue = getSelectItemValue(projectName);
      String releaseNameValue = getSelectItemValue(releaseName);
      String startingStageValue = getSelectItemValue(startingStage);
      String parametersValue = getSelectItemValue(parameters);

      Map<String, String> stagesToRunMap = getStagesToRunMapFromParams(parametersValue);
      Map<String, String> storedStagesToRunMap = getStagesToRunMapFromParams(storedParameters);

      Map<String, String> pipelineParamsMap = getPipelineParamsMapFromParams(parametersValue);
      Map<String, String> storedPipelineParamsMap =
          getPipelineParamsMapFromParams(storedParameters);

      String comparisonTable =
          "<table>"
              + getValidationComparisonHeaderRow()
              + getValidationComparisonRow("Configuration", storedConfiguration, configurationValue)
              + getValidationComparisonRow("Project Name", storedProjectName, projectNameValue)
              + getValidationComparisonRow("Release Name", storedReleaseName, releaseNameValue)
              + getValidationComparisonRow(
                  "Starting Stage", storedStartingStage, startingStageValue)
              + getValidationComparisonRowsForExtraParameters(
                  "Stages to run", storedStagesToRunMap, stagesToRunMap)
              + getValidationComparisonRowsForExtraParameters(
                  "Release pipeline parameters", storedPipelineParamsMap, pipelineParamsMap)
              + "</table>";

      if (configurationValue.equals(storedConfiguration)
          && projectNameValue.equals(storedProjectName)
          && releaseNameValue.equals(storedReleaseName)
          && startingStageValue.equals(storedStartingStage)
          && pipelineParamsMap.equals(storedPipelineParamsMap)) {
        return FormValidation.okWithMarkup("No changes detected:<br>" + comparisonTable);
      } else {
        return FormValidation.warningWithMarkup("Changes detected:<br>" + comparisonTable);
      }
    }
  }

  private static class Execution extends SynchronousNonBlockingStepExecution<JSONObject> {
    private static final long serialVersionUID = 1L;
    private final transient ElectricFlowTriggerRelease step;

    protected Execution(StepContext context, ElectricFlowTriggerRelease buildStep) {
      super(context);
      this.step = buildStep;
    }

    @Override
    protected JSONObject run() throws Exception {
      StepContext context = getContext();
      return step.executeStepAction(context.get(Run.class), context.get(TaskListener.class));
    }

    private ElectricFlowTriggerRelease getStep() {
      return step;
    }
  }
}
