
// ElectricFlowPipelinePublisher.java --
//
// ElectricFlowPipelinePublisher.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
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

public class ElectricFlowPipelinePublisher
    extends Recorder
    implements SimpleBuildStep
{

    //~ Static fields/initializers ---------------------------------------------

    private static final Log log = LogFactory.getLog(
            ElectricFlowPipelinePublisher.class);

    //~ Instance fields --------------------------------------------------------

    private String    projectName;
    private String    pipelineName;
    private String    configuration;
    private String    addParam;
    private JSONArray additionalOption;

    //~ Constructors -----------------------------------------------------------

    @DataBoundConstructor public ElectricFlowPipelinePublisher() { }

    //~ Methods ----------------------------------------------------------------

    @Override public boolean perform(
            AbstractBuild build,
            Launcher      launcher,
            BuildListener listener)
    {
        return runPipeline(build, listener, null);
    }

    @Override public void perform(
            @Nonnull Run<?, ?>    run,
            @Nonnull FilePath     filePath,
            @Nonnull Launcher     launcher,
            @Nonnull TaskListener taskListener)
        throws InterruptedException, IOException
    {
        boolean result = runPipeline(run, null, taskListener);

        if (!result) {
            run.setResult(Result.FAILURE);
        }
    }

    private void expandParameters(
            JSONArray   parameters,
            EnvReplacer env)
    {

        for (Object jsonObject : parameters) {
            JSONObject json           = (JSONObject) jsonObject;
            String     parameterValue = (String) json.get("parameterValue");
            String     expandValue    = env.expandEnv(parameterValue);

            json.put("parameterValue", expandValue);
        }
    }

    private void logListener(
            BuildListener buildListener,
            TaskListener  taskListener,
            String        log)
    {

        if (buildListener != null) {
            buildListener.getLogger()
                         .println(log);
        }
        else if (taskListener != null) {
            taskListener.getLogger()
                        .print(log);
        }
    }

    private boolean runPipeline(
            Run           run,
            BuildListener buildListener,
            TaskListener  taskListener)
    {
        Configuration cred = getDescriptor().getConfigurationByName(
                this.configuration);

        if (cred == null) {
            logListener(buildListener, taskListener,
                "Configuration name' " + this.configuration
                    + "' doesn't exist. ");

            return false;
        }

        String electricFlowUrl = cred.getElectricFlowUrl();
        String userName        = cred.getElectricFlowUser();
        String userPassword    = cred.getElectricFlowPassword();

        logListener(buildListener, taskListener,
            "Url: " + electricFlowUrl + ", Project name: " + projectName
                + ", Pipeline name: " + pipelineName);

        // exp starts here
        JSONObject obj   = new JSONObject();
        JSONArray  arr   = new JSONArray();
        JSONObject inner = new JSONObject();

        inner.put("actualParameterName", "test");
        inner.put("value", "value");
        arr.add(inner);
        obj.put("actualParameter", arr);
        obj.put("pipelineName", pipelineName);
        obj.put("projectName", projectName);

        if (log.isDebugEnabled()) {
            log.debug("Json object: " + obj.toString());
        }

        // exp ends here
        try {
            ElectricFlowClient efClient       = new ElectricFlowClient(
                    electricFlowUrl, userName, userPassword);
            List<String>       paramsResponse =
                efClient.getPipelineFormalParameters(pipelineName);

            if (log.isDebugEnabled()) {
                log.debug("FormalParameters are: "
                        + paramsResponse.toString());
            }
        }
        catch (Exception e) {
            log.error("Error occurred during formal parameters fetch: "
                    + e.getMessage(), e);

            return false;
        }

        try {
            logListener(buildListener, taskListener,
                "Preparing to run pipeline...");

            ElectricFlowClient efClient       = new ElectricFlowClient(
                    electricFlowUrl, userName, userPassword);
            String             pipelineResult;
            JSONArray          parameters     = getPipelineParameters();

            if (parameters.isEmpty()) {
                pipelineResult = efClient.runPipeline(projectName,
                        pipelineName);
            }
            else {
                EnvReplacer env = new EnvReplacer(run, taskListener);

                expandParameters(parameters, env);
                pipelineResult = efClient.runPipeline(projectName, pipelineName,
                        parameters);
            }

            String            summaryHtml = getSummaryHtml(efClient,
                    pipelineResult, parameters);
            SummaryTextAction action      = new SummaryTextAction(run,
                    summaryHtml);

            run.addAction(action);
            run.save();
            logListener(buildListener, taskListener,
                "Pipeline result: " + pipelineResult);
        }
        catch (Exception e) {
            logListener(buildListener, taskListener, e.getMessage());
            log.error(e.getMessage(), e);

            return false;
        }

        return true;
    }

    public JSONArray getAdditionalOption()
    {
        return additionalOption;
    }

    public String getAddParam()
    {
        return addParam;
    }

    public String getConfiguration()
    {
        return configuration;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getPipelineName()
    {
        return pipelineName;
    }

    private JSONArray getPipelineParameters()
    {

        if (addParam != null) {
            Object pipelineJsonObject = JSONObject.fromObject(addParam)
                                                  .get("pipeline");

            if (pipelineJsonObject instanceof String) {
                String stringParam = (String) pipelineJsonObject;

                if (!stringParam.isEmpty()) {
                    JSONObject intrinsicObject    = (JSONObject) JSONArray
                            .fromObject(stringParam)
                            .get(0);
                    JSONArray  pipelineParameters = (JSONArray)
                        intrinsicObject.get("parameters");

                    if (!pipelineParameters.isEmpty()) {
                        return pipelineParameters;
                    }
                }
            }

            JSONArray pipelineJsonParameters = (JSONArray) JSONObject
                    .fromObject(addParam)
                    .get("parameters");

            if (pipelineJsonParameters != null
                    && !pipelineJsonParameters.isEmpty()) {
                return pipelineJsonParameters;
            }
        }

        return new JSONArray();
    }

    public String getProjectName()
    {
        return projectName;
    }

    @Override public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.NONE;
    }

    private String getSummaryHtml(
            ElectricFlowClient efClient,
            String             pipelineResult,
            JSONArray          parameters)
    {
        JSONObject flowRuntime   = (JSONObject) JSONObject.fromObject(
                                                              pipelineResult)
                                                          .get("flowRuntime");
        String     pipelineId    = (String) flowRuntime.get("pipelineId");
        String     flowRuntimeId = (String) flowRuntime.get("flowRuntimeId");
        String     url           = efClient.getElectricFlowUrl()
                + "/flow/#pipeline-run/" + pipelineId
                + "/" + flowRuntimeId;
        String     summaryText   = "<h3>ElectricFlow Run Pipeline</h3>"
                + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
                + "  <tr>\n"
                + "    <td>Pipeline URL:</td>\n"
                + "    <td><a href='" + url + "'>" + url + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>Pipeline Name:</td>\n"
                + "    <td><a href='" + url + "'>" + pipelineName
                + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>Project Name:</td>\n"
                + "    <td>" + projectName + "</td>    \n"
                + "  </tr>";

        if (!parameters.isEmpty()) {
            StringBuilder strBuilder = new StringBuilder(summaryText);

            strBuilder.append("  <tr>\n"
                    + "    <td>&nbsp;<b>Parameters</b></td>\n"
                    + "    <td></td>    \n"
                    + "  </tr>\n");

            for (Object jsonObject : parameters) {
                JSONObject json           = (JSONObject) jsonObject;
                String     parameterName  = (String) json.get("parameterName");
                String     parameterValue = (String) json.get("parameterValue");

                strBuilder.append("  <tr>\n"
                                  + "    <td>&nbsp;&nbsp;&nbsp;&nbsp;")
                          .append(parameterName)
                          .append(":</td>\n"
                              + "    <td>")
                          .append(parameterValue)
                          .append("</td>    \n"
                              + "  </tr>\n");
            }

            summaryText = strBuilder.toString();
        }

        summaryText = summaryText + "</table>";

        return summaryText;
    }

    @DataBoundSetter public void setAdditionalOption(
            JSONArray additionalOption)
    {
        this.additionalOption = additionalOption;
    }

    @DataBoundSetter public void setAddParam(String addParam)
    {
        this.addParam = addParam;
    }

    @DataBoundSetter public void setConfiguration(String configuration)
    {
        this.configuration = configuration;
    }

    @DataBoundSetter public void setPipelineName(String pipelineName)
    {
        this.pipelineName = pipelineName;
    }

    @DataBoundSetter public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>See .jelly for the actual HTML fragment for the configuration
     * screen.</p>
     */
    @Extension // This indicates to Jenkins that this is an implementation of
               // an extension point.
    public static final class DescriptorImpl
        extends BuildStepDescriptor<Publisher>
    {

        //~ Static fields/initializers -----------------------------------------

        private static final Log log = LogFactory.getLog(DescriptorImpl.class);

        //~ Constructors -------------------------------------------------------

        /**
         * In order to load the persisted global configuration, you have to call
         * load() in the constructor.
         */
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

        public FormValidation doCheckPipelineName(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Pipeline name");
        }

        public FormValidation doCheckProjectName(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Project name");
        }

        public ListBoxModel doFillAddParamItems(
                @QueryParameter String configuration,
                @QueryParameter String pipelineName,
                @QueryParameter String addParam)
            throws Exception
        {
            ListBoxModel m = new ListBoxModel();

            if (configuration.isEmpty() || pipelineName.isEmpty()) {
                m.add("{}");

                return m;
            }

            // During reload if at least one value filled, return old values
            if (!addParam.isEmpty() && !"{}".equals(addParam)) {
                JSONObject json      = JSONObject.fromObject(addParam);
                JSONObject jsonArray = (JSONObject) JSONArray.fromObject(
                                                                 json.get(
                                                                     "pipeline"))
                                                             .get(0);

                if (pipelineName.equals(jsonArray.get("pipelineName"))) {
                    m.add(addParam);

                    return m;
                }
            }

            if (!configuration.isEmpty() && !pipelineName.isEmpty()) {
                Configuration      cred       = this.getConfigurationByName(
                        configuration);
                ElectricFlowClient efClient   = new ElectricFlowClient(
                        cred.getElectricFlowUrl(), cred.getElectricFlowUser(),
                        cred.getElectricFlowPassword());
                List<String>       parameters =
                    efClient.getPipelineFormalParameters(pipelineName);
                JSONObject         main       = JSONObject.fromObject(
                        "{'pipeline':[{'pipelineName':'" + pipelineName
                            + "','parameters':[]}]}");
                JSONArray          ja         = (JSONArray)
                    ((JSONObject) ((JSONArray) main.get("pipeline")).get(0))
                        .get("parameters");

                for (String param : parameters) {
                    JSONObject jo = new JSONObject();

                    jo.put("parameterName", param);
                    jo.put("parameterValue", "");
                    ja.add(jo);
                }

                m.add(main.toString());
            }

            if (m.isEmpty()) {
                m.add("{}");
            }

            return m;
        }

        public ListBoxModel doFillConfigurationItems()
        {
            return Utils.fillConfigurationItems();
        }

        public ListBoxModel doFillPipelineNameItems(
                @QueryParameter String projectName,
                @QueryParameter String configuration,
                @QueryParameter String pipelineName)
            throws Exception
        {
            ListBoxModel m = new ListBoxModel();

            m.add("Select pipeline", "");

            if (!projectName.isEmpty() && !configuration.isEmpty()) {
                Configuration cred            = this.getConfigurationByName(
                        configuration);
                String        userName        = cred.getElectricFlowUser();
                String        userPassword    = cred.getElectricFlowPassword();
                String        electricFlowUrl = cred.getElectricFlowUrl();

                if (userName.isEmpty() || userPassword.isEmpty()
                        || projectName.isEmpty()) {
                    log.warn(
                        "User name / password / project name should not be empty.");

                    return m;
                }

                ElectricFlowClient efClient        = new ElectricFlowClient(
                        electricFlowUrl, userName, userPassword);
                String             pipelinesString = efClient.getPipelines(
                        projectName);

                if (log.isDebugEnabled()) {
                    log.debug("Got pipelines: " + pipelinesString);
                }

                JSONObject jsonObject;

                try {
                    jsonObject = JSONObject.fromObject(pipelinesString);
                }
                catch (Exception e) {

                    if (log.isDebugEnabled()) {
                        log.debug("Malformed JSON" + pipelinesString);
                    }

                    log.error(e.getMessage(), e);

                    return m;
                }

                JSONArray pipelines = new JSONArray();

                try {

                    if (!jsonObject.isEmpty()) {
                        pipelines = jsonObject.getJSONArray("pipeline");
                    }
                }
                catch (Exception e) {
                    log.error(e.getMessage(), e);

                    return m;
                }

                for (int i = 0; i < pipelines.size(); i++) {
                    String gotPipelineName = pipelines.getJSONObject(i)
                                                      .getString(
                                                          "pipelineName");

                    m.add(gotPipelineName, gotPipelineName);
                }
            }

            return m;
        }

        public ListBoxModel doFillProjectNameItems(
                @QueryParameter String configuration)
            throws Exception
        {
            ListBoxModel m = new ListBoxModel();

            m.add("Select project", "");

            if (!configuration.isEmpty()) {
                Configuration cred            = this.getConfigurationByName(
                        configuration);
                String        userName        = cred.getElectricFlowUser();
                String        userPassword    = cred.getElectricFlowPassword();
                String        electricFlowUrl = cred.getElectricFlowUrl();

                if (userName.isEmpty() || userPassword.isEmpty()) {
                    log.warn("User name / password should not be empty");

                    return m;
                }

                ElectricFlowClient efClient       = new ElectricFlowClient(
                        electricFlowUrl, userName, userPassword);
                String             projectsString = efClient.getProjects();
                JSONObject         jsonObject     = JSONObject.fromObject(
                        projectsString);
                JSONArray          projects       = jsonObject.getJSONArray(
                        "project");

                for (int i = 0; i < projects.size(); i++) {

                    if (projects.getJSONObject(i)
                                .has("pluginKey")) {
                        continue;
                    }

                    String gotProjectName = projects.getJSONObject(i)
                                                    .getString("projectName");

                    m.add(gotProjectName, gotProjectName);
                }
            }

            return m;
        }

        @Override public void doHelp(
                StaplerRequest  req,
                StaplerResponse rsp)
            throws IOException, ServletException
        {
            super.doHelp(req, rsp);
        }

        public FormValidation doTestConnection()
        {
            return FormValidation.ok("Success");
        }

        public Configuration getConfigurationByName(String name)
        {
            return Utils.getConfigurationByName(name);
        }

        public List<Configuration> getConfigurations()
        {
            return Utils.getConfigurations();
        }

        /**
         * This human readable name is used in the configuration screen.
         *
         * @return  this human readable name is used in the configuration
         *          screen.
         */
        @Override public String getDisplayName()
        {
            return "ElectricFlow - Run Pipeline";
        }

        @Override public String getId()
        {
            return "electricFlowSettings";
        }

        @Override public boolean isApplicable(
                Class<? extends AbstractProject> aClass)
        {

            // Indicates that this builder can be used with all kinds of
            // project types
            return true;
        }
    }
}
