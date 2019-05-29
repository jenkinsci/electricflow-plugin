
// ElectricFlowRunProcedure.java --
//
// ElectricFlowRunProcedure.java is part of ElectricCommander.
//
// Copyright (c) 2005-2018 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.plugins.electricflow.ui.FieldValidationStatus;
import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectItemValidationWrapper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jenkinsci.plugins.electricflow.Utils.*;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.checkAnySelectItemsIsValidationWrappers;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.getSelectItemValue;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.isSelectItemValidationWrapper;

public class ElectricFlowRunProcedure
        extends Recorder
        implements SimpleBuildStep {

    private static final Log log = LogFactory.getLog(
            ElectricFlowRunProcedure.class);

    private String configuration;
    private String projectName;
    private String procedureName;
    private String procedureParameters;

    @DataBoundConstructor
    public ElectricFlowRunProcedure() {
    }

    @Override
    public void perform(
            @Nonnull Run<?, ?> run,
            @Nonnull FilePath filePath,
            @Nonnull Launcher launcher,
            @Nonnull TaskListener taskListener)
            throws InterruptedException, IOException {
        boolean isSuccess = runProcedure(run, taskListener);
        if (!isSuccess) {
            run.setResult(Result.FAILURE);
        }
    }

    private boolean runProcedure(
            @Nonnull Run<?, ?> run,
            @Nonnull TaskListener taskListener) {
        ElectricFlowClient efClient = new ElectricFlowClient(configuration);
        PrintStream logger = taskListener.getLogger();

        logger.println("Project name: " + projectName + ", Procedure name: " + procedureName);

        JSONObject procedure = JSONObject.fromObject(procedureParameters).getJSONObject("procedure");
        JSONArray parameter = JSONArray.fromObject(procedure.getString("parameters"));

        try {
            logger.println("Preparing to run procedure...");

            String result = efClient.runProcedure(projectName, procedureName, parameter);

            Map<String, String> args = new HashMap<>();

            args.put("procedureName", procedureName);
            args.put("result", result);

            String summaryHtml = getSummaryHtml(efClient, parameter,
                    args);
            SummaryTextAction action = new SummaryTextAction(run,
                    summaryHtml);

            run.addAction(action);
            run.save();
            logger.println("Run procedure result: "
                    + formatJsonOutput(result));
        } catch (Exception e) {
            logger.println(e.getMessage());
            log.error(e.getMessage(), e);

            return false;
        }

        return true;
    }

    public String getConfiguration() {
        return configuration;
    }

    public String getStoredConfiguration() {
        return configuration;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getStoredProjectName() {
        return projectName;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public String getStoredProcedureName() {
        return procedureName;
    }

    public String getProcedureParameters() {
        return procedureParameters;
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
            Map<String, String> args) {
        String result = args.get("result");
        String procedureName = args.get("procedureName");
        String jobId = JSONObject.fromObject(result)
                .getString("jobId");
        String jobUrl = configuration.getElectricFlowUrl()
                + "/commander/link/jobDetails/jobs/" + jobId;
        String summaryText = "<h3>ElectricFlow Run Procedure</h3>"
                + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
                + "  <tr>\n"
                + "    <td>Procedure Name:</td>\n"
                + "    <td><a href='" + HtmlUtils.encodeForHtml(jobUrl) + "'>" + HtmlUtils.encodeForHtml(procedureName) + "</a></td>   \n"
                + "  </tr>";

        summaryText = Utils.getParametersHTML(parameters, summaryText,
                "actualParameterName", "value");
        summaryText = summaryText + "</table>";

        return summaryText;
    }

    @DataBoundSetter
    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    @DataBoundSetter
    public void setProjectName(String projectName) {
        this.projectName = getSelectItemValue(projectName);
    }

    @DataBoundSetter
    public void setProcedureName(String procedureName) {
        this.procedureName = getSelectItemValue(procedureName);
    }


    @DataBoundSetter
    public void setProcedureParameters(String procedureParameters) {
        this.procedureParameters = getSelectItemValue(procedureParameters);
    }

    @Extension
    public static final class DescriptorImpl
            extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckConfiguration(@QueryParameter String value,
                                                   @QueryParameter boolean validationTrigger) {
            return Utils.validateConfiguration(value);
        }

        public FormValidation doCheckProjectName(@QueryParameter String value,
                                                 @QueryParameter boolean validationTrigger) {
            if (isSelectItemValidationWrapper(value)) {
                return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
            }
            return Utils.validateValueOnEmpty(value, "Project name");
        }

        public FormValidation doCheckProcedureName(@QueryParameter String value,
                                                   @QueryParameter boolean validationTrigger) {
            if (isSelectItemValidationWrapper(value)) {
                return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
            }
            return Utils.validateValueOnEmpty(value, "Procedure name");
        }

        public FormValidation doCheckProcedureParameters(@QueryParameter String value,
                                                         @QueryParameter boolean validationTrigger) {
            if (isSelectItemValidationWrapper(value)) {
                return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillConfigurationItems() {
            return Utils.fillConfigurationItems();
        }

        public ListBoxModel doFillProjectNameItems(@QueryParameter String configuration) {
            return Utils.getProjects(configuration);
        }

        public ListBoxModel doFillProcedureNameItems(
                @QueryParameter String projectName,
                @QueryParameter String configuration) {
            try {
                ListBoxModel m = new ListBoxModel();

                m.add("Select procedure", "");

                if (!configuration.isEmpty()
                        && !projectName.isEmpty()
                        && SelectFieldUtils.checkAllSelectItemsAreNotValidationWrappers(projectName)) {

                    ElectricFlowClient client = new ElectricFlowClient(configuration);

                    List<String> procedures = client.getProcedures(projectName);

                    for (String procedure : procedures) {
                        m.add(procedure);
                    }
                }

                return m;
            } catch (Exception e) {
                if (Utils.isEflowAvailable(configuration)) {
                    log.error("Error when fetching values for this parameter - procedure. Error message: " + e.getMessage(), e);
                    return SelectFieldUtils.getListBoxModelOnException("Select procedure");
                } else {
                    return SelectFieldUtils.getListBoxModelOnWrongConf("Select procedure");

                }
            }
        }

        public ListBoxModel doFillProcedureParametersItems(
                @QueryParameter String configuration,
                @QueryParameter String projectName,
                @QueryParameter String procedureName,
                @QueryParameter String procedureParameters) {
            try {
                ListBoxModel m = new ListBoxModel();

                if (configuration.isEmpty()
                        || projectName.isEmpty()
                        || procedureName.isEmpty()
                        || checkAnySelectItemsIsValidationWrappers(projectName, procedureName)) {
                    m.add("{}");

                    return m;
                }

                ElectricFlowClient client = new ElectricFlowClient(configuration);

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
                JSONObject main = JSONObject.fromObject(
                        "{'procedure':{'procedureName':'" + procedureName
                                + "',   'parameters':[]}}");
                JSONArray ja = main.getJSONObject("procedure")
                        .getJSONArray("parameters");

                addParametersToJsonAndPreserveStored(parameters, ja, "actualParameterName", "value", storedParams);
                m.add(main.toString());

                if (m.isEmpty()) {
                    m.add("{}");
                }

                return m;
            } catch (Exception e) {
                ListBoxModel m = new ListBoxModel();
                SelectItemValidationWrapper selectItemValidationWrapper;

                if (Utils.isEflowAvailable(configuration)) {
                    log.error("Error when fetching set of procedure parameters. Error message: " + e.getMessage(), e);
                    selectItemValidationWrapper = new SelectItemValidationWrapper(
                            FieldValidationStatus.ERROR,
                            "Error when fetching set of procedure parameters. Check the Jenkins logs for more details.",
                            "{}"
                    );
                } else {
                    selectItemValidationWrapper = new SelectItemValidationWrapper(
                            FieldValidationStatus.ERROR,
                            "Error when fetching set of procedure parameters. Connection to Electric Flow Server Failed. Please fix connection information and reload this page.",
                            "{}"
                    );
                }
                m.add(selectItemValidationWrapper.getJsonStr());
                return m;
            }
        }

        @Override
        public String getDisplayName() {
            return "ElectricFlow - Run Procedure";
        }

        @Override
        public String getId() {
            return "electricFlowRunProcedure";
        }

        @Override
        public boolean isApplicable(
                Class<? extends AbstractProject> aClass) {
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
                @QueryParameter("storedProcedureParameters") final String storedProcedureParameters
        ) {
            String configurationValue = configuration;
            String projectNameValue = getSelectItemValue(projectName);
            String procedureNameValue = getSelectItemValue(procedureName);
            String procedureParametersValue = getSelectItemValue(procedureParameters);

            Map<String, String> procedureParamsMap = getParamsMapFromProcedureParams(procedureParametersValue);
            Map<String, String> storedProcedureParamsMap = getParamsMapFromProcedureParams(storedProcedureParameters);

            String comparisonTable = "<table>"
                    + getValidationComparisonHeaderRow()
                    + getValidationComparisonRow("Configuration", storedConfiguration, configurationValue)
                    + getValidationComparisonRow("Project Name", storedProjectName, projectNameValue)
                    + getValidationComparisonRow("Procedure Name", storedProcedureName, procedureNameValue)
                    + getValidationComparisonRowsForExtraParameters("Procedure Parameters", storedProcedureParamsMap, procedureParamsMap)
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

            return getParamsMap(JSONArray.fromObject(json.getJSONObject("procedure").getString("parameters")),
                    "actualParameterName",
                    "value");
        }
    }
}
