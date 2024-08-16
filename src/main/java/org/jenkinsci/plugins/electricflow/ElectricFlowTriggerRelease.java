// ElectricFlowTriggerRelease.java --
//
// ElectricFlowTriggerRelease.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import static org.jenkinsci.plugins.electricflow.Utils.addParametersToJsonAndPreserveStored;
import static org.jenkinsci.plugins.electricflow.Utils.expandParameters;
import static org.jenkinsci.plugins.electricflow.Utils.formatJsonOutput;
import static org.jenkinsci.plugins.electricflow.Utils.getParametersHTML;
import static org.jenkinsci.plugins.electricflow.Utils.getParamsMap;
import static org.jenkinsci.plugins.electricflow.Utils.getValidationComparisonHeaderRow;
import static org.jenkinsci.plugins.electricflow.Utils.getValidationComparisonRow;
import static org.jenkinsci.plugins.electricflow.Utils.getValidationComparisonRowsForExtraParameters;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.checkAnySelectItemsIsValidationWrappers;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.getSelectItemValue;
import static org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils.isSelectItemValidationWrapper;

import edu.umd.cs.findbugs.annotations.NonNull;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.electricflow.action.CloudBeesCDPBABuildDetails;
import org.jenkinsci.plugins.electricflow.data.CloudBeesFlowBuildData;
import org.jenkinsci.plugins.electricflow.exceptions.FlowRuntimeException;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildAssociationType;
import org.jenkinsci.plugins.electricflow.models.CIBuildDetail.BuildTriggerSource;
import org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs.GetPipelineRuntimeDetailsResponseData;
import org.jenkinsci.plugins.electricflow.ui.FieldValidationStatus;
import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectItemValidationWrapper;
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
    private String stageOptions;

    // ~ Constructors -----------------------------------------------------------

    @DataBoundConstructor
    public ElectricFlowTriggerRelease() {}

    // ~ Methods ----------------------------------------------------------------

    @Override
    public void perform(
            @NonNull Run<?, ?> run,
            @NonNull FilePath filePath,
            @NonNull Launcher launcher,
            @NonNull TaskListener taskListener) {

        EnvReplacer env;
        ElectricFlowClient efClient;
        try {
            env = new EnvReplacer(run, taskListener);
            efClient = ElectricFlowClientFactory.getElectricFlowClient(configuration, overrideCredential, run, env);
        } catch (RuntimeException | InterruptedException | IOException e) {
            log.info("Can't create ElectricFlow client");
            throw new RuntimeException("Can't create ElectricFlowClient object: " + e.getMessage());
        }
        JSONObject release = new JSONObject(); // = JSONObject.fromObject(parameters).getJSONObject("release");
        JSONArray stages; // = JSONArray.fromObject(release.getString("stages"));
        JSONArray pipelineParameters; // = JSONArray.fromObject(release.getString("parameters"));

        if (parameters == null) {
            pipelineParameters = new JSONArray();
            stages = new JSONArray();
        } else {
            release = JSONObject.fromObject(parameters).getJSONObject("release");
            if (release.containsKey("stages")) {
                stages = JSONArray.fromObject(release.getString("stages"));
            } else {
                stages = new JSONArray();
            }
            pipelineParameters = JSONArray.fromObject(release.getString("parameters"));
        }
        List<String> stagesToRun = new ArrayList<>();
        if (this.getReleaseName() == null) {
            if (!release.containsKey("releaseName")) {
                throw new RuntimeException("Can't determine release name from parameters.");
            } else {
                this.setReleaseName(release.getString("releaseName"));
            }
        }
        if (!StringUtils.isEmpty(stageOptions)) {
            if (stageOptions.equalsIgnoreCase("runAllStages")) {
                stagesToRun.addAll(getAllStagesForRelease(efClient));
                this.startingStage = stagesToRun.get(0);
            } else if (stageOptions.equalsIgnoreCase("stagesToRun")) {
                if (stages.size() > 0) {
                    for (int i = 0; i < stages.size(); i++) {
                        JSONObject stage = stages.getJSONObject(i);
                        String stageValue = stage.get("stageValue").toString();
                        if (stageValue.equals("true")) {
                            startingStage = "";
                            stagesToRun.add(stage.getString("stageName"));
                        }
                    }
                }
            }
        } else if (startingStage == null || startingStage.isEmpty()) {
            /* Now we are handling the following logic.
            1. If there are no startingStage AND the stages object is non-empty:
            we are going to read the stages and find the starting stages from array
            2. If there are no startingStage and stages is empty we will try to read this from
            CB CD
            */
            if (stages.size() > 0) {
                for (int i = 0; i < stages.size(); i++) {
                    JSONObject stage = stages.getJSONObject(i);
                    String stageValue = stage.get("stageValue").toString();
                    if (stageValue.equals("true")) {
                        stagesToRun.add(stage.getString("stageName"));
                    }
                }
            } else {
                stagesToRun.addAll(getAllStagesForRelease(efClient));
                this.startingStage = stagesToRun.get(0);
            }
        }

        PrintStream logger = taskListener.getLogger();

        try {
            logger.println("Preparing to triggerRelease...");
            expandParameters(pipelineParameters, env);

            String releaseResult =
                    efClient.runRelease(projectName, releaseName, stagesToRun, startingStage, pipelineParameters);

            JSONObject flowRuntime = JSONObject.fromObject(releaseResult).getJSONObject("flowRuntime");
            String flowRuntimeId = flowRuntime.getString("flowRuntimeId");

            String summaryHtml = getSummaryHtml(efClient, flowRuntime, pipelineParameters, stagesToRun, null);
            SummaryTextAction action = new SummaryTextAction(run, summaryHtml);

            try {
                CloudBeesFlowBuildData cbfdb = new CloudBeesFlowBuildData(run);
                if (log.isDebugEnabled()) {
                    logger.println("CBF Data: " + cbfdb.toJsonObject().toString());
                }

                logger.println("About to call setJenkinsBuildDetails after triggering a Flow Release");

                JSONObject associateResult = efClient.attachCIBuildDetails(new CIBuildDetail(cbfdb, projectName)
                        .setFlowRuntimeId(flowRuntime.getString("flowRuntimeId"))
                        .setAssociationType(BuildAssociationType.TRIGGERED_BY_CI)
                        .setBuildTriggerSource(BuildTriggerSource.CI));

                if (log.isDebugEnabled()) {
                    logger.println("Return from efClient: " + associateResult.toString());
                }
                // Now we're creating the CloudBessCDPBABuildDetails action and adding it to the run.
                CloudBeesCDPBABuildDetails.applyToRuntime(
                        run,
                        configuration,
                        overrideCredential,
                        flowRuntimeId,
                        null,
                        projectName,
                        releaseName,
                        null,
                        BuildTriggerSource.CI,
                        BuildAssociationType.TRIGGERED_BY_CI);
            } catch (RuntimeException ex) {
                log.info("Can't attach CIBuildData to the pipeline run: " + ex.getMessage());
            }

            run.addAction(action);
            run.save();
            logger.println("TriggerRelease  result: " + formatJsonOutput(releaseResult));

            if (runAndWaitOption != null) {
                int checkInterval = runAndWaitOption.getCheckInterval();

                logger.println("Waiting till CloudBees CD pipeline is completed, checking every "
                        + checkInterval
                        + " seconds");

                GetPipelineRuntimeDetailsResponseData responseData;
                do {
                    TimeUnit.SECONDS.sleep(checkInterval);

                    responseData = efClient.getCdPipelineRuntimeDetails(flowRuntimeId);
                    logger.println(responseData.toString());

                    summaryHtml = getSummaryHtml(efClient, flowRuntime, pipelineParameters, stagesToRun, responseData);
                    action = new SummaryTextAction(run, summaryHtml);

                    run.addOrReplaceAction(action);
                    run.save();

                } while (!responseData.isCompleted());

                logger.println("CD pipeline completed with " + responseData.getStatus() + " status");

                if (runAndWaitOption.isDependOnCdJobOutcome()) {
                    Result result = Utils.getCorrespondedCiBuildResult(responseData.getStatus());

                    if (!result.equals(Result.SUCCESS)) {
                        if (runAndWaitOption.isThrowExceptionIfFailed()) {
                            throw new FlowRuntimeException(responseData);
                        }

                        run.setResult(result);
                    }
                }
            }

        } catch (IOException | InterruptedException e) {
            logger.println(e.getMessage());
            log.error(e.getMessage(), e);
            run.setResult(Result.FAILURE);
        }
    }

    private List<String> getAllStagesForRelease(ElectricFlowClient efClient) {
        Release releaseInfo;

        try {
            releaseInfo = efClient.getRelease(this.configuration, this.projectName, this.releaseName);
        } catch (Exception e) {
            log.info("Can't get release information");
            throw new RuntimeException("Can't get release information.");
        }
        return releaseInfo.getStartStages();
    }

    private String getReleaseNameFromResponse(String releaseResult) {
        JSONObject releaseJSON = JSONObject.fromObject(releaseResult).getJSONObject("release");
        return (String) releaseJSON.get("releaseName");
    }

    private String getProjectNameFromResponse(String releaseResult) {
        JSONObject releaseJSON = JSONObject.fromObject(releaseResult).getJSONObject("release");
        return (String) releaseJSON.get("projectName");
    }

    private String getSetJenkinsBuildDetailsUrlBase(String releaseResult) {
        JSONObject releaseJSON = JSONObject.fromObject(releaseResult).getJSONObject("release");
        String retval = "/flowRuntimes/" + releaseJSON.get("releaseName") + "/jenkinsBuildDetails";
        return retval;
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

    public String getStageOptions() {
        return stageOptions;
    }

    @DataBoundSetter
    public void setStageOptions(String stageOptions) {
        this.stageOptions = getSelectItemValue(stageOptions);
    }

    private String getSummaryHtml(
            ElectricFlowClient efClient,
            JSONObject flowRuntime,
            JSONArray parameters,
            List<String> stagesToRun,
            GetPipelineRuntimeDetailsResponseData getPipelineRuntimeDetailsResponseData) {
        String pipelineId = flowRuntime.getString("pipelineId");
        String flowRuntimeId = flowRuntime.getString("flowRuntimeId");
        String pipelineName = flowRuntime.getString("pipelineName");
        String releaseId = flowRuntime.getString("releaseId");
        String pipelineUrl = efClient.getElectricFlowUrl() + "/flow/#pipeline-kanban/" + pipelineId;
        String releasePipelineRunUrl = efClient.getElectricFlowUrl()
                + "/flow/#pipeline-run/"
                + pipelineId
                + "/"
                + flowRuntimeId
                + "/release/"
                + releaseId;
        String releaseUrl = efClient.getElectricFlowUrl() + "/flow/#release-kanban/" + releaseId;
        String summaryText = "<h3>CloudBees CD Trigger Release</h3>"
                + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
                + "  <tr>\n"
                + "    <td>Release Name:</td>\n"
                + "    <td><a href='"
                + HtmlUtils.encodeForHtml(releaseUrl)
                + "'>"
                + HtmlUtils.encodeForHtml(releaseName)
                + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>Release Pipeline Run URL:</td>\n"
                + "    <td><a href='"
                + HtmlUtils.encodeForHtml(releasePipelineRunUrl)
                + "'>"
                + HtmlUtils.encodeForHtml(releasePipelineRunUrl)
                + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>Pipeline Name:</td>\n"
                + "    <td><a href='"
                + HtmlUtils.encodeForHtml(pipelineUrl)
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
            summaryText = summaryText
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

        summaryText = getParametersHTML(parameters, summaryText, "parameterName", "parameterValue");
        if (getPipelineRuntimeDetailsResponseData != null) {
            summaryText = summaryText
                    + "  <tr>\n"
                    + "    <td>CD Pipeline Completed:</td>\n"
                    + "    <td>\n"
                    + getPipelineRuntimeDetailsResponseData.isCompleted()
                    + "    </td>\n"
                    + "  </tr>\n";
            summaryText = summaryText
                    + "  <tr>\n"
                    + "    <td>CD Pipeline Status:</td>\n"
                    + "    <td>\n"
                    + HtmlUtils.encodeForHtml(
                            getPipelineRuntimeDetailsResponseData.getStatus().name())
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
                    JSONArray.fromObject(json.getJSONObject("release").getString("stages")), "stageName", "stageValue");
        }

        static Map<String, String> getPipelineParamsMapFromParams(String deployParameters) {
            Map<String, String> paramsMap = new HashMap<>();

            if (deployParameters == null || deployParameters.isEmpty() || deployParameters.equals("{}")) {
                return paramsMap;
            }

            JSONObject json = JSONObject.fromObject(deployParameters);

            if (!json.containsKey("release") || !json.getJSONObject("release").containsKey("parameters")) {
                return paramsMap;
            }

            return getParamsMap(
                    JSONArray.fromObject(json.getJSONObject("release").getString("parameters")),
                    "parameterName",
                    "parameterValue");
        }

        public FormValidation doCheckConfiguration(
                @QueryParameter String value, @QueryParameter boolean validationTrigger, @AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }
            return Utils.validateConfiguration(value, item);
        }

        public FormValidation doCheckProjectName(
                @QueryParameter String value, @QueryParameter boolean validationTrigger, @AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }
            if (isSelectItemValidationWrapper(value)) {
                return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
            }
            return Utils.validateValueOnEmpty(value, "Project name");
        }

        public FormValidation doCheckReleaseName(
                @QueryParameter String value, @QueryParameter boolean validationTrigger, @AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }
            if (isSelectItemValidationWrapper(value)) {
                return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
            }
            return Utils.validateValueOnEmpty(value, "Release name");
        }

        public FormValidation doCheckStartingStage(
                @QueryParameter String value, @QueryParameter boolean validationTrigger, @AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }
            if (isSelectItemValidationWrapper(value)) {
                return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckStagesToRun(
                @QueryParameter String value, @QueryParameter boolean validationTrigger, @AncestorInPath Item item) {
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }
            if (isSelectItemValidationWrapper(value)) {
                return SelectFieldUtils.getFormValidationBasedOnSelectItemValidationWrapper(value);
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckParameters(
                @QueryParameter String value, @QueryParameter boolean validationTrigger, @AncestorInPath Item item) {
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
                    Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
                    ElectricFlowClient client = ElectricFlowClientFactory.getElectricFlowClient(
                            configuration, overrideCredentialObj, item, null);
                    Release release = client.getRelease(configuration, projectName, releaseName);
                    List<String> stages = release.getStartStages();
                    List<String> pipelineParameters = release.getPipelineParameters();
                    JSONObject main = JSONObject.fromObject("{'release':{'releaseName':'"
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
                if (Utils.isEflowAvailable(configuration, overrideCredentialObj, item)) {
                    log.error("Error when fetching set of parameters. Error message: " + e.getMessage(), e);
                    selectItemValidationWrapper = new SelectItemValidationWrapper(
                            FieldValidationStatus.ERROR,
                            "Error when fetching set of parameters. Check the Jenkins logs for more details.",
                            "{}");
                } else {
                    selectItemValidationWrapper = new SelectItemValidationWrapper(
                            FieldValidationStatus.ERROR,
                            "Error when fetching set of deploy parameters. Connection to CloudBees CD Server Failed. Please fix connection information and reload this page.",
                            "{}");
                }
                m.add(selectItemValidationWrapper.getJsonStr());
                return m;
            }
        }

        public ListBoxModel doFillStagesToRunItems(
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

                String parametersValue = getSelectItemValue(parameters);

                // During reload if at least one value filled, return old values
                if (!parametersValue.isEmpty() && !"{}".equals(parametersValue)) {
                    JSONObject json = JSONObject.fromObject(parametersValue);
                    JSONObject jsonArray = json.getJSONObject("release");

                    if (releaseName.equals(jsonArray.getString("releaseName"))) {
                        storedStagesToRun = getStagesToRunMapFromParams(parametersValue);
                    }
                }

                if (!configuration.isEmpty() && !releaseName.isEmpty()) {
                    Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
                    ElectricFlowClient client = ElectricFlowClientFactory.getElectricFlowClient(
                            configuration, overrideCredentialObj, item, null);
                    Release release = client.getRelease(configuration, projectName, releaseName);

                    List<String> stages = release.getStartStages();
                    JSONObject main = JSONObject.fromObject("{'release':{'releaseName':'"
                            + releaseName
                            + "','stages':[], pipelineName:'"
                            + release.getPipelineName()
                            + "'}}");
                    JSONArray stagesArray = main.getJSONObject("release").getJSONArray("stages");
                    addParametersToJsonAndPreserveStored(
                            stages, stagesArray, "stageName", "stageValue", storedStagesToRun);
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
                if (Utils.isEflowAvailable(configuration, overrideCredentialObj, item)) {
                    log.error("Error when fetching set of parameters. Error message: " + e.getMessage(), e);
                    selectItemValidationWrapper = new SelectItemValidationWrapper(
                            FieldValidationStatus.ERROR,
                            "Error when fetching set of parameters. Check the Jenkins logs for more details.",
                            "{}");
                } else {
                    selectItemValidationWrapper = new SelectItemValidationWrapper(
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

                    Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
                    ElectricFlowClient client = ElectricFlowClientFactory.getElectricFlowClient(
                            configuration, overrideCredentialObj, item, null);

                    // List<String> releasesList = client.getReleases(configuration, projectName);
                    List<String> releasesList = client.getReleaseNames(configuration, projectName);

                    for (String release : releasesList) {
                        m.add(release);
                    }
                }

                return m;
            } catch (Exception e) {
                Credential overrideCredentialObj = overrideCredential ? new Credential(credentialId) : null;
                if (Utils.isEflowAvailable(configuration, overrideCredentialObj, item)) {
                    log.error(
                            "Error when fetching values for this parameter - release. Error message: " + e.getMessage(),
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
                ElectricFlowClient client = ElectricFlowClientFactory.getElectricFlowClient(
                        configuration, overrideCredentialObj, item, null);

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
                if (Utils.isEflowAvailable(configuration, overrideCredentialObj, item)) {
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
            Map<String, String> storedPipelineParamsMap = getPipelineParamsMapFromParams(storedParameters);

            String comparisonTable = "<table>"
                    + getValidationComparisonHeaderRow()
                    + getValidationComparisonRow("Configuration", storedConfiguration, configurationValue)
                    + getValidationComparisonRow("Project Name", storedProjectName, projectNameValue)
                    + getValidationComparisonRow("Release Name", storedReleaseName, releaseNameValue)
                    + getValidationComparisonRow("Starting Stage", storedStartingStage, startingStageValue)
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
}
