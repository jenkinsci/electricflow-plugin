package org.jenkinsci.plugins.electricflow.envvars;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Run;

public class VariableInjectionAction implements EnvironmentContributingAction {

    private String key;
    private String value;

    public VariableInjectionAction(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "VariableInjectionAction";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }

    @Override
    public void buildEnvironment(@NonNull Run<?, ?> run, @NonNull EnvVars envVars) {
        if (key != null && value != null) {
            envVars.put(key, value);
        }
    }
}
