
// ElectricFlowTriggerRelease.java --
//
// ElectricFlowTriggerRelease.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jenkinsci.plugins.electricflow.Utils.*;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.checkAnySelectItemsIsValidationWrappers;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.getSelectItemValue;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.isSelectItemValidationWrapper;

public class ElectricFlowTriggerRelease
    extends Recorder
    implements SimpleBuildStep
{

    //~ Static fields/initializers ---------------------------------------------

    private static final Log log = LogFactory.getLog(
            ElectricFlowTriggerRelease.class);

    //~ Instance fields --------------------------------------------------------

    private String configuration;
    private String projectName;
    private String releaseName;
    private String startingStage;
    private String parameters;

    //~ Constructors -----------------------------------------------------------

    @DataBoundConstructor public ElectricFlowTriggerRelease() { }

    //~ Methods ----------------------------------------------------------------

    @Override public void perform(
            @Nonnull Run<?, ?>    run,
            @Nonnull FilePath     filePath,
            @Nonnull Launcher     launcher,
            @Nonnull TaskListener taskListener)
        throws InterruptedException, IOException
    {
        JSONObject   release            = JSONObject.fromObject(parameters)
                                                    .getJSONObject("release");
        JSONArray    stages             = JSONArray.fromObject(
                release.getString("stages"));
        JSONArray    pipelineParameters = JSONArray.fromObject(
                release.getString("parameters"));
        List<String> stagesToRun        = new ArrayList<>();

        if (startingStage.isEmpty()) {

            for (int i = 0; i < stages.size(); i++) {
                JSONObject stage = stages.getJSONObject(i);

                if (stage.getBoolean("stageValue")) {
                    stagesToRun.add(stage.getString("stageName"));
                }
            }
        }

        PrintStream logger = taskListener.getLogger();

        try {
            logger.println("Preparing to triggerRelease...");

            EnvReplacer        env      = new EnvReplacer(run, taskListener);
            ElectricFlowClient efClient = new ElectricFlowClient(configuration,
                    env);

            expandParameters(pipelineParameters, env);

            String            releaseResult = efClient.runRelease(projectName,
                    releaseName, stagesToRun, startingStage,
                    pipelineParameters);
            String            summaryHtml   = getSummaryHtml(efClient,
                    releaseResult, pipelineParameters, stagesToRun);
            SummaryTextAction action        = new SummaryTextAction(run,
                    summaryHtml);

            run.addAction(action);
            run.save();
            logger.println("TriggerRelease  result: "
                    + formatJsonOutput(releaseResult));
        }
        catch (Exception e) {
            logger.println(e.getMessage());
            log.error(e.getMessage(), e);
        }
    }

    public String getConfiguration()
    {
        return configuration;
    }

    public String getStoredConfiguration()
    {
        return configuration;
    }

    @Override public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getParameters()
    {
        return parameters;
    }

    public String getStoredParameters()
    {
        return parameters;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public String getStoredProjectName()
    {
        return projectName;
    }

    public String getReleaseName()
    {
        return releaseName;
    }

    public String getStoredReleaseName()
    {
        return releaseName;
    }

    @Override public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.NONE;
    }

    public String getStartingStage()
    {
        return startingStage;
    }

    public String getStoredStartingStage()
    {
        return startingStage;
    }

    public boolean getValidationTrigger() {
        return true;
    }

    private String getSummaryHtml(
            ElectricFlowClient efClient,
            String             releaseResult,
            JSONArray          parameters,
            List<String>       stagesToRun)
    {
        JSONObject flowRuntime   = JSONObject.fromObject(releaseResult)
                                             .getJSONObject("flowRuntime");
        String     pipelineId    = flowRuntime.getString("pipelineId");
        String     flowRuntimeId = flowRuntime.getString("flowRuntimeId");
        String     pipelineName  = flowRuntime.getString("pipelineName");
        String     urlPipeline   = efClient.getElectricFlowUrl()
                + "/flow/#pipeline-run/" + pipelineId
                + "/" + flowRuntimeId;
        String     urlRelease    = efClient.getElectricFlowUrl()
                + "/flow/#releases";
        String     summaryText   = "<h3>ElectricFlow Trigger Release</h3>"
                + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
                + "  <tr>\n"
                + "    <td>Release Name:</td>\n"
                + "    <td><a href='" + HtmlUtils.encodeForHtml(urlRelease) + "'>" + HtmlUtils.encodeForHtml(releaseName)
                + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>Pipeline URL:</td>\n"
                + "    <td><a href='" + HtmlUtils.encodeForHtml(urlPipeline) + "'>" + HtmlUtils.encodeForHtml(urlPipeline)
                + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>Pipeline Name:</td>\n"
                + "    <td><a href='" + HtmlUtils.encodeForHtml(urlPipeline) + "'>" + HtmlUtils.encodeForHtml(pipelineName)
                + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>Project Name:</td>\n"
                + "    <td>" + HtmlUtils.encodeForHtml(projectName) + "</td>    \n"
                + "  </tr>";

        if (!startingStage.isEmpty()) {
            summaryText = summaryText + "  <tr>\n"
                    + "    <td>Starting stage:</td>\n"
                    + "    <td>" + HtmlUtils.encodeForHtml(startingStage) + "</td>    \n"
                    + "  </tr>";
        }

        if (!stagesToRun.isEmpty()) {
            summaryText = getParametersHTML(stagesToRun, summaryText);
        }

        summaryText = getParametersHTML(parameters, summaryText,
                "parameterName", "parameterValue");
        summaryText = summaryText + "</table>";

        return summaryText;
    }

    @DataBoundSetter public void setConfiguration(String configuration)
    {
        this.configuration = configuration;
    }

    @DataBoundSetter public void setParameters(String parameters)
    {
        this.parameters = getSelectItemValue(parameters);
    }

    @DataBoundSetter public void setProjectName(String projectName)
    {
        this.projectName = getSelectItemValue(projectName);;
    }

    @DataBoundSetter public void setReleaseName(String releaseName)
    {
        this.releaseName = getSelectItemValue(releaseName);
    }

    @DataBoundSetter public void setStartingStage(String startingStage)
    {
        this.startingStage = getSelectItemValue(startingStage);
    }

    @DataBoundSetter public void setValidationTrigger(String validationTrigger) {

    }

    //~ Inner Classes ----------------------------------------------------------

    @Extension public static final class DescriptorImpl
        extends BuildStepDescriptor<Publisher>
    {

        public DescriptorImpl()
        {
            load();
        }

        //~ Methods ------------------------------------------------------------

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

        public FormValidation doCheckReleaseName(@QueryParameter String value,
                                                 @QueryParameter boolean validationTrigger) {
            if (isSelectItemValidationWrapper(value)) {
                return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
            }
            return Utils.validateValueOnEmpty(value, "Release name");
        }

        public FormValidation doCheckStartingStage(@QueryParameter String value,
                                                   @QueryParameter boolean validationTrigger
        ) {
            if (isSelectItemValidationWrapper(value)) {
                return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckParameters(@QueryParameter String value,
                                                @QueryParameter boolean validationTrigger
        ) {
            if (isSelectItemValidationWrapper(value)) {
                return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillConfigurationItems()
        {
            return Utils.fillConfigurationItems();
        }

        public ListBoxModel doFillParametersItems(
                @QueryParameter String configuration,
                @QueryParameter String projectName,
                @QueryParameter String releaseName,
                @QueryParameter String parameters) {
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
                    JSONObject json      = JSONObject.fromObject(parametersValue);
                    JSONObject jsonArray = json.getJSONObject("release");

                    if (releaseName.equals(jsonArray.getString("releaseName"))) {
                        storedStagesToRun = getStagesToRunMapFromParams(parametersValue);
                        storedPipelineParams = getPipelineParamsMapFromParams(parametersValue);
                    }
                }

                if (!configuration.isEmpty() && !releaseName.isEmpty()) {
                    ElectricFlowClient client = new ElectricFlowClient(configuration);
                    Release      release            = client.getRelease(
                            configuration, projectName, releaseName);
                    List<String> stages             = release.getStartStages();
                    List<String> pipelineParameters =
                            release.getPipelineParameters();
                    JSONObject   main               = JSONObject.fromObject(
                            "{'release':{'releaseName':'" + releaseName
                                    + "','stages':[], pipelineName:'"
                                    + release.getPipelineName()
                                    + "', 'parameters':[]}}");
                    JSONArray    stagesArray        = main.getJSONObject("release")
                            .getJSONArray("stages");

                    addParametersToJsonAndPreserveStored(stages, stagesArray, "stageName", "stageValue", storedStagesToRun);

                    JSONArray parametersArray = main.getJSONObject("release")
                            .getJSONArray("parameters");

                    addParametersToJsonAndPreserveStored(pipelineParameters, parametersArray, "parameterName", "parameterValue", storedPipelineParams);
                    m.add(main.toString());
                }

                if (m.isEmpty()) {
                    m.add("{}");
                }

                return m;
            } catch (Exception e) {
                ListBoxModel m = new ListBoxModel();
                SelectItemValidationWrapper selectItemValidationWrapper;

                if (Utils.isEflowAvailable(configuration)) {
                    log.error("Error when fetching set of parameters. Error message: " + e.getMessage(), e);
                    selectItemValidationWrapper = new SelectItemValidationWrapper(
                            FieldValidationStatus.ERROR,
                            "Error when fetching set of parameters. Check the Jenkins logs for more details.",
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

        public ListBoxModel doFillProjectNameItems(
                @QueryParameter String configuration) {
            return Utils.getProjects(configuration);
        }

        public ListBoxModel doFillReleaseNameItems(
                @QueryParameter String projectName,
                @QueryParameter String configuration) {
            try {
                ListBoxModel m = new ListBoxModel();

                m.add("Select release", "");

                if (!configuration.isEmpty()
                        && !projectName.isEmpty()
                        && SelectFieldUtils.checkAllSelectItemsAreNotValidationWrappers(projectName)) {

                    ElectricFlowClient client = new ElectricFlowClient(configuration);

                    List<String> releasesList = client.getReleases(configuration,
                            projectName);

                    for (String release : releasesList) {
                        m.add(release);
                    }
                }

                return m;
            } catch (Exception e) {
                if (Utils.isEflowAvailable(configuration)) {
                    log.error("Error when fetching values for this parameter - release. Error message: " + e.getMessage(), e);
                    return SelectFieldUtils.getListBoxModelOnException("Select release");
                } else {
                    return SelectFieldUtils.getListBoxModelOnWrongConf("Select release");
                }
            }
        }

        public ListBoxModel doFillStartingStageItems(
                @QueryParameter String configuration,
                @QueryParameter String projectName,
                @QueryParameter String releaseName)
            throws Exception
        {
            try {
                ListBoxModel m = new ListBoxModel();

                m.add("Select starting stage", "");

                if (projectName.isEmpty()
                        || releaseName.isEmpty()
                        || configuration.isEmpty()
                        || checkAnySelectItemsIsValidationWrappers(projectName, releaseName)) {
                    return m;
                }

                ElectricFlowClient client = new ElectricFlowClient(configuration);

                Release release = client.getRelease(configuration, projectName,
                        releaseName);

                if (release == null) {
                    return m;
                }

                List<String> startStages = release.getStartStages();

                for (String state : startStages) {
                    m.add(state);
                }

                return m;
            } catch (Exception e) {
                if (Utils.isEflowAvailable(configuration)) {
                    log.error("Error when fetching values for this parameter - starting stage. Error message: " + e.getMessage(), e);
                    return SelectFieldUtils.getListBoxModelOnException("Select starting stage");
                } else {
                    return SelectFieldUtils.getListBoxModelOnWrongConf("Select starting stage");

                }
            }

        }

        @Override public String getDisplayName()
        {
            return "ElectricFlow - Trigger Release";
        }

        @Override public String getId()
        {
            return "electricFlowTriggerRelease";
        }

        @Override public boolean isApplicable(
                Class<? extends AbstractProject> aClass)
        {
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
                @QueryParameter("storedParameters") final String storedParameters
        ) {
            String configurationValue = configuration;
            String projectNameValue = getSelectItemValue(projectName);
            String releaseNameValue = getSelectItemValue(releaseName);
            String startingStageValue = getSelectItemValue(startingStage);
            String parametersValue = getSelectItemValue(parameters);

            Map<String, String> stagesToRunMap = getStagesToRunMapFromParams(parametersValue);
            Map<String, String> storedStagesToRunMap = getStagesToRunMapFromParams(storedParameters);

            Map<String, String> pipelineParamsMap = getPipelineParamsMapFromParams(parametersValue);
            Map<String, String> storedPipelineParamsMap = getPipelineParamsMapFromParams(storedParameters);

            String comparisonTable = "<table>"
                    + getValidationComparisonHeaderRow()
                    + getValidationComparisonRow("Configuration", storedConfiguration, configurationValue)
                    + getValidationComparisonRow("Project Name", storedProjectName, projectNameValue)
                    + getValidationComparisonRow("Release Name", storedReleaseName, releaseNameValue)
                    + getValidationComparisonRow("Starting Stage", storedStartingStage, startingStageValue)
                    + getValidationComparisonRowsForExtraParameters("Stages to run", storedStagesToRunMap, stagesToRunMap)
                    + getValidationComparisonRowsForExtraParameters("Pipeline parameters", storedPipelineParamsMap, pipelineParamsMap)
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

        static Map<String, String> getStagesToRunMapFromParams(String deployParameters) {
            Map<String, String> paramsMap = new HashMap<>();

            if (deployParameters == null
                    || deployParameters.isEmpty()
                    || deployParameters.equals("{}")) {
                return paramsMap;
            }

            JSONObject json = JSONObject.fromObject(deployParameters);

            if (!json.containsKey("release")
                    || !json.getJSONObject("release").containsKey("stages")) {
                return paramsMap;
            }

            return getParamsMap(JSONArray.fromObject(json.getJSONObject("release").getString("stages")),
                    "stageName",
                    "stageValue");
        }

        static Map<String, String> getPipelineParamsMapFromParams(String deployParameters) {
            Map<String, String> paramsMap = new HashMap<>();

            if (deployParameters == null
                    || deployParameters.isEmpty()
                    || deployParameters.equals("{}")) {
                return paramsMap;
            }

            JSONObject json = JSONObject.fromObject(deployParameters);

            if (!json.containsKey("release")
                    || !json.getJSONObject("release").containsKey("parameters")) {
                return paramsMap;
            }

            return getParamsMap(JSONArray.fromObject(json.getJSONObject("release").getString("parameters")),
                    "parameterName",
                    "parameterValue");
        }
    }
}
