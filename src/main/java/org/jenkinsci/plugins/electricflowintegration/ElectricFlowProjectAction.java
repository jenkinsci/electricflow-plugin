
// ElectricFlowProjectAction.java --
//
// ElectricFlowProjectAction.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflowintegration;

import hudson.model.AbstractProject;
import hudson.model.Action;

public class ElectricFlowProjectAction
    implements Action
{

    //~ Instance fields --------------------------------------------------------

    private AbstractProject<?, ?> project;

    //~ Constructors -----------------------------------------------------------

    ElectricFlowProjectAction(final AbstractProject<?, ?> project)
    {
        this.project = project;
    }

    //~ Methods ----------------------------------------------------------------

    @Override public String getDisplayName()
    {
        return "ElectricFlow Pipeline publisher action";
    }

    @Override public String getIconFileName()
    {
        return "/plugin/electricflow-integration/img/project_icon.png";
    }

    public AbstractProject<?, ?> getProject()
    {
        return this.project;
    }

    public String getProjectName()
    {
        return this.project.getName();
    }

    @Override public String getUrlName()
    {
        return "Electric flow publisher action";
    }
}
