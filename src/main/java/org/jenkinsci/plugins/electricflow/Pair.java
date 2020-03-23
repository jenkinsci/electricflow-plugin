// Pair.java --
//
// Pair.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class Pair extends AbstractDescribableImpl<Pair> {
  private String key;
  private String value;

  @DataBoundConstructor
  public Pair(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<Pair> {

    @Override
    public String getDisplayName() {
      return "Parr";
    }
  }
}
