package org.jenkinsci.plugins.electricflow.utils;

import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.electricflow.*;
import org.jenkinsci.plugins.electricflow.envvars.VariableInjectionAction;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.jenkinsci.plugins.electricflow.models.CallRestApiModel;
import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;

import java.io.IOException;

import static org.jenkinsci.plugins.electricflow.Utils.formatJsonOutput;

public class CallRestApiUtils {

    public static String getDisplayName() {
        return "CloudBees Flow - Call REST API";
    }

    public static String getFunctionName() {
        return "cloudBeesFlowCallRestApi";
    }

    public static String perform(CallRestApiModel callRestApiModel,
                                 Run run,
                                 TaskListener taskListener) throws IOException {

        try {
            EnvReplacer envReplacer = new EnvReplacer(run, taskListener);
            ElectricFlowClient efClient = ElectricFlowClientFactory
                    .getElectricFlowClient(
                            callRestApiModel.getConfiguration(),
                            callRestApiModel.getOverrideCredential(),
                            run,
                            envReplacer,
                            false);

            String result = efClient.runRestAPI(
                    callRestApiModel.getUrlPath(),
                    HttpMethod.valueOf(callRestApiModel.getHttpMethod()),
                    callRestApiModel.getBody(),
                    callRestApiModel.getParameters(envReplacer)
            );
            String summaryHtml = getSummaryHtml(
                    callRestApiModel,
                    envReplacer,
                    efClient,
                    result
            );

            SummaryTextAction action = new SummaryTextAction(run, summaryHtml);
            run.addAction(action);
            run.save();

            taskListener.getLogger().println("Call REST API result: '" + formatJsonOutput(result) + "'");

            if (callRestApiModel.isEnvVarNameForResultSet()) {
                String envVarForResult = callRestApiModel.getEnvVarNameForResult();
                taskListener.getLogger().println("Setting environment variable " + envVarForResult + "='" + result + "'");
                run.addAction(new VariableInjectionAction(envVarForResult, result));
            }

            return result;

        } catch (IOException | InterruptedException e) {
            run.setResult(Result.FAILURE);
            throw new IOException(e);
        }
    }

    private static String getSummaryHtml(CallRestApiModel callRestApiModel,
                                         EnvReplacer envReplacer,
                                         ElectricFlowClient efClient,
                                         String result) throws IOException {
        Configuration configuration = Utils.getConfigurationByName(callRestApiModel.getConfiguration());
        String urlPath = callRestApiModel.getUrlPath();
        urlPath = urlPath.startsWith("/") ? urlPath : "/" + urlPath;
        String url = efClient.getElectricFlowUrl() + configuration.getElectricFlowApiVersion() + urlPath;

        String summaryText = "<h3>CloudBees Flow Generic REST API</h3>"
                + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
                + "  <tr>\n"
                + "    <td style='width:20%;'>URL Path:</td>\n"
                + "    <td><a href='" + HtmlUtils.encodeForHtml(url) + "'>" + HtmlUtils.encodeForHtml(url)
                + "</a></td>   \n"
                + "  </tr>\n"
                + "  <tr>\n"
                + "    <td>HTTP Method:</td>\n"
                + "    <td>" + HtmlUtils.encodeForHtml(callRestApiModel.getHttpMethod()) + "</td>   \n"
                + "  </tr>\n";

        if (!HttpMethod.GET.equals(HttpMethod.valueOf(callRestApiModel.getHttpMethod()))) {

            if (!callRestApiModel.getParameters().isEmpty()) {
                StringBuilder strBuilder = new StringBuilder(summaryText);

                strBuilder.append("  <tr>\n"
                        + "    <td>&nbsp;<b>Parameters</b></td>\n"
                        + "    <td></td>    \n"
                        + "  </tr>\n");

                for (Pair pair : callRestApiModel.getParameters(envReplacer)) {
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
            } else if (!callRestApiModel.getBody().isEmpty()) {
                summaryText = summaryText + "  <tr>\n"
                        + "    <td>Body:</td>\n"
                        + "    <td>" + HtmlUtils.encodeForHtml(formatJsonOutput(callRestApiModel.getBody())) + "</td>    \n"
                        + "  </tr>\n";
            }
        }

        if (callRestApiModel.isEnvVarNameForResultSet()) {
            summaryText = summaryText + "  <tr>\n"
                    + "    <td>Environment variable name for storing result:</td>\n"
                    + "    <td>" + HtmlUtils.encodeForHtml(callRestApiModel.getEnvVarNameForResult()) + "</td>    \n"
                    + "  </tr>\n";
        }

        summaryText = summaryText + "  <tr>\n"
                + "    <td>Result:</td>\n"
                + "    <td><pre>" + HtmlUtils.encodeForHtml(formatJsonOutput(result))
                + "</pre></td>    \n"
                + "  </tr>\n";
        summaryText = summaryText + "</table>";

        return summaryText;
    }

    public static ListBoxModel doFillConfigurationItems(Item item) {
        if (item == null || !item.hasPermission(Item.CONFIGURE)) {
            return new ListBoxModel();
        }
        return Utils.fillConfigurationItems();
    }

    public static ListBoxModel doFillHttpMethodItems(Item item) {
        if (item == null || !item.hasPermission(Item.CONFIGURE)) {
            return new ListBoxModel();
        }
        ListBoxModel m = new ListBoxModel();

        m.add("Select HTTP method", "");

        for (HttpMethod httpMethod : HttpMethod.values()) {
            m.add(httpMethod.name(), httpMethod.name());
        }

        return m;
    }
}
