package org.jenkinsci.plugins.electricflow;

import hudson.Extension;
import hudson.model.*;
import hudson.util.ListBoxModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.plugins.electricflow.models.CallRestApiModel;
import org.jenkinsci.plugins.electricflow.utils.CallRestApiUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.*;

public class CloudBeesFlowCallRestApiStep
        extends Step
        implements CallRestApiModel {

    private String body;
    private String configuration;
    private List<Pair> parameters;
    private String urlPath;
    private String httpMethod;
    private String envVarNameForResult;

    private static final Log log = LogFactory.getLog(CloudBeesFlowCallRestApiStep.class);

    @DataBoundConstructor
    public CloudBeesFlowCallRestApiStep(
            List<Pair> parameters) {

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

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(stepContext, this);
    }

    private static class Execution extends SynchronousStepExecution {

        private transient CloudBeesFlowCallRestApiStep step;

        Execution(@Nonnull StepContext context, @Nonnull CloudBeesFlowCallRestApiStep step) {
            super(context);
            this.step = step;
        }

        private CloudBeesFlowCallRestApiStep getStep() {
            return step;
        }

        @Override
        protected String run() throws Exception {
            return CallRestApiUtils.perform(
                    getStep(),
                    getContext().get(Run.class),
                    getContext().get(TaskListener.class)
            );
        }

        private static final long serialVersionUID = 1L;
    }

    @Extension
    public static final class DescriptorImpl
            extends StepDescriptor {


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
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(Run.class);
        }

        @Override
        public String getFunctionName() {
            return CallRestApiUtils.getFunctionName();
        }
    }
}
