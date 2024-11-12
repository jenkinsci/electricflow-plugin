package org.jenkinsci.plugins.electricflow;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class RunAndWaitOption extends AbstractDescribableImpl<RunAndWaitOption> {

    private static final int CHECK_INTERVAL_MIN = 1;
    private static final int CHECK_INTERVAL_DEFAULT = 5;
    private static final boolean DEPEND_ON_CD_JOB_OUTCOME_DEFAULT = true;
    private static final boolean THROW_EXCEPTION_IF_FAILED_DEFAULT = false;

    private boolean dependOnCdJobOutcome = DEPEND_ON_CD_JOB_OUTCOME_DEFAULT;
    private boolean throwExceptionIfFailed = THROW_EXCEPTION_IF_FAILED_DEFAULT;
    private int checkInterval = CHECK_INTERVAL_DEFAULT;

    @DataBoundConstructor
    public RunAndWaitOption() {}

    public boolean isDependOnCdJobOutcome() {
        return dependOnCdJobOutcome;
    }

    @DataBoundSetter
    public void setDependOnCdJobOutcome(boolean value) {
        this.dependOnCdJobOutcome = value;
    }

    public boolean isThrowExceptionIfFailed() {
        return throwExceptionIfFailed;
    }

    @DataBoundSetter
    public void setThrowExceptionIfFailed(boolean value) {
        this.throwExceptionIfFailed = value;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    @DataBoundSetter
    public void setCheckInterval(int checkInterval) {
        if (checkInterval < CHECK_INTERVAL_MIN) {
            checkInterval = CHECK_INTERVAL_DEFAULT;
        }
        this.checkInterval = checkInterval;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RunAndWaitOption> {

        @Override
        public String getDisplayName() {
            return "RunAndWaitOption";
        }
    }
}
