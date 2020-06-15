package org.jenkinsci.plugins.electricflow;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class RunAndWaitOption extends AbstractDescribableImpl<RunAndWaitOption> {

  private boolean dependOnCdJobOutcome;
  private int checkInterval;

  @DataBoundConstructor
  public RunAndWaitOption(boolean dependOnCdJobOutcome, int checkInterval) {
    this.dependOnCdJobOutcome = dependOnCdJobOutcome;
    this.checkInterval = checkInterval;
  }

  public boolean isDependOnCdJobOutcome() {
    return dependOnCdJobOutcome;
  }

  public int getCheckInterval() {
    return checkInterval;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<RunAndWaitOption> {

    @Override
    public String getDisplayName() {
      return "RunAndWaitOption";
    }
  }
}
