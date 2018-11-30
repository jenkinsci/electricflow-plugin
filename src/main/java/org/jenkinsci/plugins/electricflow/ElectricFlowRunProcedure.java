
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
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jenkinsci.plugins.electricflow.Utils.addParametersToJson;
import static org.jenkinsci.plugins.electricflow.Utils.formatJsonOutput;

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

    public String getProjectName() {
        return projectName;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public String getProcedureParameters() {
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
                + "    <td><a href='" + jobUrl + "'>" + procedureName + "</a></td>   \n"
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
        this.projectName = projectName;
    }

    @DataBoundSetter
    public void setProcedureName(String procedureName) {
        this.procedureName = procedureName;
    }


    @DataBoundSetter
    public void setProcedureParameters(String procedureParameters) {
        this.procedureParameters = procedureParameters;
    }

    @Extension
    public static final class DescriptorImpl
            extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckConfiguration(
                @QueryParameter String value) {
            return Utils.validateConfiguration(value);
        }

        public FormValidation doCheckProjectName(@QueryParameter String value) {
            return Utils.validateValueOnEmpty(value, "Project name");
        }

        public FormValidation doCheckProcedureName(@QueryParameter String value) {
            return Utils.validateValueOnEmpty(value, "Procedure name");
        }

        public ListBoxModel doFillProcedureNameItems(
                @QueryParameter String projectName,
                @QueryParameter String configuration)
                throws IOException {
            ListBoxModel m = new ListBoxModel();

            m.add("Select procedure", "");

            if (!configuration.isEmpty() && !projectName.isEmpty()) {

                ElectricFlowClient client = new ElectricFlowClient(configuration);

                List<String> procedures = client.getProcedures(projectName);

                for (String procedure : procedures) {
                    m.add(procedure);
                }
            }

            return m;
        }

        public ListBoxModel doFillConfigurationItems() {
            return Utils.fillConfigurationItems();
        }

        public ListBoxModel doFillProcedureParametersItems(
                @QueryParameter String configuration,
                @QueryParameter String projectName,
                @QueryParameter String procedureName,
                @QueryParameter String procedureParameters)
                throws IOException {
            ListBoxModel m = new ListBoxModel();

            if (configuration.isEmpty() || projectName.isEmpty() || procedureName.isEmpty()) {
                m.add("{}");

                return m;
            }

            ElectricFlowClient client = new ElectricFlowClient(configuration);

            // During reload if at least one value filled, return old values
            if (!procedureParameters.isEmpty() && !"{}".equals(procedureParameters)) {
                JSONObject json = JSONObject.fromObject(procedureParameters);
                JSONObject jsonArray = json.getJSONObject("procedure");

                if (procedureName.equals(jsonArray.get("procedureName"))) {
                    m.add(procedureParameters);
                    return m;
                }
            }

            List<String> parameters = client.getProcedureFormalParameters(projectName, procedureName);
            JSONObject main = JSONObject.fromObject(
                    "{'procedure':{'procedureName':'" + procedureName
                            + "',   'parameters':[]}}");
            JSONArray ja = main.getJSONObject("procedure")
                    .getJSONArray("parameters");

            addParametersToJson(parameters, ja, "actualParameterName", "value");
            m.add(main.toString());

            if (m.isEmpty()) {
                m.add("{}");
            }

            return m;
        }

        public ListBoxModel doFillProjectNameItems(
                @QueryParameter String configuration)
                throws IOException {
            return Utils.getProjects(configuration);
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
    }
}
