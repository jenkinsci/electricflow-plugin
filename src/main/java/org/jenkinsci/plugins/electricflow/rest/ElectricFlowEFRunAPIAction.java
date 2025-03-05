package org.jenkinsci.plugins.electricflow.rest;

import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.model.StringParameterValue;
import jakarta.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jenkins.model.Jenkins;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.electricflow.causes.EFCause;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.verb.GET;
import org.kohsuke.stapler.verb.POST;

// public class ElectricFlowEFRunAPIAction<T extends Job<?, ?> &
// ParameterizedJobMixIn.ParameterizedJob>  implements Action {
public class ElectricFlowEFRunAPIAction<T extends Job<?, ?> & Queue.Task> implements Action {
    private static final String URL_NAME = "efrun";
    private final T project;

    // constructor
    public ElectricFlowEFRunAPIAction(T project) {
        this.project = project;
    }

    // Interface methods

    @Override
    public String getIconFileName() {
        // return "/plugin/electricflow-integration/img/flow-icon-white.svg";
        return null;
        // return null; // Invisible
    }

    @Override
    public String getDisplayName() {
        return null; // Invisible
    }

    @Override
    public String getUrlName() {
        return URL_NAME;
    }

    // end of interface methods.

    // action methods
    @GET
    @POST
    public void doIndex(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException {
        rsp.setStatus(201);
        JSONObject jsonObject = this.getEFRunIndexResponse();
        this.sendJSONResponse(rsp, jsonObject);
    }

    @POST
    @GET
    public void doBuild(StaplerRequest2 req, StaplerResponse2 rsp
            // @QueryParameter final JSONObject json
            // @QueryParameter("value") final String value,
            // JSONObject formData
            ) throws IOException, ServletException {
        if (!Objects.equals(req.getMethod(), "POST")) {
            this.sendJSONResponse(rsp, this.getEFRunIndexBuildResponse());
            return;
        }
        if (!project.hasPermission(Item.BUILD)) {
            String message =
                    String.format("User is not authorized to queue builds for project '%s'", project.getDisplayName());

            JSONObject responseObject = new JSONObject();

            rsp.setStatus(403);
            responseObject.put("status", "fail");
            responseObject.put("reason", message);

            OutputStream out = rsp.getOutputStream();
            String responseString = responseObject.toString();
            byte[] responseBytes = responseString.getBytes("UTF-8");
            rsp.setContentLength(responseBytes.length);
            out.write(responseBytes);
            out.flush();

            return;
        }

        rsp.setStatus(201);
        // ServletInputStream is = req.getInputStream();
        BufferedReader br = req.getReader();
        StringBuilder sb = new StringBuilder();
        String strCurrentLine;
        while ((strCurrentLine = br.readLine()) != null) {
            sb.append(strCurrentLine);
        }
        JSONObject jsonObject = null;

        try {
            jsonObject = JSONObject.fromObject(sb.toString());
        } catch (JSONException e) {
            JSONObject jsonResponse = this.getErrorResponse(e.getMessage());
            this.sendJSONResponse(500, rsp, jsonResponse);
            return;
        }

        String flowRuntimeId = "";
        String projectName = "";
        String releaseName = "";
        String flowRuntimeStateId = "";
        String stageName = "";

        boolean hasBuildParams = false;
        JSONObject buildParams = new JSONObject();
        List<ParameterValue> parameterValues = new ArrayList<ParameterValue>();

        try {
            flowRuntimeId = jsonObject.getString("flowRuntimeId");
        } catch (JSONException ignored) {
        }
        ;
        try {
            projectName = jsonObject.getString("projectName");
        } catch (JSONException ignored) {
        }
        ;
        try {
            releaseName = jsonObject.getString("releaseName");
        } catch (JSONException ignored) {
        }
        ;
        try {
            flowRuntimeStateId = jsonObject.getString("flowRuntimeStateId");
        } catch (JSONException ignored) {
        }
        ;
        try {
            stageName = jsonObject.getString("stageName");
        } catch (JSONException ignored) {
        }
        ;

        try {
            buildParams = jsonObject.getJSONObject("buildParams");
            if (buildParams.keySet().size() > 0) {
                hasBuildParams = true;
            }
            //            if (buildParams != null) {
            //                hasBuildParams = true;
            //            }
        } catch (JSONException e) {
            //      JSONObject jsonResponse = this.getErrorResponse(e.getMessage());
            //      this.sendJSONResponse(rsp, jsonResponse);
            //      return;
        }
        ;

        JSONObject responseObject = new JSONObject();

        EFCause efcause = new EFCause();
        Cause.UserIdCause userIdCause = new Cause.UserIdCause();
        if (flowRuntimeId != null) {
            efcause.setFlowRuntimeId(flowRuntimeId);
        }
        if (projectName != null) {
            efcause.setProjectName(projectName);
        }
        if (releaseName != null) {
            efcause.setReleaseName(releaseName);
        }
        if (flowRuntimeStateId != null) {
            efcause.setFlowRuntimeStateId(flowRuntimeStateId);
        }
        if (stageName != null) {
            efcause.setStageName(stageName);
        }
        CauseAction ca = new CauseAction(efcause, userIdCause);

        Queue.WaitingItem schedule = null;
        OutputStream out = rsp.getOutputStream();
        if (hasBuildParams) {
            for (ParameterDefinition parameterDefinition : getParameterDefinitions()) {
                String parameterName = parameterDefinition.getName();
                try {
                    String formValue = buildParams.getString(parameterName);
                    StringParameterValue spv = new StringParameterValue(parameterName, formValue);
                    parameterValues.add(spv);
                } catch (JSONException e) {
                    //          JSONObject jsonResponse = this.getErrorResponse(e.getMessage());
                    //          this.sendJSONResponse(rsp, jsonResponse);
                    //          return;
                }
                ;
            }
            schedule = Jenkins.get().getQueue().schedule(project, 0, new ParametersAction(parameterValues), ca);
        } else {
            schedule = Jenkins.get().getQueue().schedule(project, 0, ca);
        }
        if (schedule == null) {
            responseObject.put("status", "fail");
            byte[] responseFailed = responseObject.toString().getBytes("UTF-8");
            rsp.setContentLength(responseFailed.length);
            out.write(responseFailed);
            return;
        }
        responseObject.put("status", "ok");
        rsp.setHeader("location", "queue/" + Long.toString(schedule.getId()));

        responseObject.put("queueId", schedule.getId());
        String responseString = responseObject.toString();
        byte[] responseBytes = responseString.getBytes("UTF-8");
        rsp.setContentLength(responseBytes.length);
        out.write(responseBytes);
        out.flush();
    }

    private List<ParameterDefinition> getParameterDefinitions() {
        ParametersDefinitionProperty property =
                (ParametersDefinitionProperty) project.getProperty(ParametersDefinitionProperty.class);
        if (property != null && property.getParameterDefinitions() != null) {
            return property.getParameterDefinitions();
        }
        return new ArrayList<ParameterDefinition>();
    }

    private void sendJSONResponse(StaplerResponse2 rsp, JSONObject responseObject)
            throws IOException, ServletException {
        this.sendJSONResponse(201, rsp, responseObject);
    }

    private void sendJSONResponse(int responseCode, StaplerResponse2 rsp, JSONObject responseObject)
            throws IOException, ServletException {
        rsp.setStatus(responseCode);
        String responseString = responseObject.toString();
        byte[] responseBytes = responseString.getBytes(StandardCharsets.UTF_8);
        rsp.setContentLength(responseBytes.length);
        OutputStream out = rsp.getOutputStream();
        out.write(responseBytes);
        out.flush();
    }

    private JSONObject getEFRunIndexBuildResponse() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "ok");
        jsonObject.put("description", "efrun/build: Use POST method to trigger the build.");
        return jsonObject;
    }

    private JSONObject getEFRunIndexResponse() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "ok");
        jsonObject.put("description", "EFRun API is running and available");
        return jsonObject;
    }

    private JSONObject getErrorResponse(String msg) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "error");
        jsonObject.put("description", msg);
        return jsonObject;
    }
}
