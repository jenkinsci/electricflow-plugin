package org.jenkinsci.plugins.electricflow;
import java.io.IOException;
import java.lang.InterruptedException;
import hudson.model.TaskListener;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import java.util.Map;

public class EnvReplacer {
    private TaskListener listener;
    private AbstractBuild build;
    private EnvVars treeMap;
    
    public EnvReplacer(AbstractBuild build, TaskListener listener) throws IOException, InterruptedException {
        this.listener = listener;
        this.build = build;
        this.treeMap = this.build.getEnvironment(this.listener);
    }

    public String expandEnvs(String pattern) {
        return this.treeMap.expand(pattern);
    }
}



