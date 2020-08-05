// ElectricFlowRunProcedure.java --
//
// ElectricFlowRunProcedure.java is part of ElectricCommander.
//
// Copyright (c) 2005-2018 Electric Cloud, Inc.
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
import org.jenkinsci.plugins.electricflow.exceptions.PluginException;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs.CdJobOutcome;
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

public class ElectricFlowRunProcedure extends Recorder implements SimpleBuildStep {

  private static final Log log = LogFactory.getLog(ElectricFlowRunProcedure.class);

  private String configuration;
  private Credential overrideCredential;
  private RunAndWaitOption runAndWaitOption;
  private String projectName;
  private String procedureName;
  private String procedureParameters;

  @DataBoundConstructor
  public ElectricFlowRunProcedure() {}

  @Override
  public void perform(
      @Nonnull Run<?, ?> run,
      @Nonnull FilePath filePath,
      @Nonnull Launcher launcher,
      @Nonnull TaskListener taskListener) {
    Result result = runProcedure(run, taskListener);
    if (result != Result.SUCCESS) {
      run.setResult(result);
    }
  }

  private Result runProcedure(@Nonnull Run<?, ?> run, @Nonnull TaskListener taskListener) {
    PrintStream logger = taskListener.getLogger();

    logger.println("Project name: " + projectName + ", Procedure name: " + procedureName);

    JSONObject procedure = JSONObject.fromObject(procedureParameters).getJSONObject("procedure");
    JSONArray parameter = JSONArray.fromObject(procedure.getString("parameters"));

    try {
      logger.println("Preparing to run procedure...");

      EnvReplacer env = new EnvReplacer(run, taskListener);
      expandParameters(parameter, env, "value");

      ElectricFlowClient efClient =
          ElectricFlowClientFactory.getElectricFlowClient(
              configuration, overrideCredential, run, env, false);

      String result = efClient.runProcedure(projectName, procedureName, parameter);
      logger.println("Run procedure launched. Response JSON: " + formatJsonOutput(result));

      Map<String, String> args = new HashMap<>();

      args.put("procedureName", procedureName);
      args.put("result", result);

      String summaryHtml = getSummaryHtml(efClient, parameter, args, null);
      SummaryTextAction action = new SummaryTextAction(run, summaryHtml);

      run.addAction(action);
      run.save();

      if (runAndWaitOption != null) {
        int checkInterval = runAndWaitOption.getCheckInterval();

        logger.println(
            "Waiting till CloudBees CD job is completed, checking every "
                + checkInterval
                + " seconds");

        String jobId = JSONObject.fromObject(result).getString("jobId");
        GetJobStatusResponseData getJobStatusResponseData;
        do {
          TimeUnit.SECONDS.sleep(checkInterval);

          getJobStatusResponseData = efClient.getCdJobStatus(jobId);
          logger.println(getJobStatusResponseData);

          summaryHtml = getSummaryHtml(efClient, parameter, args, getJobStatusResponseData);
          action = new SummaryTextAction(run, summaryHtml);
          run.addOrReplaceAction(action);
          run.save();
          if (getJobStatusResponseData.getStatus() == CdJobStatus.unknown) {
            throw new PluginException("Unexpected format of CD job status response");
          }
        } while (getJobStatusResponseData.getStatus() != CdJobStatus.completed);

        logger.println(
            "CD job completed with " + getJobStatusResponseData.getOutcome() + " outcome");
        if (runAndWaitOption.isDependOnCdJobOutcome()) {
          if (getJobStatusResponseData.getOutcome() == CdJobOutcome.error
              || getJobStatusResponseData.getOutcome() == CdJobOutcome.unknown) {
            return Result.FAILURE;
          } else if (getJobStatusResponseData.getOutcome() == CdJobOutcome.warning) {
            return Result.UNSTABLE;
          }
        }
      }
    } catch (PluginException | IOException | InterruptedException e) {
      logger.println(e.getMessage());
      log.error(e.getMessage(), e);
      return Result.FAILURE;
    }

    return Result.SUCCESS;
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

  public RunAndWaitOption getRunAndWaitOption() {
    return runAndWaitOption;
  }

  @DataBoundSetter
  public void setRunAndWaitOption(RunAndWaitOption runAndWaitOption) {
    this.runAndWaitOption = runAndWaitOption;
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

  public String getProcedureName() {
    return procedureName;
  }

  @DataBoundSetter
  public void setProcedureName(String procedureName) {
    this.procedureName = getSelectItemValue(procedureName);
  }

  public String getStoredProcedureName() {
    return procedureName;
  }

  public String getProcedureParameters() {
    return procedureParameters;
  }

  @DataBoundSetter
  public void setProcedureParameters(String procedureParameters) {
    this.procedureParameters = getSelectItemValue(procedureParameters);
  }

  public String getStoredProcedureParameters() {
    return procedureParameters;
  }

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
    String procedureName = args.get("procedureName");
    String jobId = JSONObject.fromObject(result).getString("jobId");
    String jobUrl = configuration.getElectricFlowUrl() + "/commander/link/jobDetails/jobs/" + jobId;
    String summaryText =
        "<h3>CloudBees CD Run Procedure</h3>"
            + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
            + "  <tr>\n"
            + "    <td>Procedure Name:</td>\n"
            + "    <td><a href='"
            + HtmlUtils.encodeForHtml(jobUrl)
            + "'>"
            + HtmlUtils.encodeForHtml(procedureName)
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

  @Symbol("cloudBeesFlowRunProcedure")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    public DescriptorImpl() {
      load();
    }

    static Map<String, String> getParamsMapFromProcedureParams(String procedureParameters) {
      Map<String, String> paramsMap = new HashMap<>();

      if (procedureParameters == null
          || procedureParameters.isEmpty()
          || procedureParameters.equals("{}")) {
        return paramsMap;
      }

      JSONObject json = JSONObject.fromObject(procedureParameters);

      if (!json.containsKey("procedure")
          || !json.getJSONObject("procedure").containsKey("parameters")) {
        return paramsMap;
      }

      return getParamsMap(
          JSONArray.fromObject(json.getJSONObject("procedure").getString("parameters")),
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

    public FormValidation doCheckProcedureName(
        @QueryParameter String value,
        @QueryParameter boolean validationTrigger,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      if (isSelectItemValidationWrapper(value)) {
        return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
      }
      return Utils.validateValueOnEmpty(value, "Procedure name");
    }

    public FormValidation doCheckProcedureParameters(
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

    public ListBoxModel doFillProcedureNameItems(
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

        m.add("Select procedure", "");

        if (!configuration.isEmpty()
            && !projectName.isEmpty()
            && SelectFieldUtils.checkAllSelectItemsAreNotValidationWrappers(projectName)) {

          Credential overrideCredentialObj =
              overrideCredential ? new Credential(credentialId) : null;
          ElectricFlowClient client =
              ElectricFlowClientFactory.getElectricFlowClient(
                  configuration, overrideCredentialObj, null, true);

          List<String> procedures = client.getProcedures(projectName);

          for (String procedure : procedures) {
            m.add(procedure);
          }
        }

        return m;
      } catch (Exception e) {
        Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
        if (Utils.isEflowAvailable(configuration, overrideCredentialObj)) {
          log.error(
              "Error when fetching values for this parameter - procedure. Error message: "
                  + e.getMessage(),
              e);
          return SelectFieldUtils.getListBoxModelOnException("Select procedure");
        } else {
          return SelectFieldUtils.getListBoxModelOnWrongConf("Select procedure");
        }
      }
    }

    public ListBoxModel doFillProcedureParametersItems(
        @QueryParameter String configuration,
        @QueryParameter boolean overrideCredential,
        @QueryParameter @RelativePath("overrideCredential") String credentialId,
        @QueryParameter String projectName,
        @QueryParameter String procedureName,
        @QueryParameter String procedureParameters,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      try {
        ListBoxModel m = new ListBoxModel();

        if (configuration.isEmpty()
            || projectName.isEmpty()
            || procedureName.isEmpty()
            || checkAnySelectItemsIsValidationWrappers(projectName, procedureName)) {
          m.add("{}");

          return m;
        }

        Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
        ElectricFlowClient client =
            ElectricFlowClientFactory.getElectricFlowClient(
                configuration, overrideCredentialObj, null, true);

        Map<String, String> storedParams = new HashMap<>();

        String deployParametersValue = getSelectItemValue(procedureParameters);

        if (!deployParametersValue.isEmpty() && !"{}".equals(deployParametersValue)) {
          JSONObject json = JSONObject.fromObject(deployParametersValue);
          JSONObject jsonArray = json.getJSONObject("procedure");

          if (procedureName.equals(jsonArray.get("procedureName"))) {
            storedParams = getParamsMapFromProcedureParams(deployParametersValue);
          }
        }

        List<String> parameters = client.getProcedureFormalParameters(projectName, procedureName);
        JSONObject main =
            JSONObject.fromObject(
                "{'procedure':{'procedureName':'" + procedureName + "',   'parameters':[]}}");
        JSONArray ja = main.getJSONObject("procedure").getJSONArray("parameters");

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
              "Error when fetching set of procedure parameters. Error message: " + e.getMessage(),
              e);
          selectItemValidationWrapper =
              new SelectItemValidationWrapper(
                  FieldValidationStatus.ERROR,
                  "Error when fetching set of procedure parameters. Check the Jenkins logs for more details.",
                  "{}");
        } else {
          selectItemValidationWrapper =
              new SelectItemValidationWrapper(
                  FieldValidationStatus.ERROR,
                  "Error when fetching set of procedure parameters. Connection to CloudBees CD Server Failed. Please fix connection information and reload this page.",
                  "{}");
        }
        m.add(selectItemValidationWrapper.getJsonStr());
        return m;
      }
    }

    @Override
    public String getDisplayName() {
      return "CloudBees CD - Run Procedure";
    }

    @Override
    public String getId() {
      return "electricFlowRunProcedure";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    public FormValidation doShowOldValues(
        @QueryParameter("configuration") final String configuration,
        @QueryParameter("projectName") final String projectName,
        @QueryParameter("procedureName") final String procedureName,
        @QueryParameter("procedureParameters") final String procedureParameters,
        @QueryParameter("storedConfiguration") final String storedConfiguration,
        @QueryParameter("storedProjectName") final String storedProjectName,
        @QueryParameter("storedProcedureName") final String storedProcedureName,
        @QueryParameter("storedProcedureParameters") final String storedProcedureParameters,
        @AncestorInPath Item item) {
      if (item == null || !item.hasPermission(Item.CONFIGURE)) {
        return FormValidation.ok();
      }
      String configurationValue = configuration;
      String projectNameValue = getSelectItemValue(projectName);
      String procedureNameValue = getSelectItemValue(procedureName);
      String procedureParametersValue = getSelectItemValue(procedureParameters);

      Map<String, String> procedureParamsMap =
          getParamsMapFromProcedureParams(procedureParametersValue);
      Map<String, String> storedProcedureParamsMap =
          getParamsMapFromProcedureParams(storedProcedureParameters);

      String comparisonTable =
          "<table>"
              + getValidationComparisonHeaderRow()
              + getValidationComparisonRow("Configuration", storedConfiguration, configurationValue)
              + getValidationComparisonRow("Project Name", storedProjectName, projectNameValue)
              + getValidationComparisonRow(
                  "Procedure Name", storedProcedureName, procedureNameValue)
              + getValidationComparisonRowsForExtraParameters(
                  "Procedure Parameters", storedProcedureParamsMap, procedureParamsMap)
              + "</table>";

      if (configurationValue.equals(storedConfiguration)
          && projectNameValue.equals(storedProjectName)
          && procedureNameValue.equals(storedProcedureName)
          && procedureParamsMap.equals(storedProcedureParamsMap)) {
        return FormValidation.okWithMarkup("No changes detected:<br>" + comparisonTable);
      } else {
        return FormValidation.warningWithMarkup("Changes detected:<br>" + comparisonTable);
      }
    }
  }
}
