
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

import org.jenkinsci.plugins.electricflow.models.CallRestApiModel;
import hudson.model.*;
import org.jenkinsci.plugins.electricflow.utils.CallRestApiUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import hudson.util.ListBoxModel;

import jenkins.tasks.SimpleBuildStep;

public class ElectricFlowGenericRestApi
        extends Recorder
        implements SimpleBuildStep, CallRestApiModel {

    private String configuration;
    private String urlPath;
    private String httpMethod;
    private List<Pair> parameters;
    private String body;
    private String envVarNameForResult;

    @DataBoundConstructor
    public ElectricFlowGenericRestApi(
            List<Pair> parameters) {

        if (parameters == null) {
            this.parameters = new ArrayList<>(0);
        } else {
            this.parameters = new ArrayList<>(parameters);
        }
    }

    @Override
    public void perform(
            @Nonnull Run<?, ?> run,
            @Nonnull FilePath filePath,
            @Nonnull Launcher launcher,
            @Nonnull TaskListener taskListener)
            throws InterruptedException, IOException {

        CallRestApiUtils.perform(this, run, taskListener);
    }

    @Override
    public String getConfiguration() {
        return configuration;
    }

    @Override
    public String getUrlPath() {
        return urlPath;
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }

    @Override
    public List<Pair> getParameters() {
        return parameters;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public String getEnvVarNameForResult() {
        return envVarNameForResult;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @DataBoundSetter
    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    @DataBoundSetter
    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    @DataBoundSetter
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    @DataBoundSetter
    public void setParameters(List<Pair> parameters) {
        this.parameters = parameters;
    }

    @DataBoundSetter
    public void setBody(String body) {
        this.body = body;
    }

    @DataBoundSetter
    public void setEnvVarNameForResult(String envVarNameForResult) {
        this.envVarNameForResult = envVarNameForResult;
    }

    @Extension
    public static final class DescriptorImpl
            extends BuildStepDescriptor<Publisher> {


        public ListBoxModel doFillConfigurationItems(@AncestorInPath Item item) {
            return CallRestApiUtils.doFillConfigurationItems(item);
        }

        public ListBoxModel doFillHttpMethodItems(@AncestorInPath Item item) {
            return CallRestApiUtils.doFillHttpMethodItems(item);
        }

        @Override
        public String getDisplayName() {
            return CallRestApiUtils.getDisplayName();
        }

        @Override
        public boolean isApplicable(
                Class<? extends AbstractProject> aClass) {
            return true;
        }
    }
}
