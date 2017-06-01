
// Utils.java --
//
// Utils.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import org.codehaus.jackson.map.ObjectMapper;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import jenkins.model.GlobalConfiguration;

public class Utils
{

    //~ Methods ----------------------------------------------------------------

    public static String encodeURL(String url)
        throws UnsupportedEncodingException
    {
        return URLEncoder.encode(url, "UTF-8")
                         .replaceAll("\\+", "%20");
    }

    public static void expandParameters(
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

    public static ListBoxModel fillConfigurationItems()
    {
        ListBoxModel m = new ListBoxModel();

        m.add("Select configuration", "");

        for (Configuration cred : getConfigurations()) {
            m.add(cred.getConfigurationName(), cred.getConfigurationName());
        }

        return m;
    }

    public static String formatJsonOutput(String result)
        throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        Object       json   = mapper.readValue(result, Object.class);

        return mapper.writerWithDefaultPrettyPrinter()
                     .writeValueAsString(json);
    }

    public static FormValidation validateValueOnEmpty(
            String value,
            String fieldName)
    {

        if (!value.isEmpty()) {
            return FormValidation.ok();
        }
        else {
            return FormValidation.warning(fieldName
                    + " field should not be empty.");
        }
    }

    public static Configuration getConfigurationByName(String name)
    {

        for (Configuration cred : getConfigurations()) {

            if (cred.getConfigurationName()
                    .equals(name)) {
                return cred;
            }
        }

        return null;
    }

    public static List<Configuration> getConfigurations()
    {
        ElectricFlowGlobalConfiguration cred = GlobalConfiguration.all()
                                                                  .get(
                                                                      ElectricFlowGlobalConfiguration.class);

        if (cred != null && cred.efConfigurations != null) {
            return cred.efConfigurations;
        }

        return new ArrayList<>();
    }

    public static String getParametersHTML(
            List<String> parameters,
            String       summaryText)
    {

        if (!parameters.isEmpty()) {
            StringBuilder strBuilder = new StringBuilder(summaryText);

            strBuilder.append("  <tr>\n"
                              + "    <td>&nbsp;<b>Stages to run</b></td>\n")
                      .append("    <td></td>    \n")
                      .append("  </tr>\n");

            for (String param : parameters) {
                strBuilder.append("  <tr>\n"
                                  + "    <td>&nbsp;&nbsp;&nbsp;&nbsp;")
                          .append(param)
                          .append("</td>\n")
                          .append("  </tr>\n");
            }

            summaryText = strBuilder.toString();
        }

        return summaryText;
    }

    public static String getParametersHTML(
            JSONArray parameters,
            String    summaryText,
            String    parameterName,
            String    parameterValue)
    {

        if (!parameters.isEmpty()) {
            StringBuilder strBuilder = new StringBuilder(summaryText);

            strBuilder.append("  <tr>\n"
                    + "    <td>&nbsp;<b>Parameters</b></td>\n"
                    + "    <td></td>    \n"
                    + "  </tr>\n");

            for (int i = 0; i < parameters.size(); i++) {
                JSONObject json  = parameters.getJSONObject(i);
                String     name  = json.getString(parameterName);
                String     value = json.getString(parameterValue);

                strBuilder.append("  <tr>\n"
                                  + "    <td>&nbsp;&nbsp;&nbsp;&nbsp;")
                          .append(name)
                          .append(":</td>\n"
                              + "    <td>")
                          .append(value)
                          .append("</td>    \n"
                              + "  </tr>\n");
            }

            summaryText = strBuilder.toString();
        }

        return summaryText;
    }

    public static void addParametersToJson(
            List<String> pipelineParameters,
            JSONArray    parametersArray,
            String       parameterName,
            String       parameterValue)
    {

        for (String param : pipelineParameters) {
            JSONObject mainJson = new JSONObject();

            mainJson.put(parameterName, param);
            mainJson.put(parameterValue, "");
            parametersArray.add(mainJson);
        }
    }

    public static ListBoxModel getPipelines(
            String configuration,
            String projectName,
            String pipelineName,
            Log    log)
        throws IOException
    {
        ListBoxModel m = new ListBoxModel();

        m.add("Select pipeline", "");

        if (!projectName.isEmpty() && !configuration.isEmpty()) {

//            Configuration cred            = getConfigurationByName(
//                    configuration);
//            String        userName        = cred.getElectricFlowUser();
//            String        userPassword    = cred.getElectricFlowPassword();
//            String        electricFlowUrl = cred.getElectricFlowUrl();
//
//            if (userName.isEmpty() || userPassword.isEmpty()
//                    || projectName.isEmpty()) {
//                log.warn(
//                        "User name / password / project name should not be empty.");
//
//                return m;
//            }
            ElectricFlowClient efClient        = new ElectricFlowClient(
                    configuration);
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
                                                  .getString("pipelineName");

                m.add(gotPipelineName, gotPipelineName);
            }
        }

        return m;
    }

    public static ListBoxModel getProjects(
            String configuration,
            Log    log)
        throws IOException
    {
        ListBoxModel m = new ListBoxModel();

        m.add("Select project", "");

        if (!configuration.isEmpty()) {
            ElectricFlowClient efClient       = new ElectricFlowClient(
                    configuration);
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
}
