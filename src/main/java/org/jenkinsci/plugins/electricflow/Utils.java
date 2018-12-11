
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
import java.util.*;

import org.apache.commons.logging.Log;

import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import jenkins.model.GlobalConfiguration;
import org.jenkinsci.plugins.electricflow.ui.FieldValidationStatus;
import org.jenkinsci.plugins.electricflow.ui.SelectFieldUtils;
import org.jenkinsci.plugins.electricflow.ui.SelectItemValidationWrapper;

public class Utils
{

    private static final Log log = LogFactory.getLog(Utils.class);

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

    public static LinkedHashMap<String, String> getParamsMap(JSONArray paramsJsonArray,
                                                             String parameterName,
                                                             String parameterValue) {
        LinkedHashMap<String, String> paramsMap = new LinkedHashMap<>();

        for (int i = 0; i < paramsJsonArray.size(); i++) {
            JSONObject param = paramsJsonArray.getJSONObject(i);
            paramsMap.put(param.getString(parameterName), param.getString(parameterValue));
        }

        return paramsMap;
    }

    public static void addParametersToJsonAndPreserveStored(
            List<String> pipelineParameters,
            JSONArray parametersArray,
            String parameterName,
            String parameterValue,
            Map<String, String> storedParamsMap) {

        for (String param : pipelineParameters) {
            JSONObject mainJson = new JSONObject();

            mainJson.put(parameterName, param);
            mainJson.put(parameterValue, storedParamsMap.getOrDefault(param, ""));
            parametersArray.add(mainJson);
        }
    }

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

    public static FormValidation validateConfiguration(String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return FormValidation.warning("Configuration field should not be empty.");
        }

        try {
            new ElectricFlowClient(configuration).testConnection();
        } catch (IOException e) {
            log.error("Connection to Electric Flow Server Failed. Please check the connection information. Error message: " + e.getMessage(), e);
            return FormValidation.error("Connection to Electric Flow Server Failed. Please check the connection information. Error message: " + e.getMessage());
        }

        return FormValidation.ok();
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

    public static boolean isEflowAvailable(String configuration) {
        try {
            new ElectricFlowClient(configuration).testConnection();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static ListBoxModel getProjects(String configuration) {
        try {
            ListBoxModel m = new ListBoxModel();

            m.add("Select project", new SelectItemValidationWrapper(
                    FieldValidationStatus.WARN,
                    "Project name field should not be empty.",
                    ""
            ).getJsonStr());

            if (!configuration.isEmpty()) {
                ElectricFlowClient efClient = new ElectricFlowClient(configuration);
                String projectsString = efClient.getProjects();
                JSONObject jsonObject = JSONObject.fromObject(projectsString);
                JSONArray projects = jsonObject.getJSONArray("project");

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
        } catch (Exception e) {
            if (Utils.isEflowAvailable(configuration)) {
                log.error("Error when fetching values for this parameter - project. Error message: " + e.getMessage(), e);
                return SelectFieldUtils.getListBoxModelOnException("Select project");
            } else {
                return SelectFieldUtils.getListBoxModelOnWrongConf("Select project");
            }
        }
    }

    public static String getValidationComparisonHeaderRow() {
        return "<thead style=\"background-color: #e0e0e0;\"><td>Parameter Name</td><td>Old Value</td><td>New Value</td></thead>";
    }

    public static String getValidationComparisonRow(String parameterName, Object oldValue, Object newValue) {
        String rowStyleAttr = "";
        if (!newValue.equals(oldValue)) {
            rowStyleAttr = "style=\"background-color: #e2db0c;\"";
        }
        return "<tr " + rowStyleAttr + "><td>" + parameterName + "</td><td>" + oldValue + "</td><td>" + newValue + "</td></tr>";
    }

    public static String getValidationComparisonRowOldParam(String parameterName, Object oldValue) {
        String rowStyleAttr = "style=\"background-color: #fa9a76;\"";
        return "<tr " + rowStyleAttr + "><td>" + parameterName + "</td><td>" + oldValue + "</td><td></td></tr>";
    }

    public static String getValidationComparisonRowNewParam(String parameterName, Object newValue) {
        String rowStyleAttr = "style=\"background-color: #82dc84;\"";
        return "<tr " + rowStyleAttr + "><td>" + parameterName + "</td><td></td><td>" + newValue + "</td></tr>";
    }

    public static String getValidationComparisonRowsForExtraParameters(String sectionName, Map<String, String> oldParamsMap, Map<String, String> newParamsMap) {
        if (oldParamsMap.isEmpty() && newParamsMap.isEmpty()) {
            return "";
        }

        StringBuilder rows = new StringBuilder();
        rows.append("<tr><td></td><td></td><td></td></tr>");
        rows.append("<tr><td>" + sectionName + "</td><td></td><td></td></tr>");

        Set<String> oldKeysSet = new HashSet<String>(oldParamsMap.keySet());
        oldKeysSet.removeAll(newParamsMap.keySet());

        Set<String> matchingKeysSet = new HashSet<String>(oldParamsMap.keySet());
        matchingKeysSet.retainAll(newParamsMap.keySet());

        for (String oldKey : oldKeysSet) {
            rows.append(getValidationComparisonRowOldParam(oldKey, oldParamsMap.get(oldKey)));
        }

        for (Map.Entry<String, String> entry : newParamsMap.entrySet()) {
            if (matchingKeysSet.contains(entry.getKey())) {
                rows.append(getValidationComparisonRow(entry.getKey(), oldParamsMap.get(entry.getKey()), entry.getValue()));
            } else {
                rows.append(getValidationComparisonRowNewParam(entry.getKey(), entry.getValue()));
            }
        }

        return rows.toString();
    }
}
