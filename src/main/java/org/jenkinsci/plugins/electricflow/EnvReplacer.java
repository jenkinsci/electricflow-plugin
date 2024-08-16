// EnvReplacer.java --
//
// EnvReplacer.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;

public class EnvReplacer {

    // ~ Instance fields --------------------------------------------------------

    private TaskListener listener;
    private Run run;
    private EnvVars treeMap;

    // ~ Constructors -----------------------------------------------------------

    public EnvReplacer(Run run, TaskListener listener) throws IOException, InterruptedException {
        this.listener = listener;
        this.run = run;
        this.treeMap = this.run.getEnvironment(this.listener);
    }

    // ~ Methods ----------------------------------------------------------------

    public String expandEnv(String pattern) {
        return this.treeMap.expand(pattern);
    }
}
