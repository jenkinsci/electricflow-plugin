package org.jenkinsci.plugins.electricflow.rest;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import java.util.Collection;
import java.util.Collections;
import jenkins.model.TransientActionFactory;

@Extension
public class ElectricFlowEFRunAPIActionFactory extends TransientActionFactory<Job> {
  @Override
  public Class<Job> type() {
    return Job.class;
  }

  @NonNull
  @Override
  // @SuppressWarnings("unchecked")
  public Collection<? extends Action> createFor(@NonNull Job target) {
    return Collections.singletonList(new ElectricFlowEFRunAPIAction(target));
    // return Collections.emptyList();
  }
}
