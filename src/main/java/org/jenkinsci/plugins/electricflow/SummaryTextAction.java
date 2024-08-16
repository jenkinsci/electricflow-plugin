// SummaryTextAction.java --
//
// SummaryTextAction.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import static org.jenkinsci.plugins.electricflow.ui.HtmlUtils.getHtmlPolicy;

import hudson.model.Action;
import hudson.model.Run;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jenkins.tasks.SimpleBuildStep;

public class SummaryTextAction implements Action, SimpleBuildStep.LastBuildAction {

    // ~ Instance fields --------------------------------------------------------

    protected final Run<?, ?> run;
    protected String summaryText;
    protected List<SummaryTextAction> projectActions;

    // ~ Constructors -----------------------------------------------------------

    public SummaryTextAction(Run<?, ?> run, String summaryText) {
        this.run = run;
        this.summaryText = summaryText;

        List<SummaryTextAction> projectActions = new ArrayList<>();

        projectActions.add(this);
        this.projectActions = projectActions;
    }

    // ~ Methods ----------------------------------------------------------------

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return this.projectActions;
    }

    public Run<?, ?> getRun() {
        return this.run;
    }

    public String getSummaryText() {
        return getHtmlPolicy().sanitize(this.summaryText);
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    @Override
    public String getUrlName() {
        return null;
    }
}
