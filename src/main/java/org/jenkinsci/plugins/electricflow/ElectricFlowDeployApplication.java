
// ElectricFlowDeployApplication.java --
//
// ElectricFlowDeployApplication.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
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
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import static org.jenkinsci.plugins.electricflow.Utils.*;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.getSelectItemValue;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.isSelectItemValidationWrapper;

public class ElectricFlowDeployApplication
    extends Recorder
    implements SimpleBuildStep
{

    //~ Static fields/initializers ---------------------------------------------

    private static final Log log = LogFactory.getLog(ElectricFlowDeployApplication.class);

    //~ Instance fields --------------------------------------------------------

    private String configuration;
    private String projectName;
    private String applicationName;
    private String applicationProcessName;
    private String environmentName;
    private String deployParameters;

    //~ Constructors -----------------------------------------------------------

    @DataBoundConstructor public ElectricFlowDeployApplication() { }

    //~ Methods ----------------------------------------------------------------

    @Override public void perform(
            @Nonnull Run<?, ?>    run,
            @Nonnull FilePath     filePath,
            @Nonnull Launcher     launcher,
            @Nonnull TaskListener taskListener)
        throws InterruptedException, IOException
    {
        boolean isSuccess = runProcess(run, taskListener);
        if (!isSuccess) {
            run.setResult(Result.FAILURE);
        }
    }

    private boolean runProcess(
            @Nonnull Run<?, ?>    run,
            @Nonnull TaskListener taskListener)
    {
        ElectricFlowClient efClient = new ElectricFlowClient(configuration);
        PrintStream        logger   = taskListener.getLogger();

        logger.println("Project name: "
                + projectName
                + ", Application name: " + applicationName
                + ", Application process name: " + applicationProcessName
                + ", Environment name: " + environmentName);

        JSONObject runProcess = JSONObject.fromObject(deployParameters)
                                          .getJSONObject("runProcess");
        JSONArray  parameter  = JSONArray.fromObject(runProcess.getString(
                    "parameter"));

        try {
            logger.println("Preparing to run process...");

            String     result  = efClient.runProcess(projectName,
                    applicationName, applicationProcessName, environmentName,
                    parameter);
            JSONObject process = efClient.getProcess(projectName,
                    applicationName, applicationProcessName);

            if (process == null || process.isEmpty()) {
                return false;
            }

            String              processId = process.getJSONObject("process")
                                                   .getString("processId");
            Map<String, String> args      = new HashMap<>();

            args.put("applicationName", applicationName);
            args.put("processName", applicationProcessName);
            args.put("processId", processId);
            args.put("result", result);

            String            summaryHtml = getSummaryHtml(efClient, parameter,
                    args);
            SummaryTextAction action      = new SummaryTextAction(run,
                    summaryHtml);

            run.addAction(action);
            run.save();
            logger.println("Deploy application result: "
                    + formatJsonOutput(result));
        }
        catch (Exception e) {
            logger.println(e.getMessage());
            log.error(e.getMessage(), e);

            return false;
        }

        return true;
    }

    public String getApplicationName()
    {
        return applicationName;
    }

    public String getStoredApplicationName() {
        return applicationName;
    }

    public String getApplicationProcessName()
    {
        return applicationProcessName;
    }

    public String getStoredApplicationProcessName() {
        return applicationProcessName;
    }

    public String getConfiguration()
    {
        return configuration;
    }

    public String getStoredConfiguration()
    {
        return configuration;
    }

    public String getDeployParameters()
    {
        return deployParameters;
    }

    public String getStoredDeployParameters()
    {
        return deployParameters;
    }

    public String getEnvironmentName()
    {
        return environmentName;
    }

    public String getStoredEnvironmentName() {
        return environmentName;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public String getStoredProjectName() {
        return projectName;
    }

    public boolean getValidationTrigger() {
        return true;
    }

    @Override public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.NONE;
    }

    private String getSummaryHtml(
            ElectricFlowClient  configuration,
            JSONArray           parameters,
            Map<String, String> args)
    {
        String result          = args.get("result");
        String applicationName = args.get("applicationName");
        String processId       = args.get("processId");
        String jobId           = JSONObject.fromObject(result)
                                           .getString("jobId");
        String applicationUrl  = configuration.getElectricFlowUrl()
                + "/flow/#applications/applications";
        String deployRunUrl    = configuration.getElectricFlowUrl()
                + "/flow/#applications/" + processId + "/" + jobId
                + "/runningProcess";
        String summaryText     = "<h3>ElectricFlow Deploy Application</h3>"
                + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
                + "  <tr>\n"
                + "    <td>Application Name:</td>\n"
                + "    <td><a href='" + HtmlUtils.encodeForHtml(applicationUrl) + "'>" + HtmlUtils.encodeForHtml(applicationName)
                + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>Deploy run URL:</td>\n"
                + "    <td><a href='" + HtmlUtils.encodeForHtml(deployRunUrl) + "'>" + HtmlUtils.encodeForHtml(deployRunUrl)
                + "</a></td>   \n"
                + "  </tr>";

        summaryText = Utils.getParametersHTML(parameters, summaryText,
                "actualParameterName", "value");
        summaryText = summaryText + "</table>";

        return summaryText;
    }

    @DataBoundSetter public void setApplicationName(String applicationName)
    {
        this.applicationName = getSelectItemValue(applicationName);
    }

    @DataBoundSetter public void setApplicationProcessName(
            String applicationProcessName)
    {
        this.applicationProcessName = getSelectItemValue(applicationProcessName);
    }

    @DataBoundSetter public void setConfiguration(String configuration)
    {
        this.configuration = configuration;
    }

    @DataBoundSetter public void setDeployParameters(String deployParameters)
    {
        this.deployParameters = getSelectItemValue(deployParameters);
    }

    @DataBoundSetter public void setEnvironmentName(String environmentName)
    {
        this.environmentName = getSelectItemValue(environmentName);
    }

    @DataBoundSetter public void setProjectName(String projectName)
    {
        this.projectName = getSelectItemValue(projectName);
    }

    @DataBoundSetter public void setValidationTrigger(String validationTrigger) {

    }

    //~ Inner Classes ----------------------------------------------------------

    @Extension public static final class DescriptorImpl
        extends BuildStepDescriptor<Publisher>
    {

        //~ Instance fields ----------------------------------------------------

        //~ Constructors -------------------------------------------------------

        public DescriptorImpl()
        {
            load();
        }

        //~ Methods ------------------------------------------------------------

        public FormValidation doCheckConfiguration(@QueryParameter String value,
                                                   @QueryParameter boolean validationTrigger,
                                                   @AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }
            return Utils.validateConfiguration(value);
        }

        public FormValidation doCheckDeployParameters(@QueryParameter String value,
                                                      @QueryParameter boolean validationTrigger,
                                                      @AncestorInPath Item item
                                                      ) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }
            if (isSelectItemValidationWrapper(value)) {
                return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckProjectName(@QueryParameter String value,
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

        public FormValidation doCheckApplicationName(@QueryParameter String value,
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

        public FormValidation doCheckApplicationProcessName(@QueryParameter String value,
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

        public FormValidation doCheckEnvironmentName(@QueryParameter String value,
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
                    ElectricFlowClient client = new ElectricFlowClient(configuration);

                    List<String> applications = client.getApplications(projectName);

                    for (String application : applications) {
                        m.add(application);
                    }
                }

                return m;
            } catch (Exception e) {
                if (Utils.isEflowAvailable(configuration)) {
                    log.error("Error when fetching values for this parameter - application. Error message: " + e.getMessage(), e);
                    return SelectFieldUtils.getListBoxModelOnException("Select application");
                } else {
                    return SelectFieldUtils.getListBoxModelOnWrongConf("Select application");

                }
            }
        }

        public ListBoxModel doFillApplicationProcessNameItems(
                @QueryParameter String configuration,
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
                        && SelectFieldUtils.checkAllSelectItemsAreNotValidationWrappers(projectName, applicationName)) {
                    ElectricFlowClient client = new ElectricFlowClient(configuration);
                    List<String> processes = client.getProcesses(projectName,
                            applicationName);

                    for (String process : processes) {
                        m.add(process);
                    }
                }

                return m;
            } catch (Exception e) {
                if (Utils.isEflowAvailable(configuration)) {
                    log.error("Error when fetching values for this parameter - application process. Error message: " + e.getMessage(), e);
                    return SelectFieldUtils.getListBoxModelOnException("Select application process");
                } else {
                    return SelectFieldUtils.getListBoxModelOnWrongConf("Select application process");

                }
            }
        }

        public ListBoxModel doFillConfigurationItems(@AncestorInPath Item item)
        {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            return Utils.fillConfigurationItems();
        }

        public ListBoxModel doFillDeployParametersItems(
                @QueryParameter String configuration,
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
                        || !SelectFieldUtils.checkAllSelectItemsAreNotValidationWrappers(projectName, applicationName, applicationProcessName)) {
                    m.add("{}");

                    return m;
                }

                ElectricFlowClient client = new ElectricFlowClient(configuration);

                Map<String, String> storedParams = new HashMap<>();

                String deployParametersValue = getSelectItemValue(deployParameters);

                // During reload if at least one value filled, return old values
                if (!deployParametersValue.isEmpty() && !"{}".equals(deployParametersValue)) {
                    JSONObject json      = JSONObject.fromObject(deployParametersValue);
                    JSONObject jsonArray = json.getJSONObject("runProcess");

                    if (applicationName.equals(jsonArray.get("applicationName"))
                            && applicationProcessName.equals(
                            jsonArray.get("applicationProcessName"))) {
                        storedParams = getParamsMapFromDeployParams(deployParametersValue);
                    }
                }

                List<String> parameters = client.getFormalParameters(projectName,
                        applicationName, applicationProcessName);
                JSONObject   main       = JSONObject.fromObject(
                        "{'runProcess':{'applicationName':'" + applicationName
                                + "', 'applicationProcessName':'"
                                + applicationProcessName
                                + "',   'parameter':[]}}");
                JSONArray    ja         = main.getJSONObject("runProcess")
                        .getJSONArray("parameter");

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
                    log.error("Error when fetching set of deploy parameters. Error message: " + e.getMessage(), e);
                    selectItemValidationWrapper = new SelectItemValidationWrapper(
                            FieldValidationStatus.ERROR,
                            "Error when fetching set of deploy parameters. Check the Jenkins logs for more details.",
                            "{}"
                    );
                } else {
                    selectItemValidationWrapper = new SelectItemValidationWrapper(
                            FieldValidationStatus.ERROR,
                            "Error when fetching set of deploy parameters. Connection to Electric Flow Server Failed. Please fix connection information and reload this page.",
                            "{}"
                    );
                }
                m.add(selectItemValidationWrapper.getJsonStr());
                return m;
            }
        }

        public ListBoxModel doFillEnvironmentNameItems(
                @QueryParameter String configuration,
                @QueryParameter String projectName,
                @AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            try {
                ListBoxModel m = new ListBoxModel();

                m.add("Select environment", "");

                if (!configuration.isEmpty()
                        && !projectName.isEmpty()
                        && SelectFieldUtils.checkAllSelectItemsAreNotValidationWrappers(projectName)) {
                    ElectricFlowClient client = new ElectricFlowClient(configuration);
                    List<String> environments = client.getEnvironments(projectName);

                    for (String environment : environments) {
                        m.add(environment);
                    }
                }

                return m;
            } catch (Exception e) {
                if (Utils.isEflowAvailable(configuration)) {
                    log.error("Error when fetching values for this parameter - environment. Error message: " + e.getMessage(), e);
                    return SelectFieldUtils.getListBoxModelOnException("Select environment");
                } else {
                    return SelectFieldUtils.getListBoxModelOnWrongConf("Select environment");

                }
            }
        }

        public ListBoxModel doFillProjectNameItems(
                @QueryParameter String configuration,
                @AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            return Utils.getProjects(configuration);
        }

        @Override public String getDisplayName()
        {
            return "ElectricFlow - Deploy Application";
        }

        @Override public String getId()
        {
            return "electricFlowDeployApplication";
        }

        @Override public boolean isApplicable(
                Class<? extends AbstractProject> aClass)
        {
            return true;
        }

        public FormValidation doShowOldValues(
                @QueryParameter("configuration") final String configuration,
                @QueryParameter("projectName") final String projectName,
                @QueryParameter("applicationName") final String applicationName,
                @QueryParameter("applicationProcessName") final String applicationProcessName,
                @QueryParameter("environmentName") final String environmentName,
                @QueryParameter("deployParameters") final String deployParameters,
                @QueryParameter("storedConfiguration") final String storedConfiguration,
                @QueryParameter("storedProjectName") final String storedProjectName,
                @QueryParameter("storedApplicationName") final String storedApplicationName,
                @QueryParameter("storedApplicationProcessName") final String storedApplicationProcessName,
                @QueryParameter("storedEnvironmentName") final String storedEnvironmentName,
                @QueryParameter("storedDeployParameters") final String storedDeployParameters,
                @AncestorInPath Item item
        ) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }
            String configurationValue = configuration;
            String projectNameValue = getSelectItemValue(projectName);
            String applicationNameValue = getSelectItemValue(applicationName);
            String applicationProcessNameValue = getSelectItemValue(applicationProcessName);
            String environmentNameValue = getSelectItemValue(environmentName);
            String deployParametersValue = getSelectItemValue(deployParameters);

            Map<String, String> deployParamsMap = getParamsMapFromDeployParams(deployParametersValue);
            Map<String, String> storedDeployParamsMap = getParamsMapFromDeployParams(storedDeployParameters);

            String comparisonTable = "<table>"
                    + getValidationComparisonHeaderRow()
                    + getValidationComparisonRow("Configuration", storedConfiguration, configurationValue)
                    + getValidationComparisonRow("Project Name", storedProjectName, projectNameValue)
                    + getValidationComparisonRow("Application Name", storedApplicationName, applicationNameValue)
                    + getValidationComparisonRow("Application Process Name", storedApplicationProcessName, applicationProcessNameValue)
                    + getValidationComparisonRow("Environment Name", storedEnvironmentName, environmentNameValue)
                    + getValidationComparisonRowsForExtraParameters("Deploy Parameters", storedDeployParamsMap, deployParamsMap)
                    + "</table>";

            if (configurationValue.equals(storedConfiguration)
                    && projectNameValue.equals(storedProjectName)
                    && applicationNameValue.equals(storedApplicationName)
                    && applicationProcessNameValue.equals(storedApplicationProcessName)
                    && environmentNameValue.equals(storedEnvironmentName)
                    && deployParamsMap.equals(storedDeployParamsMap)) {
                return FormValidation.okWithMarkup("No changes detected:<br>" + comparisonTable);
            } else {
                return FormValidation.warningWithMarkup("Changes detected:<br>" + comparisonTable);
            }
        }

        static Map<String, String> getParamsMapFromDeployParams(String deployParameters) {
            Map<String, String> paramsMap = new HashMap<>();

            if (deployParameters == null
                    || deployParameters.isEmpty()
                    || deployParameters.equals("{}")) {
                return paramsMap;
            }

            JSONObject json = JSONObject.fromObject(deployParameters);

            if (!json.containsKey("runProcess")
                    || !json.getJSONObject("runProcess").containsKey("parameter")) {
                return paramsMap;
            }

            return getParamsMap(JSONArray.fromObject(json.getJSONObject("runProcess").getString("parameter")),
                    "actualParameterName",
                    "value");
        }
    }
}
