package org.jenkinsci.plugins.electricflow.rest;

import groovy.json.JsonException;
import hudson.model.*;
import hudson.util.IOUtils;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSON;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.electricflow.causes.EFCause;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.json.JsonBody;
import org.kohsuke.stapler.json.JsonResponse;
import org.kohsuke.stapler.verb.GET;
import org.kohsuke.stapler.verb.POST;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// public class ElectricFlowEFRunAPIAction<T extends Job<?, ?> & ParameterizedJobMixIn.ParameterizedJob>  implements Action {
public class ElectricFlowEFRunAPIAction<T extends Job<?, ?> & Queue.Task>  implements Action {
    private static final String URL_NAME = "efrun";
    private final T project;

    // constructor
    public ElectricFlowEFRunAPIAction(T project) {
        this.project = project;
    }

    // Interface methods

    public String getIconFileName() {
        // return "/plugin/electricflow-integration/img/flow-icon-white.svg";
        return null;
        //return null; // Invisible
    }

    public String getDisplayName() {
        return null; // Invisible
    }

    public String getUrlName() {
        return URL_NAME;
    }

    // end of interface methods.

    // action methods
    @POST
    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        rsp.setStatus(201);
        String responseString = "Hello World";
        byte[] responseBytes = responseString.getBytes("UTF-8");
        rsp.setContentLength(responseBytes.length);
        OutputStream out = rsp.getOutputStream();
        out.write(responseBytes);
        out.flush();
        // out.close();
    }

    @POST
    public void doBuild(
            StaplerRequest req,
            StaplerResponse rsp
            // @QueryParameter final JSONObject json
            // @QueryParameter("value") final String value,
            // JSONObject formData
            ) throws IOException, ServletException {
        rsp.setStatus(201);
        // ServletInputStream is = req.getInputStream();
        BufferedReader br = req.getReader();
        StringBuilder sb = new StringBuilder();
        String strCurrentLine;
        while ((strCurrentLine = br.readLine()) != null) {
            sb.append(strCurrentLine);
        }
        JSONObject jsonObject = JSONObject.fromObject(sb.toString());

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
        } catch (JSONException ignored) {};
        try {
            projectName = jsonObject.getString("projectName");
        } catch (JSONException ignored) {};
        try {
            releaseName = jsonObject.getString("releaseName");
        } catch (JSONException ignored) {};
        try {
            flowRuntimeStateId = jsonObject.getString("flowRuntimeStateId");
        } catch (JSONException ignored) {};
        try {
            stageName = jsonObject.getString("stageName");
        } catch (JSONException ignored) {};

        try {
            buildParams = jsonObject.getJSONObject("buildParams");
            if (buildParams.keySet().size() > 0) {
                hasBuildParams = true;
            }
//            if (buildParams != null) {
//                hasBuildParams = true;
//            }
        } catch (JSONException e) {};

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
                } catch (JSONException e) {};
            }
            schedule = Jenkins.get().getQueue().schedule(project, 0, new ParametersAction(parameterValues), ca);
        }
        else {
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
        ParametersDefinitionProperty property = (ParametersDefinitionProperty) project.getProperty(ParametersDefinitionProperty.class);
        if (property != null && property.getParameterDefinitions() != null) {
            return property.getParameterDefinitions();
        }
        return new ArrayList<ParameterDefinition>();
    }
}

