package org.jenkinsci.plugins.electricflow.rest;

import hudson.model.*;
import hudson.util.IOUtils;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
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
import java.util.Map;

public class ElectricFlowEFRunAPIAction<T extends Job<?, ?> & ParameterizedJobMixIn.ParameterizedJob>  implements Action {
    private static final String URL_NAME = "efrun";
    private final T project;

    // constructor
    public ElectricFlowEFRunAPIAction(T project) {
        this.project = project;
    }

    // Interface methods

    public String getIconFileName() {
        return null; // Invisible
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
        rsp.setStatus(200);
        String responseString = "Hello World";
        byte[] responseBytes = responseString.getBytes();
        rsp.setContentLength(responseBytes.length);
        OutputStream out = rsp.getOutputStream();
        out.write(responseBytes);
        out.flush();
        // out.close();
    }

    public void doBuild(
            StaplerRequest req,
            StaplerResponse rsp
            // @QueryParameter final JSONObject json
            // @QueryParameter("value") final String value,
            // JSONObject formData
            ) throws IOException, ServletException {
        rsp.setStatus(200);
        // ServletInputStream is = req.getInputStream();
        BufferedReader br = req.getReader();
        StringBuilder sb = new StringBuilder();
        String strCurrentLine;
        while ((strCurrentLine = br.readLine()) != null) {
            sb.append(strCurrentLine);
        }
        JSONObject jsonObject = JSONObject.fromObject(sb.toString());

        String flowRuntimeId = null;
        String projectName;
        String releaseName;

        try {
            flowRuntimeId = jsonObject.getString("flowRuntimeId");
        } catch (JSONException e) {};

        String responseString = "Hello World default";
        if (flowRuntimeId != null) {
            responseString = flowRuntimeId;
        }
        byte[] responseBytes = responseString.getBytes();
        rsp.setContentLength(responseBytes.length);
        OutputStream out = rsp.getOutputStream();
        EFCause efcause = new EFCause();
        if (flowRuntimeId != null) {
            efcause.setFlowRuntimeId(flowRuntimeId);
            efcause.setProjectName(flowRuntimeId);
            efcause.setReleaseName(flowRuntimeId);
        }
        CauseAction ca = new CauseAction(efcause);
        Queue.WaitingItem schedule = Jenkins.get().getQueue().schedule(project, 0, ca);
        out.write(responseBytes);
        out.flush();
    }
}

