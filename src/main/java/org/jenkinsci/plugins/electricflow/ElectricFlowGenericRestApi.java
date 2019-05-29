
// ElectricFlowGenericRestApi.java --
//
// ElectricFlowGenericRestApi.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

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

import hudson.util.ListBoxModel;

import jenkins.tasks.SimpleBuildStep;

import static org.jenkinsci.plugins.electricflow.Utils.formatJsonOutput;

public class ElectricFlowGenericRestApi
    extends Recorder
    implements SimpleBuildStep
{

    //~ Instance fields --------------------------------------------------------

    private String     body;
    private String     configuration;
    private List<Pair> parameters;
    private String     urlPath;
    private String     httpMethod;

    //~ Constructors -----------------------------------------------------------

    @DataBoundConstructor public ElectricFlowGenericRestApi(
            List<Pair> parameters)
    {

        if (parameters == null) {
            this.parameters = new ArrayList<>(0);
        }
        else {
            this.parameters = new ArrayList<>(parameters);
        }
    }

    //~ Methods ----------------------------------------------------------------

    @Override public void perform(
            @Nonnull Run<?, ?>    run,
            @Nonnull FilePath     filePath,
            @Nonnull Launcher     launcher,
            @Nonnull TaskListener taskListener)
        throws InterruptedException, IOException
    {

        try {
            ElectricFlowClient efClient    = new ElectricFlowClient(
                    configuration);
            String             result      = efClient.runRestAPI(urlPath,
                    HttpMethod.valueOf(httpMethod), body, parameters);
            String             summaryHtml = getSummaryHtml(efClient, result);
            SummaryTextAction  action      = new SummaryTextAction(run,
                    summaryHtml);

            run.addAction(action);
            run.save();
            taskListener.getLogger()
                        .println(formatJsonOutput(result));
        }
        catch (IOException e) {
            run.setResult(Result.FAILURE);
            throw new IOException(e);
        }
    }

    public String getBody()
    {
        return body;
    }

    public String getConfiguration()
    {
        return configuration;
    }

    public String getHttpMethod()
    {
        return httpMethod;
    }

    public List<Pair> getParameters()
    {
        return parameters;
    }

    @Override public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.NONE;
    }

    private String getSummaryHtml(
            ElectricFlowClient efClient,
            String             result)
        throws IOException
    {
        String url         = efClient.getElectricFlowUrl() + urlPath;
        String summaryText = "<h3>ElectricFlow Generic REST API</h3>"
                + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
                + "  <tr>\n"
                + "    <td style='width:20%;'>URL Path:</td>\n"
                + "    <td><a href='" + HtmlUtils.encodeForHtml(url) + "'>" + HtmlUtils.encodeForHtml(url)
                + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>HTTP Method:</td>\n"
                + "    <td>" + HtmlUtils.encodeForHtml(httpMethod) + "</td>   \n"
                + "  </tr>\n";

        if (!HttpMethod.GET.equals(HttpMethod.valueOf(httpMethod))) {

            if (!parameters.isEmpty()) {
                StringBuilder strBuilder = new StringBuilder(summaryText);

                strBuilder.append("  <tr>\n"
                        + "    <td>&nbsp;<b>Parameters</b></td>\n"
                        + "    <td></td>    \n"
                        + "  </tr>\n");

                for (Pair pair : parameters) {
                    strBuilder.append("  <tr>\n"
                                      + "    <td>&nbsp;&nbsp;&nbsp;&nbsp;")
                              .append(HtmlUtils.encodeForHtml(pair.getKey()))
                              .append(":</td>\n"
                                  + "    <td>")
                              .append(HtmlUtils.encodeForHtml(pair.getValue()))
                              .append("</td>    \n"
                                  + "  </tr>\n");
                }

                summaryText = strBuilder.toString();
            }
            else if (!body.isEmpty()) {
                summaryText = summaryText + "  <tr>\n"
                        + "    <td>Body:</td>\n"
                        + "    <td>" + HtmlUtils.encodeForHtml(formatJsonOutput(body)) + "</td>    \n"
                        + "  </tr>\n";
            }
        }

        summaryText = summaryText + "  <tr>\n"
                + "    <td>Result:</td>\n"
                + "    <td><pre>" + HtmlUtils.encodeForHtml(formatJsonOutput(result))
                + "</pre></td>    \n"
                + "  </tr>\n";
        summaryText = summaryText + "</table>";

        return summaryText;
    }

    public String getUrlPath()
    {
        return urlPath;
    }

    @DataBoundSetter public void setBody(String body)
    {
        this.body = body;
    }

    @DataBoundSetter public void setConfiguration(String configuration)
    {
        this.configuration = configuration;
    }

    @DataBoundSetter public void setHttpMethod(String httpMethod)
    {
        this.httpMethod = httpMethod;
    }

    @DataBoundSetter public void setParameters(List<Pair> parameters)
    {
        this.parameters = parameters;
    }

    @DataBoundSetter public void setUrlPath(String urlPath)
    {
        this.urlPath = urlPath;
    }

    //~ Inner Classes ----------------------------------------------------------

    @Extension public static final class DescriptorImpl
        extends BuildStepDescriptor<Publisher>
    {

        //~ Methods ------------------------------------------------------------

        public ListBoxModel doFillConfigurationItems()
        {
            return Utils.fillConfigurationItems();
        }

        public ListBoxModel doFillHttpMethodItems()
        {
            ListBoxModel m = new ListBoxModel();

            m.add("Select HTTP method", "");

            for (HttpMethod httpMethod : HttpMethod.values()) {
                m.add(httpMethod.name(), httpMethod.name());
            }

            return m;
        }

        @Override public String getDisplayName()
        {
            return "ElectricFlow - Call REST API";
        }

        @Override public boolean isApplicable(
                Class<? extends AbstractProject> aClass)
        {
            return true;
        }
    }
}
