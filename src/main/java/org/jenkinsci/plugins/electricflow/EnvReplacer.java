
// EnvReplacer.java --
//
// EnvReplacer.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.io.IOException;

import hudson.EnvVars;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;

public class EnvReplacer
{

    //~ Instance fields --------------------------------------------------------

    private TaskListener  listener;
    private AbstractBuild build;
    private EnvVars       treeMap;

    //~ Constructors -----------------------------------------------------------

    public EnvReplacer(
            AbstractBuild build,
            TaskListener  listener)
        throws IOException, InterruptedException
    {
        this.listener = listener;
        this.build    = build;
        this.treeMap  = this.build.getEnvironment(this.listener);
    }

    //~ Methods ----------------------------------------------------------------

    public String expandEnv(String pattern)
    {
        return this.treeMap.expand(pattern);
    }
}
