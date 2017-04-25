
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
import java.util.Map;

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
import hudson.model.Run;
import hudson.model.TaskListener;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

/**
 * Sample {@link Builder}.
 *
 * <p>When the user configures the project and enables this builder is invoked
 * and a new {@link ElectricFlowPipelinePublisher} is created. The created
 * instance is persisted to the project configuration XML by using XStream, so
 * this allows you to use instance fields (like {@link #projectName}) to
 * remember the configuration.</p>
 *
 * <p>When a build is performed, the {@link #perform} method will be invoked.
 * </p>
 */
public class ElectricFlowPipelinePublisher
    extends Recorder
// implements SimpleBuildStep
{

    //~ Static fields/initializers ---------------------------------------------

    private static final Log log = LogFactory.getLog(
            ElectricFlowPipelinePublisher.class);

    //~ Instance fields --------------------------------------------------------

    private String    projectName;
    private String    pipelineName;
    private String    credential;
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
        Configuration cred            = getDescriptor().getCredentialByName(
                this.credential);
        String        electricFlowUrl = cred.getElectricFlowUrl();
        String        userName        = cred.getElectricFlowUser();
        String        userPassword    = cred.getElectricFlowPassword();

        listener.getLogger()
                .println("Url: " + electricFlowUrl + ", Project name: "
                    + projectName + ", Pipeline name: " + pipelineName);

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
            listener.getLogger()
                    .println("Preparing to run pipeline...");

            ElectricFlowClient efClient       = new ElectricFlowClient(
                    electricFlowUrl, userName, userPassword);
            String             pipelineResult;
            JSONArray          parameters     = getPipelineParameters();

            if (parameters.isEmpty()) {
                pipelineResult = efClient.runPipeline(projectName,
                        pipelineName);
            }
            else {
                pipelineResult = efClient.runPipeline(projectName, pipelineName,
                        parameters);
            }

            JSONObject    flowRuntime     = (JSONObject) JSONObject.fromObject(
                                                                       pipelineResult)
                                                                   .get(
                                                                       "flowRuntime");
            String        pipelineId      = (String) flowRuntime.get(
                    "pipelineId");
            String        flowRuntimeId   = (String) flowRuntime.get(
                    "flowRuntimeId");
            String        flowRuntimeName = (String) flowRuntime.get(
                    "flowRuntimeName");
            StringBuilder url             = new StringBuilder(
                    efClient.getElectricFlowUrl());

            url.append("/flow/#pipeline-run/")
               .append(pipelineId)
               .append("/")
               .append(flowRuntimeId);

            SummaryTextAction action = new SummaryTextAction(build,
                    "<hr><h2>ElectricFlow Pipeline</h2> <a href='" + url.toString()
                        + "'>" + flowRuntimeName + "</a>");

            build.addAction(action);
            build.save();
            listener.getLogger()
                    .println("Pipeline result: " + pipelineResult);
        }
        catch (Exception e) {
            listener.getLogger()
                    .println(e.getMessage());
            log.error(e.getMessage(), e);

            return false;
        }

        return true;
    }

    public void performdf(
            @Nonnull Run<?, ?>    build,
            @Nonnull FilePath     filePath,
            @Nonnull Launcher     launcher,
            @Nonnull TaskListener taskListener)
        throws InterruptedException, IOException
    {
//        SummaryTextAction action = new SummaryTextAction(build,
//                "<hr><h2>ElectricFlow</h2> <a href='https://google.com'>EF.com</a>");
//
//        build.addAction(action);
//        build.save();
    }

    private String replaceVars(
            String              publishText,
            Map<String, String> vars)
    {

        for (Map.Entry<String, String> var : vars.entrySet()) {
            String key   = String.format("${%s}", var.getKey());
            String value = var.getValue();

            publishText = publishText.replace(key, value);
        }

        return publishText;
    }

    public JSONArray getAdditionalOption()
    {
        return additionalOption;
    }

    public String getAddParam()
    {
        return addParam;
    }

    public String getCredential()
    {
        return credential;
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

    @DataBoundSetter public void setAdditionalOption(
            JSONArray additionalOption)
    {
        this.additionalOption = additionalOption;
    }

    @DataBoundSetter public void setAddParam(String addParam)
    {
        this.addParam = addParam;
    }

    @DataBoundSetter public void setCredential(String credential)
    {
        this.credential = credential;
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

        /**
         * To persist global configuration information, simply store it in a
         * field and call save().
         *
         * <p>If you don't want fields to be persisted, use {@code transient}.
         * </p>
         */
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

        public FormValidation doCheckCredential(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Credential");
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
                @QueryParameter String credential,
                @QueryParameter String pipelineName,
                @QueryParameter String addParam)
            throws Exception
        {
            ListBoxModel m = new ListBoxModel();

            if (credential.isEmpty() || pipelineName.isEmpty()) {
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

            if (!credential.isEmpty() && !pipelineName.isEmpty()) {
                Configuration      cred       = this.getCredentialByName(
                        credential);
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

        public ListBoxModel doFillCredentialItems()
        {
            return Utils.fillCredentialItems();
        }

        public ListBoxModel doFillPipelineNameItems(
                @QueryParameter String projectName,
                @QueryParameter String credential,
                @QueryParameter String pipelineName)
            throws Exception
        {
            ListBoxModel m = new ListBoxModel();

            m.add("Select pipeline", "");

            if (!projectName.isEmpty() && !credential.isEmpty()) {
                Configuration cred            = this.getCredentialByName(
                        credential);
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
                @QueryParameter String credential)
            throws Exception
        {
            ListBoxModel m = new ListBoxModel();

            m.add("Select project", "");

            if (!credential.isEmpty()) {
                Configuration cred            = this.getCredentialByName(
                        credential);
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

        public Configuration getCredentialByName(String name)
        {
            return Utils.getCredentialByName(name);
        }

        public List<Configuration> getCredentials()
        {
            return Utils.getCredentials();
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

        @Override public String getHelpFile()
        {
            return "ASDFASDFASDFASDFASDFASDFASDF";
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
