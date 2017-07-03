
// ElectricFlowStartRelease.java --
//
// ElectricFlowStartRelease.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

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

import static org.jenkinsci.plugins.electricflow.Utils.addParametersToJson;
import static org.jenkinsci.plugins.electricflow.Utils.expandParameters;
import static org.jenkinsci.plugins.electricflow.Utils.formatJsonOutput;
import static org.jenkinsci.plugins.electricflow.Utils.getParametersHTML;

public class ElectricFlowStartRelease
    extends Recorder
    implements SimpleBuildStep
{

    //~ Static fields/initializers ---------------------------------------------

    private static final Log log = LogFactory.getLog(
            ElectricFlowStartRelease.class);

    //~ Instance fields --------------------------------------------------------

    private String configuration;
    private String projectName;
    private String releaseName;
    private String startingStage;
    private String parameters;

    //~ Constructors -----------------------------------------------------------

    @DataBoundConstructor public ElectricFlowStartRelease() { }

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
            logger.println("Preparing to startRelease...");

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
            logger.println("StartRelease  result: "
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

    @Override public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getParameters()
    {
        return parameters;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public String getReleaseName()
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
        String     summaryText   = "<h3>ElectricFlow Start Release</h3>"
                + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
                + "  <tr>\n"
                + "    <td>Release Name:</td>\n"
                + "    <td><a href='" + urlRelease + "'>" + releaseName
                + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>Pipeline URL:</td>\n"
                + "    <td><a href='" + urlPipeline + "'>" + urlPipeline
                + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>Pipeline Name:</td>\n"
                + "    <td><a href='" + urlPipeline + "'>" + pipelineName
                + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>Project Name:</td>\n"
                + "    <td>" + projectName + "</td>    \n"
                + "  </tr>";

        if (!startingStage.isEmpty()) {
            summaryText = summaryText + "  <tr>\n"
                    + "    <td>Starting stage:</td>\n"
                    + "    <td>" + startingStage + "</td>    \n"
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
        this.parameters = parameters;
    }

    @DataBoundSetter public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    @DataBoundSetter public void setReleaseName(String releaseName)
    {
        this.releaseName = releaseName;
    }

    @DataBoundSetter public void setStartingStage(String startingStage)
    {
        this.startingStage = startingStage;
    }

    //~ Inner Classes ----------------------------------------------------------

    @Extension public static final class DescriptorImpl
        extends BuildStepDescriptor<Publisher>
    {

        //~ Instance fields ----------------------------------------------------

        private ElectricFlowClient client;

        //~ Constructors -------------------------------------------------------

        public DescriptorImpl()
        {
            load();
        }

        //~ Methods ------------------------------------------------------------

        public FormValidation doCheckConfiguration(
                @QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Configuration");
        }

        public FormValidation doCheckProjectName(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Project name");
        }

        public FormValidation doCheckReleaseName(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Release name");
        }

        public ListBoxModel doFillConfigurationItems()
        {
            return Utils.fillConfigurationItems();
        }

        public ListBoxModel doFillParametersItems(
                @QueryParameter String configuration,
                @QueryParameter String projectName,
                @QueryParameter String releaseName,
                @QueryParameter String parameters)
            throws Exception
        {
            ListBoxModel m = new ListBoxModel();

            if (projectName.isEmpty() || releaseName.isEmpty()
                    || configuration.isEmpty()) {
                m.add("{}");

                return m;
            }

            // During reload if at least one value filled, return old values
            if (!parameters.isEmpty() && !"{}".equals(parameters)) {
                JSONObject json      = JSONObject.fromObject(parameters);
                JSONObject jsonArray = json.getJSONObject("release");

                if (releaseName.equals(jsonArray.getString("releaseName"))) {
                    m.add(parameters);

                    return m;
                }
            }

            if (!configuration.isEmpty() && !releaseName.isEmpty()) {
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

                addParametersToJson(stages, stagesArray, "stageName",
                    "stageValue");

                JSONArray parametersArray = main.getJSONObject("release")
                                                .getJSONArray("parameters");

                addParametersToJson(pipelineParameters, parametersArray,
                    "parameterName", "parameterValue");
                m.add(main.toString());
            }

            if (m.isEmpty()) {
                m.add("{}");
            }

            return m;
        }

        public ListBoxModel doFillProjectNameItems(
                @QueryParameter String configuration)
            throws IOException
        {
            return Utils.getProjects(configuration);
        }

        public ListBoxModel doFillReleaseNameItems(
                @QueryParameter String projectName,
                @QueryParameter String configuration)
            throws Exception
        {
            ListBoxModel m = new ListBoxModel();

            m.add("Select release", "");

            if (!configuration.isEmpty() && !projectName.isEmpty()) {

                if (client == null) {
                    client = new ElectricFlowClient(configuration);
                }

                List<String> releasesList = client.getReleases(configuration,
                        projectName);

                for (String release : releasesList) {
                    m.add(release);
                }
            }

            return m;
        }

        public ListBoxModel doFillStartingStageItems(
                @QueryParameter String configuration,
                @QueryParameter String projectName,
                @QueryParameter String releaseName)
            throws Exception
        {
            ListBoxModel m = new ListBoxModel();

            m.add("Select starting stage", "");

            if (projectName.isEmpty() || releaseName.isEmpty()
                    || configuration.isEmpty()) {
                return m;
            }

            if (client == null) {
                client = new ElectricFlowClient(configuration);
            }

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
        }

        @Override public String getDisplayName()
        {
            return "ElectricFlow - Start Release";
        }

        @Override public String getId()
        {
            return "electricFlowRunRelease";
        }

        @Override public boolean isApplicable(
                Class<? extends AbstractProject> aClass)
        {
            return true;
        }
    }
}
