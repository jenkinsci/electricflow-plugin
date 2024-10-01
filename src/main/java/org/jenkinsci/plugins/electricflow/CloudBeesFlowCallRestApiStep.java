package org.jenkinsci.plugins.electricflow;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.plugins.electricflow.models.CallRestApiModel;
import org.jenkinsci.plugins.electricflow.utils.CallRestApiUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class CloudBeesFlowCallRestApiStep extends Step implements CallRestApiModel {

    private static final Log log = LogFactory.getLog(CloudBeesFlowCallRestApiStep.class);
    private String body;
    private String configuration;
    private Credential overrideCredential;
    private List<Pair> parameters;
    private String urlPath;
    private String httpMethod;
    private String envVarNameForResult;

    @DataBoundConstructor
    public CloudBeesFlowCallRestApiStep(List<Pair> parameters) {

        if (parameters == null) {
            this.parameters = new ArrayList<>(0);
        } else {
            this.parameters = new ArrayList<>(parameters);
        }
    }

    @Override
    public String getConfiguration() {
        return configuration;
    }

    @DataBoundSetter
    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public Credential getOverrideCredential() {
        return overrideCredential;
    }

    @DataBoundSetter
    public void setOverrideCredential(Credential overrideCredential) {
        this.overrideCredential = overrideCredential;
    }

    @Override
    public String getUrlPath() {
        return urlPath;
    }

    @DataBoundSetter
    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }

    @DataBoundSetter
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    @Override
    public List<Pair> getParameters() {
        return parameters;
    }

    @DataBoundSetter
    public void setParameters(List<Pair> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String getBody() {
        return body;
    }

    @DataBoundSetter
    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String getEnvVarNameForResult() {
        return envVarNameForResult;
    }

    @DataBoundSetter
    public void setEnvVarNameForResult(String envVarNameForResult) {
        this.envVarNameForResult = envVarNameForResult;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(stepContext, this);
    }

    private static class Execution extends SynchronousStepExecution {

        private static final long serialVersionUID = 1L;
        private transient CloudBeesFlowCallRestApiStep step;

        Execution(@NonNull StepContext context, @NonNull CloudBeesFlowCallRestApiStep step) {
            super(context);
            this.step = step;
        }

        private CloudBeesFlowCallRestApiStep getStep() {
            return step;
        }

        @Override
        protected String run() throws Exception {
            return CallRestApiUtils.perform(
                    getStep(), getContext().get(Run.class), getContext().get(TaskListener.class));
        }
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        public ListBoxModel doFillConfigurationItems(@AncestorInPath Item item) {
            return CallRestApiUtils.doFillConfigurationItems(item);
        }

        public ListBoxModel doFillCredentialIdItems(@AncestorInPath Item item) {
            return Credential.DescriptorImpl.doFillCredentialIdItems(item);
        }

        public ListBoxModel doFillHttpMethodItems(@AncestorInPath Item item) {
            return CallRestApiUtils.doFillHttpMethodItems(item);
        }

        @Override
        public String getDisplayName() {
            return CallRestApiUtils.getDisplayName();
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(Run.class);
        }

        @Override
        public String getFunctionName() {
            return CallRestApiUtils.getFunctionName();
        }
    }
}
