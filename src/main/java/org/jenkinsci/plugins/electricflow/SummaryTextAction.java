
// SummaryTextAction.java --
//
// SummaryTextAction.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import hudson.model.Action;
import hudson.model.Run;

import jenkins.tasks.SimpleBuildStep;

import static org.jenkinsci.plugins.electricflow.ui.HtmlUtils.getHtmlPolicy;

public class SummaryTextAction
    implements Action,
        SimpleBuildStep.LastBuildAction
{

    //~ Instance fields --------------------------------------------------------

    private final Run<?, ?>         run;
    private final String            summaryText;
    private List<SummaryTextAction> projectActions;

    //~ Constructors -----------------------------------------------------------

    public SummaryTextAction(
            Run<?, ?> run,
            String    summaryText)
    {
        this.run         = run;
        this.summaryText = summaryText;

        List<SummaryTextAction> projectActions = new ArrayList<>();

        projectActions.add(this);
        this.projectActions = projectActions;
    }

    //This should never be used but is required for `extends`
    public SummaryTextAction(){
        this.run = null;
        this.summaryText = null;
        List<SummaryTextAction> projectActions = new ArrayList<>();
        this.projectActions = projectActions;
    }

    //~ Methods ----------------------------------------------------------------

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

    public Run<?, ?> getRun()
    {
        return this.run;
    }

    public String getSummaryText() {
        return getHtmlPolicy().sanitize(this.summaryText);
    }

    @Override public String getUrlName()
    {
        return null;
    }
}
