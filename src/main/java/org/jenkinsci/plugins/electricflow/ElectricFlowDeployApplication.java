// ElectricFlowDeployApplication.java --
//
// ElectricFlowDeployApplication.java is part of ElectricCommander.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.electricflow.exceptions.FlowRuntimeException;
import org.jenkinsci.plugins.electricflow.exceptions.PluginException;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs.CdJobStatus;
import org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs.GetJobStatusResponseData;
import org.jenkinsci.plugins.electricflow.ui.FieldValidationStatus;
import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectItemValidationWrapper;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class ElectricFlowDeployApplication extends Recorder implements SimpleBuildStep {

  // ~ Static fields/initializers ---------------------------------------------

  private static final Log log = LogFactory.getLog(ElectricFlowDeployApplication.class);

  // ~ Instance fields --------------------------------------------------------

  private String configuration;
  private Credential overrideCredential;
  private RunAndWaitOption runAndWaitOption;
  private String projectName;
  private String applicationName;
  private String applicationProcessName;
  private String environmentProjectName;
  private String environmentName;
  private String deployParameters;

  // ~ Constructors -----------------------------------------------------------

  @DataBoundConstructor
  public ElectricFlowDeployApplication() {}

  // ~ Methods ----------------------------------------------------------------

  @Override
  public void perform(
      @Nonnull Run<?, ?> run,
      @Nonnull FilePath filePath,
      @Nonnull Launcher launcher,
      @Nonnull TaskListener taskListener)
      throws InterruptedException, IOException {
    Result result = runProcess(run, taskListener);
    if (result != Result.SUCCESS) {
      run.setResult(result);
    }
  }

  private Result runProcess(@Nonnull Run<?, ?> run, @Nonnull TaskListener taskListener) {
    PrintStream logger = taskListener.getLogger();

    logger.println(
        "Application project name: "
            + projectName
            + ", Application name: "
            + applicationName
            + ", Application process name: "
            + applicationProcessName
            + ", Environment project name: "
            + environmentProjectName
            + ", Environment name: "
            + environmentName);

    JSONObject runProcess = JSONObject.fromObject(deployParameters).getJSONObject("runProcess");
    JSONArray parameter = JSONArray.fromObject(runProcess.getString("parameter"));

    try {
      logger.println("Preparing to run process...");

      EnvReplacer env = new EnvReplacer(run, taskListener);
      ElectricFlowClient efClient =
          ElectricFlowClientFactory.getElectricFlowClient(
              configuration, overrideCredential, run, env, false);
      expandParameters(parameter, env, "value");

      String result =
          efClient.runProcess(
              projectName, applicationName, applicationProcessName, environmentProjectName, environmentName, parameter);
      JSONObject process =
          efClient.getProcess(projectName, applicationName, applicationProcessName);

      if (process == null || process.isEmpty()) {
        throw new PluginException("Cannot find triggered deploy process");
      }

      String processId = process.getJSONObject("process").getString("processId");
      Map<String, String> args = new HashMap<>();

      args.put("applicationName", applicationName);
      args.put("processName", applicationProcessName);
      args.put("processId", processId);
      args.put("result", result);
      args.put("applicationId", process.getJSONObject("process").getString("applicationId"));

      String summaryHtml = getSummaryHtml(efClient, parameter, args, null);
      SummaryTextAction action = new SummaryTextAction(run, summaryHtml);

      run.addAction(action);
      run.save();
      logger.println("Deploy application result: " + formatJsonOutput(result));

      if (runAndWaitOption != null) {
        int checkInterval = runAndWaitOption.getCheckInterval();

        logger.println(
            "Waiting till CloudBees CD job is completed, checking every "
                + checkInterval
                + " seconds");

        String jobId = JSONObject.fromObject(result).getString("jobId");
        GetJobStatusResponseData responseData;
        do {
          TimeUnit.SECONDS.sleep(checkInterval);

          responseData = efClient.getCdJobStatus(jobId);
          logger.println(responseData);

          summaryHtml = getSummaryHtml(efClient, parameter, args, responseData);
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
    } catch (PluginException | InterruptedException | IOException e) {
      logger.println(e.getMessage());
      log.error(e.getMessage(), e);

      return Result.FAILURE;
    }

    return Result.SUCCESS;
  }

  public String getApplicationName() {
    return applicationName;
  }

  @DataBoundSetter
  public void setApplicationName(String applicationName) {
    this.applicationName = getSelectItemValue(applicationName);
  }

  public String getStoredApplicationName() {
    return applicationName;
  }

  public String getApplicationProcessName() {
    return applicationProcessName;
  }

  @DataBoundSetter
  public void setApplicationProcessName(String applicationProcessName) {
    this.applicationProcessName = getSelectItemValue(applicationProcessName);
  }

  public String getStoredApplicationProcessName() {
    return applicationProcessName;
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

  public String getDeployParameters() {
    return deployParameters;
  }

  @DataBoundSetter
  public void setDeployParameters(String deployParameters) {
    this.deployParameters = getSelectItemValue(deployParameters);
  }

  public String getStoredDeployParameters() {
    return deployParameters;
  }

  public String getEnvironmentProjectName() {
    return environmentProjectName;
  }

  @DataBoundSetter
  public void setEnvironmentProjectName(String environmentProjectName) {
    this.environmentProjectName = getSelectItemValue(environmentProjectName);
  }

  public String getStoredEnvironmentProjectName() {
    return environmentProjectName;
  }

  public String getEnvironmentName() {
    return environmentName;
  }

  @DataBoundSetter
  public void setEnvironmentName(String environmentName) {
    this.environmentName = getSelectItemValue(environmentName);
  }

  public String getStoredEnvironmentName() {
    return environmentName;
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

  public boolean getValidationTrigger() {
    return true;
  }

  @DataBoundSetter
  public void setValidationTrigger(String validationTrigger) {}

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  private String getSummaryHtml(
      ElectricFlowClient configuration,
      JSONArray parameters,
      Map<String, String> args,
      GetJobStatusResponseData getJobStatusResponseData) {
    String result = args.get("result");
    String applicationName = args.get("applicationName");
    String processId = args.get("processId");
    String jobId = JSONObject.fromObject(result).getString("jobId");
    String applicationId = args.get("applicationId");
    String applicationUrl =
        configuration.getElectricFlowUrl() + "/flow/#applications/" + applicationId;
    String deployRunUrl =
        configuration.getElectricFlowUrl()
            + "/flow/#applications/"
            + processId
            + "/"
            + jobId
            + "/runningProcess";
    String summaryText =
        "<h3>CloudBees CD Deploy Application</h3>"
            + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
            + "  <tr>\n"
            + "    <td>Application Name:</td>\n"
            + "    <td><a href='"
            + HtmlUtils.encodeForHtml(applicationUrl)
            + "'>"
            + HtmlUtils.encodeForHtml(applicationName)
            + "</a></td>   \n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>Deploy run URL:</td>\n"
            + "    <td><a href='"
            + HtmlUtils.encodeForHtml(deployRunUrl)
            + "'>"
            + HtmlUtils.encodeForHtml(deployRunUrl)
            + "</a></td>   \n"
            + "  </tr>";

    summaryText = Utils.getParametersHTML(parameters, summaryText, "actualParameterName", "value");
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

  @Symbol("cloudBeesFlowDeployApplication")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    // ~ Instance fields ----------------------------------------------------

    // ~ Constructors -------------------------------------------------------

    public DescriptorImpl() {
      load();
    }

    // ~ Methods ------------------------------------------------------------

    static Map<String, String> getParamsMapFromDeployParams(String deployParameters) {
      Map<String, String> paramsMap = new HashMap<>();

      if (deployParameters == null || deployParameters.isEmpty() || deployParameters.equals("{}")) {
        return paramsMap;
      }

      JSONObject json = JSONObject.fromObject(deployParameters);

      if (!json.containsKey("runProcess")
          || !json.getJSONObject("runProcess").containsKey("parameter")) {
        return paramsMap;
      }

      return getParamsMap(
          JSONArray.fromObject(json.getJSONObject("runProcess").getString("parameter")),
          "actualParameterName",
          "value");
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

    public FormValidation doCheckDeployParameters(
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

    public FormValidation doCheckApplicationName(
        @QueryParameter String value,
        @QueryParameter boolean validationTrigger,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      if (isSelectItemValidationWrapper(value)) {
        return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
      }
      return Utils.validateValueOnEmpty(value, "Application name");
    }

    public FormValidation doCheckApplicationProcessName(
        @QueryParameter String value,
        @QueryParameter boolean validationTrigger,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      if (isSelectItemValidationWrapper(value)) {
        return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
      }
      return Utils.validateValueOnEmpty(value, "Application process name");
    }

    public FormValidation doCheckEnvironmentProjectName(
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

    public FormValidation doCheckEnvironmentName(
        @QueryParameter String value,
        @QueryParameter boolean validationTrigger,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      if (isSelectItemValidationWrapper(value)) {
        return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
      }
      return Utils.validateValueOnEmpty(value, "Environment name");
    }

    public ListBoxModel doFillApplicationNameItems(
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

        m.add("Select application", "");

        if (!configuration.isEmpty()
            && !projectName.isEmpty()
            && SelectFieldUtils.checkAllSelectItemsAreNotValidationWrappers(projectName)) {
          Credential overrideCredentialObj =
              overrideCredential ? new Credential(credentialId) : null;
          ElectricFlowClient client =
              ElectricFlowClientFactory.getElectricFlowClient(
                  configuration, overrideCredentialObj, null, true);

          List<String> applications = client.getApplications(projectName);

          for (String application : applications) {
            m.add(application);
          }
        }

        return m;
      } catch (Exception e) {
        Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
        if (Utils.isEflowAvailable(configuration, overrideCredentialObj)) {
          log.error(
              "Error when fetching values for this parameter - application. Error message: "
                  + e.getMessage(),
              e);
          return SelectFieldUtils.getListBoxModelOnException("Select application");
        } else {
          return SelectFieldUtils.getListBoxModelOnWrongConf("Select application");
        }
      }
    }

    public ListBoxModel doFillApplicationProcessNameItems(
        @QueryParameter String configuration,
        @QueryParameter boolean overrideCredential,
        @QueryParameter @RelativePath("overrideCredential") String credentialId,
        @QueryParameter String projectName,
        @QueryParameter String applicationName,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      try {
        ListBoxModel m = new ListBoxModel();

        m.add("Select application process", "");

        if (!configuration.isEmpty()
            && !projectName.isEmpty()
            && !applicationName.isEmpty()
            && SelectFieldUtils.checkAllSelectItemsAreNotValidationWrappers(
                projectName, applicationName)) {
          Credential overrideCredentialObj =
              overrideCredential ? new Credential(credentialId) : null;
          ElectricFlowClient client =
              ElectricFlowClientFactory.getElectricFlowClient(
                  configuration, overrideCredentialObj, null, true);
          List<String> processes = client.getProcesses(projectName, applicationName);

          for (String process : processes) {
            m.add(process);
          }
        }

        return m;
      } catch (Exception e) {
        Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
        if (Utils.isEflowAvailable(configuration, overrideCredentialObj)) {
          log.error(
              "Error when fetching values for this parameter - application process. Error message: "
                  + e.getMessage(),
              e);
          return SelectFieldUtils.getListBoxModelOnException("Select application process");
        } else {
          return SelectFieldUtils.getListBoxModelOnWrongConf("Select application process");
        }
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

    public ListBoxModel doFillDeployParametersItems(
        @QueryParameter String configuration,
        @QueryParameter boolean overrideCredential,
        @QueryParameter @RelativePath("overrideCredential") String credentialId,
        @QueryParameter String projectName,
        @QueryParameter String applicationName,
        @QueryParameter String applicationProcessName,
        @QueryParameter String deployParameters,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      try {
        ListBoxModel m = new ListBoxModel();

        if (configuration.isEmpty()
            || projectName.isEmpty()
            || applicationName.isEmpty()
            || applicationProcessName.isEmpty()
            || !SelectFieldUtils.checkAllSelectItemsAreNotValidationWrappers(
                projectName, applicationName, applicationProcessName)) {
          m.add("{}");

          return m;
        }

        Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
        ElectricFlowClient client =
            ElectricFlowClientFactory.getElectricFlowClient(
                configuration, overrideCredentialObj, null, true);

        Map<String, String> storedParams = new HashMap<>();

        String deployParametersValue = getSelectItemValue(deployParameters);

        // During reload if at least one value filled, return old values
        if (!deployParametersValue.isEmpty() && !"{}".equals(deployParametersValue)) {
          JSONObject json = JSONObject.fromObject(deployParametersValue);
          JSONObject jsonArray = json.getJSONObject("runProcess");

          if (applicationName.equals(jsonArray.get("applicationName"))
              && applicationProcessName.equals(jsonArray.get("applicationProcessName"))) {
            storedParams = getParamsMapFromDeployParams(deployParametersValue);
          }
        }

        List<String> parameters =
            client.getFormalParameters(projectName, applicationName, applicationProcessName);
        JSONObject main =
            JSONObject.fromObject(
                "{'runProcess':{'applicationName':'"
                    + applicationName
                    + "', 'applicationProcessName':'"
                    + applicationProcessName
                    + "',   'parameter':[]}}");
        JSONArray ja = main.getJSONObject("runProcess").getJSONArray("parameter");

        addParametersToJsonAndPreserveStored(
            parameters, ja, "actualParameterName", "value", storedParams);
        m.add(main.toString());

        if (m.isEmpty()) {
          m.add("{}");
        }

        return m;
      } catch (Exception e) {
        ListBoxModel m = new ListBoxModel();
        SelectItemValidationWrapper selectItemValidationWrapper;

        Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
        if (Utils.isEflowAvailable(configuration, overrideCredentialObj)) {
          log.error(
              "Error when fetching set of deploy parameters. Error message: " + e.getMessage(), e);
          selectItemValidationWrapper =
              new SelectItemValidationWrapper(
                  FieldValidationStatus.ERROR,
                  "Error when fetching set of deploy parameters. Check the Jenkins logs for more details.",
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

    public ListBoxModel doFillEnvironmentProjectNameItems(
        @QueryParameter String configuration,
        @QueryParameter boolean overrideCredential,
        @QueryParameter @RelativePath("overrideCredential") String credentialId,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
      return Utils.getProjects(configuration, overrideCredentialObj, false);
    }

    public ListBoxModel doFillEnvironmentNameItems(
        @QueryParameter String configuration,
        @QueryParameter boolean overrideCredential,
        @QueryParameter @RelativePath("overrideCredential") String credentialId,
        @QueryParameter String projectName,
        @QueryParameter String environmentProjectName,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      try {
        ListBoxModel m = new ListBoxModel();

        m.add("Select environment", "");

        String actualEnvironmentProjectName =
            environmentProjectName != null
                    && !environmentProjectName.isEmpty()
                    && !isSelectItemValidationWrapper(environmentProjectName)
                ? environmentProjectName
                : projectName;

        if (!configuration.isEmpty()
            && !actualEnvironmentProjectName.isEmpty()
            && SelectFieldUtils.checkAllSelectItemsAreNotValidationWrappers(actualEnvironmentProjectName)) {
          Credential overrideCredentialObj =
              overrideCredential ? new Credential(credentialId) : null;
          ElectricFlowClient client =
              ElectricFlowClientFactory.getElectricFlowClient(
                  configuration, overrideCredentialObj, null, true);
          List<String> environments = client.getEnvironments(actualEnvironmentProjectName);

          for (String environment : environments) {
            m.add(environment);
          }
        }

        return m;
      } catch (Exception e) {
        Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
        if (Utils.isEflowAvailable(configuration, overrideCredentialObj)) {
          log.error(
              "Error when fetching values for this parameter - environment. Error message: "
                  + e.getMessage(),
              e);
          return SelectFieldUtils.getListBoxModelOnException("Select environment");
        } else {
          return SelectFieldUtils.getListBoxModelOnWrongConf("Select environment");
        }
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

    @Override
    public String getDisplayName() {
      return "CloudBees CD - Deploy Application";
    }

    @Override
    public String getId() {
      return "electricFlowDeployApplication";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    public FormValidation doShowOldValues(
        @QueryParameter("configuration") final String configuration,
        @QueryParameter("projectName") final String projectName,
        @QueryParameter("applicationName") final String applicationName,
        @QueryParameter("applicationProcessName") final String applicationProcessName,
        @QueryParameter("environmentProjectName") final String environmentProjectName,
        @QueryParameter("environmentName") final String environmentName,
        @QueryParameter("deployParameters") final String deployParameters,
        @QueryParameter("storedConfiguration") final String storedConfiguration,
        @QueryParameter("storedProjectName") final String storedProjectName,
        @QueryParameter("storedApplicationName") final String storedApplicationName,
        @QueryParameter("storedApplicationProcessName") final String storedApplicationProcessName,
        @QueryParameter("storedEnvironmentProjectName") final String storedEnvironmentProjectName,
        @QueryParameter("storedEnvironmentName") final String storedEnvironmentName,
        @QueryParameter("storedDeployParameters") final String storedDeployParameters,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      String configurationValue = configuration;
      String projectNameValue = getSelectItemValue(projectName);
      String applicationNameValue = getSelectItemValue(applicationName);
      String applicationProcessNameValue = getSelectItemValue(applicationProcessName);
      String environmentProjectNameValue = getSelectItemValue(environmentProjectName);
      String environmentNameValue = getSelectItemValue(environmentName);
      String deployParametersValue = getSelectItemValue(deployParameters);

      Map<String, String> deployParamsMap = getParamsMapFromDeployParams(deployParametersValue);
      Map<String, String> storedDeployParamsMap =
          getParamsMapFromDeployParams(storedDeployParameters);

      String comparisonTable =
          "<table>"
              + getValidationComparisonHeaderRow()
              + getValidationComparisonRow("Configuration", storedConfiguration, configurationValue)
              + getValidationComparisonRow("Application Project Name", storedProjectName, projectNameValue)
              + getValidationComparisonRow(
                  "Application Name", storedApplicationName, applicationNameValue)
              + getValidationComparisonRow(
                  "Application Process Name",
                  storedApplicationProcessName,
                  applicationProcessNameValue)
              + getValidationComparisonRow(
              "Environment Project Name", storedEnvironmentProjectName, environmentProjectNameValue)
              + getValidationComparisonRow(
                  "Environment Name", storedEnvironmentName, environmentNameValue)
              + getValidationComparisonRowsForExtraParameters(
                  "Deploy Parameters", storedDeployParamsMap, deployParamsMap)
              + "</table>";

      if (configurationValue.equals(storedConfiguration)
          && projectNameValue.equals(storedProjectName)
          && applicationNameValue.equals(storedApplicationName)
          && applicationProcessNameValue.equals(storedApplicationProcessName)
          && environmentProjectNameValue.equals(storedEnvironmentProjectName)
          && environmentNameValue.equals(storedEnvironmentName)
          && deployParamsMap.equals(storedDeployParamsMap)) {
        return FormValidation.okWithMarkup("No changes detected:<br>" + comparisonTable);
      } else {
        return FormValidation.warningWithMarkup("Changes detected:<br>" + comparisonTable);
      }
    }
  }
}
