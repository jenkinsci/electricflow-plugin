// ElectricFlowPipelinePublisher.java --
//
// ElectricFlowPipelinePublisher.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import static org.jenkinsci.plugins.electricflow.Utils.addParametersToJsonAndPreserveStored;
import static org.jenkinsci.plugins.electricflow.Utils.expandParameters;
import static org.jenkinsci.plugins.electricflow.Utils.formatJsonOutput;
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
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildAssociationType;
import org.jenkinsci.plugins.electricflow.ui.FieldValidationStatus;
import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectItemValidationWrapper;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ElectricFlowPipelinePublisher extends Recorder implements SimpleBuildStep {

  // ~ Static fields/initializers ---------------------------------------------

  private static final Log log = LogFactory.getLog(ElectricFlowPipelinePublisher.class);

  // ~ Instance fields --------------------------------------------------------

  private String projectName;
  private String pipelineName;
  private String configuration;
  private Credential overrideCredential;
  private String addParam;
  private JSONArray additionalOption;

  // ~ Constructors -----------------------------------------------------------

  @DataBoundConstructor
  public ElectricFlowPipelinePublisher() {}

  // ~ Methods ----------------------------------------------------------------

  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    return runPipeline(build, listener, null);
  }

  @Override
  public void perform(
      @Nonnull Run<?, ?> run,
      @Nonnull FilePath filePath,
      @Nonnull Launcher launcher,
      @Nonnull TaskListener taskListener)
      throws InterruptedException, IOException {
    boolean result = runPipeline(run, null, taskListener);

    if (!result) {
      run.setResult(Result.FAILURE);
    }
  }

  private void logListener(BuildListener buildListener, TaskListener taskListener, String log) {

    if (buildListener != null) {
      buildListener.getLogger().println(log);
    } else if (taskListener != null) {
      taskListener.getLogger().println(log);
    }
  }

  private boolean runPipeline(
      Run<?, ?> run, BuildListener buildListener, TaskListener taskListener) {
    logListener(
        buildListener,
        taskListener,
        "Project name: " + projectName + ", Pipeline name: " + pipelineName);

    EnvReplacer env = null;
    ElectricFlowClient efClient;
    try {
      env = new EnvReplacer(run, taskListener);
      efClient =
          ElectricFlowClientFactory.getElectricFlowClient(configuration, overrideCredential, env);
    } catch (Exception e) {
      taskListener
          .getLogger()
          .println("Cannot create CloudBees Flow client. Error: " + e.getMessage());
      log.error("Cannot create CloudBees Flow client. Error: " + e.getMessage(), e);

      return false;
    }

    try {
      List<String> paramsResponse = efClient.getPipelineFormalParameters(projectName, pipelineName);

      if (log.isDebugEnabled()) {
        log.debug("FormalParameters are: " + paramsResponse.toString());
      }
    } catch (Exception e) {
      taskListener
          .getLogger()
          .println("Error occurred during formal parameters fetch: " + e.getMessage());
      log.error("Error occurred during formal parameters fetch: " + e.getMessage(), e);

      return false;
    }

    try {
      logListener(buildListener, taskListener, "Preparing to run pipeline...");

      String pipelineResult;
      JSONArray parameters = getPipelineParameters();

      if (parameters.isEmpty()) {
        pipelineResult = efClient.runPipeline(projectName, pipelineName);
      } else {
        expandParameters(parameters, env);
        pipelineResult = efClient.runPipeline(projectName, pipelineName, parameters);
      }

      String summaryHtml = getSummaryHtml(efClient, pipelineResult, parameters);
      SummaryTextAction action = new SummaryTextAction(run, summaryHtml);
      String flowRuntimeId = getFlowRuntimeIdFromResponse(pipelineResult);
      String projectName = getProjectNameFromResponse(pipelineResult);

      // PrintStream logger = taskListener.getLogger();
      CloudBeesFlowBuildData cbfdb = new CloudBeesFlowBuildData(run);
      taskListener.getLogger().println("++++++++++++++++++++++++++++++++++++++++++++");
      taskListener.getLogger().println("CBF Data: " + cbfdb.toJsonObject().toString());
      taskListener.getLogger().println("++++++++++++++++++++++++++++++++++++++++++++");
      taskListener
          .getLogger()
          .println("About to call setJenkinsBuildDetails after running a Pipeline");

      JSONObject associateResult =
          efClient.setCIBuildDetails(
              new CIBuildDetail(cbfdb, projectName)
                  .setFlowRuntimeId(flowRuntimeId)
                  .setAssociationType(BuildAssociationType.TRIGGERED_BY_CI));

      taskListener.getLogger().println("Return from efClient: " + associateResult.toString());
      taskListener.getLogger().println("++++++++++++++++++++++++++++++++++++++++++++");

      run.addAction(action);
      run.save();
      logListener(
          buildListener, taskListener, "Pipeline result: " + formatJsonOutput(pipelineResult));
    } catch (Exception e) {
      logListener(buildListener, taskListener, e.getMessage());
      log.error(e.getMessage(), e);

      return false;
    }

    return true;
  }

  public JSONArray getAdditionalOption() {
    return additionalOption;
  }

  @DataBoundSetter
  public void setAdditionalOption(JSONArray additionalOption) {
    this.additionalOption = additionalOption;
  }

  public String getAddParam() {
    return addParam;
  }

  @DataBoundSetter
  public void setAddParam(String addParam) {
    this.addParam = getSelectItemValue(addParam);
  }

  public String getStoredAddParam() {
    return addParam;
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

  // Overridden for better type safety.
  // If your plugin doesn't really define any property on Descriptor,
  // you don't have to do this.
  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public String getPipelineName() {
    return pipelineName;
  }

  @DataBoundSetter
  public void setPipelineName(String pipelineName) {
    this.pipelineName = getSelectItemValue(pipelineName);
  }

  public String getStoredPipelineName() {
    return pipelineName;
  }

  private JSONArray getPipelineParameters() {

    if (addParam != null && !addParam.isEmpty() && !"{}".equals(addParam)) {
      JSONObject pipelineJsonObject = JSONObject.fromObject(addParam).getJSONObject("pipeline");
      JSONArray pipelineParameters =
          JSONArray.fromObject(pipelineJsonObject.getString("parameters"));

      if (!pipelineParameters.isEmpty()) {
        return pipelineParameters;
      }
    }

    return new JSONArray();
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

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  private String getFlowRuntimeIdFromResponse(String pipelineResult) {
    JSONObject flowRuntime = JSONObject.fromObject(pipelineResult).getJSONObject("flowRuntime");
    // String     pipelineId    = (String) flowRuntime.get("pipelineId");
    String flowRuntimeId = (String) flowRuntime.get("flowRuntimeId");
    return flowRuntimeId;
  }

  private String getProjectNameFromResponse(String pipelineResult) {
    JSONObject flowRuntime = JSONObject.fromObject(pipelineResult).getJSONObject("flowRuntime");

    String projectName = (String) flowRuntime.get("projectName");
    return projectName;
  }

  private String getSummaryHtml(
      ElectricFlowClient efClient, String pipelineResult, JSONArray parameters) {
    JSONObject flowRuntime = JSONObject.fromObject(pipelineResult).getJSONObject("flowRuntime");
    String pipelineId = (String) flowRuntime.get("pipelineId");
    String flowRuntimeId = (String) flowRuntime.get("flowRuntimeId");
    String url =
        efClient.getElectricFlowUrl() + "/flow/#pipeline-run/" + pipelineId + "/" + flowRuntimeId;
    String summaryText =
        "<h3>CloudBees Flow Run Pipeline</h3>"
            + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
            + "  <tr>\n"
            + "    <td>Pipeline URL:</td>\n"
            + "    <td><a href='"
            + HtmlUtils.encodeForHtml(url)
            + "'>"
            + HtmlUtils.encodeForHtml(url)
            + "</a></td>   \n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>Pipeline Name:</td>\n"
            + "    <td><a href='"
            + HtmlUtils.encodeForHtml(url)
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

    summaryText =
        Utils.getParametersHTML(parameters, summaryText, "parameterName", "parameterValue");
    summaryText = summaryText + "</table>";

    return summaryText;
  }

  // ~ Inner Classes ----------------------------------------------------------

  /**
   * The class is marked as public so that it can be accessed from views.
   *
   * <p>See .jelly for the actual HTML fragment for the configuration screen.
   */
  @Symbol("cloudBeesFlowRunPipeline")
  @Extension // This indicates to Jenkins that this is an implementation of
  // an extension point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    // ~ Constructors -------------------------------------------------------

    /**
     * In order to load the persisted global configuration, you have to call load() in the
     * constructor.
     */
    public DescriptorImpl() {
      load();
    }

    // ~ Methods ------------------------------------------------------------

    static Map<String, String> getParamsMapFromAddParam(String addParam) {
      Map<String, String> paramsMap = new HashMap<>();

      if (addParam == null || addParam.isEmpty() || addParam.equals("{}")) {
        return paramsMap;
      }

      JSONObject json = JSONObject.fromObject(addParam);

      if (!json.containsKey("pipeline")
          || !json.getJSONObject("pipeline").containsKey("parameters")) {
        return paramsMap;
      }

      return getParamsMap(
          JSONArray.fromObject(json.getJSONObject("pipeline").getString("parameters")),
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

    public FormValidation doCheckPipelineName(
        @QueryParameter String value,
        @QueryParameter boolean validationTrigger,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      if (isSelectItemValidationWrapper(value)) {
        return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
      }
      return Utils.validateValueOnEmpty(value, "Pipeline name");
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

    public FormValidation doCheckAddParam(
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

    public ListBoxModel doFillAddParamItems(
        @QueryParameter String configuration,
        @QueryParameter String projectName,
        @QueryParameter String pipelineName,
        @QueryParameter String addParam,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      try {
        ListBoxModel m = new ListBoxModel();

        if (configuration.isEmpty()
            || pipelineName.isEmpty()
            || checkAnySelectItemsIsValidationWrappers(pipelineName)) {
          m.add("{}");

          return m;
        }

        Map<String, String> storedParams = new HashMap<>();

        String addParamValue = getSelectItemValue(addParam);

        if (!addParamValue.isEmpty() && !"{}".equals(addParamValue)) {
          JSONObject json = JSONObject.fromObject(addParamValue);
          JSONObject jsonArray = json.getJSONObject("pipeline");

          if (pipelineName.equals(jsonArray.get("pipelineName"))) {
            storedParams = getParamsMapFromAddParam(addParamValue);
          }
        }

        ElectricFlowClient efClient = new ElectricFlowClient(configuration);
        List<String> parameters = efClient.getPipelineFormalParameters(projectName, pipelineName);
        JSONObject main =
            JSONObject.fromObject(
                "{'pipeline':{'pipelineName':'" + pipelineName + "','parameters':[]}}");
        JSONArray ja = main.getJSONObject("pipeline").getJSONArray("parameters");

        addParametersToJsonAndPreserveStored(
            parameters, ja, "parameterName", "parameterValue", storedParams);
        m.add(main.toString());

        if (m.isEmpty()) {
          m.add("{}");
        }

        return m;
      } catch (Exception e) {
        ListBoxModel m = new ListBoxModel();
        SelectItemValidationWrapper selectItemValidationWrapper;

        if (Utils.isEflowAvailable(configuration)) {
          log.error(
              "Error when fetching set of pipeline parameters. Error message: " + e.getMessage(),
              e);
          selectItemValidationWrapper =
              new SelectItemValidationWrapper(
                  FieldValidationStatus.ERROR,
                  "Error when fetching set of pipeline parameters. Check the Jenkins logs for more details.",
                  "{}");
        } else {
          selectItemValidationWrapper =
              new SelectItemValidationWrapper(
                  FieldValidationStatus.ERROR,
                  "Error when fetching set of pipeline parameters. Connection to CloudBees Flow Server Failed. Please fix connection information and reload this page.",
                  "{}");
        }
        m.add(selectItemValidationWrapper.getJsonStr());
        return m;
      }
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

    public ListBoxModel doFillPipelineNameItems(
        @QueryParameter String projectName,
        @QueryParameter String configuration,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      return Utils.getPipelines(configuration, projectName);
    }

    public ListBoxModel doFillProjectNameItems(
        @QueryParameter String configuration, @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      return Utils.getProjects(configuration);
    }

    @Override
    public void doHelp(StaplerRequest req, StaplerResponse rsp)
        throws IOException, ServletException {
      super.doHelp(req, rsp);
    }

    public FormValidation doTestConnection() {
      return FormValidation.ok("Success");
    }

    public Configuration getConfigurationByName(String name) {
      return Utils.getConfigurationByName(name);
    }

    public List<Configuration> getConfigurations() {
      return Utils.getConfigurations();
    }

    /**
     * This human readable name is used in the configuration screen.
     *
     * @return this human readable name is used in the configuration screen.
     */
    @Override
    public String getDisplayName() {
      return "CloudBees Flow - Run Pipeline";
    }

    @Override
    public String getId() {
      return "electricFlowSettings";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {

      // Indicates that this builder can be used with all kinds of
      // project types
      return true;
    }

    public FormValidation doShowOldValues(
        @QueryParameter("configuration") final String configuration,
        @QueryParameter("projectName") final String projectName,
        @QueryParameter("pipelineName") final String pipelineName,
        @QueryParameter("addParam") final String addParam,
        @QueryParameter("storedConfiguration") final String storedConfiguration,
        @QueryParameter("storedProjectName") final String storedProjectName,
        @QueryParameter("storedPipelineName") final String storedPipelineName,
        @QueryParameter("storedAddParam") final String storedAddParam,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      String configurationValue = configuration;
      String projectNameValue = getSelectItemValue(projectName);
      String pipelineNameValue = getSelectItemValue(pipelineName);
      String addParamValue = getSelectItemValue(addParam);

      Map<String, String> pipelineParamsMap = getParamsMapFromAddParam(addParamValue);
      Map<String, String> storedPipelineParamsMap = getParamsMapFromAddParam(storedAddParam);

      String comparisonTable =
          "<table>"
              + getValidationComparisonHeaderRow()
              + getValidationComparisonRow("Configuration", storedConfiguration, configurationValue)
              + getValidationComparisonRow("Project Name", storedProjectName, projectNameValue)
              + getValidationComparisonRow("Pipeline Name", storedPipelineName, pipelineNameValue)
              + getValidationComparisonRowsForExtraParameters(
                  "Pipeline Parameters", storedPipelineParamsMap, pipelineParamsMap)
              + "</table>";

      if (configurationValue.equals(storedConfiguration)
          && projectNameValue.equals(storedProjectName)
          && pipelineNameValue.equals(storedPipelineName)
          && pipelineParamsMap.equals(storedPipelineParamsMap)) {
        return FormValidation.okWithMarkup("No changes detected:<br>" + comparisonTable);
      } else {
        return FormValidation.warningWithMarkup("Changes detected:<br>" + comparisonTable);
      }
    }
  }
}
