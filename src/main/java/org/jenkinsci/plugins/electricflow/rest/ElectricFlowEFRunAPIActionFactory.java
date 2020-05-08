package org.jenkinsci.plugins.electricflow.rest;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import jenkins.model.TransientActionFactory;

@Extension
public class ElectricFlowEFRunAPIActionFactory extends TransientActionFactory<Job> {
  @Override
  public Class<Job> type() {
    return Job.class;
  }

  @Nonnull
  @Override
  // @SuppressWarnings("unchecked")
  public Collection<? extends Action> createFor(@Nonnull Job target) {
    return Collections.singletonList(new ElectricFlowEFRunAPIAction(target));
    // return Collections.emptyList();
  }
}
