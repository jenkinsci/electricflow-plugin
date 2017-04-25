
// SummaryTextAction.java --
//
// SummaryTextAction.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflowintegration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;

import jenkins.tasks.SimpleBuildStep;

public class SummaryTextAction
    implements Action,
        SimpleBuildStep.LastBuildAction
{

    //~ Instance fields --------------------------------------------------------

    private final AbstractBuild<?, ?>         build;
    private final String            summaryText;
    private List<SummaryTextAction> projectActions;

    //~ Constructors -----------------------------------------------------------

    public SummaryTextAction(
            AbstractBuild<?, ?> build,
            String    summaryText)
    {
        this.build       = build;
        this.summaryText = summaryText;

        List<SummaryTextAction> projectActions = new ArrayList<>();

        projectActions.add(this);
        this.projectActions = projectActions;
    }

    //~ Methods ----------------------------------------------------------------

    public AbstractBuild<?, ?> getBuild()
    {
        return this.build;
    }

    @Override public String getDisplayName()
    {
        return null;
    }

    @Override public String getIconFileName()
    {
        return null;
    }

    @Override public Collection<? extends Action> getProjectActions()
    {
        return this.projectActions;
    }

    public String getSummaryText()
    {
        return this.summaryText;
    }

    @Override public String getUrlName()
    {
        return null;
    }
}
