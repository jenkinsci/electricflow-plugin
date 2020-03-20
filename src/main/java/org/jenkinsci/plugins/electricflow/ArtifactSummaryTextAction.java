package org.jenkinsci.plugins.electricflow;

import hudson.model.Action;
import hudson.model.Run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jenkinsci.plugins.electricflow.ui.HtmlUtils.getHtmlPolicy;

public class ArtifactSummaryTextAction extends SummaryTextAction{

    private final Run<?, ?> run;
    private final String            summaryText;
    private List<SummaryTextAction> projectActions;
    private FlowArtifactObject artifactDetails;

    public ArtifactSummaryTextAction(
            Run<?, ?> run,
            String    summaryText,
            FlowArtifactObject artifactDetails)
    {
        this.run         = run;
        this.summaryText = summaryText;
        this.artifactDetails = artifactDetails;

        List<SummaryTextAction> projectActions = new ArrayList<>();

        projectActions.add(this);
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

    public FlowArtifactObject getArtifactDetails()
    {
        return this.artifactDetails;
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
