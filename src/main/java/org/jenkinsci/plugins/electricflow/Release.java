// Release.java --
//
// Release.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.util.Collections;
import java.util.List;

public class Release {

  // ~ Instance fields --------------------------------------------------------

  private String configuration;
  private List<String> startStages;
  private String releaseName;
  private String pipelineName;
  private List<String> pipelineParameters;
  private String projectName;

  public Release(String configuration, String projectName, String releaseName) {
    this.configuration = configuration;
    this.releaseName = releaseName;
    this.projectName = projectName;
  }

  // ~ Constructors -----------------------------------------------------------

  public String getProjectName() {
    return projectName;
  }

  // ~ Methods ----------------------------------------------------------------

  public String getConfiguration() {
    return configuration;
  }

  public String getPipelineName() {
    return pipelineName;
  }

  public void setPipelineName(String pipelineName) {
    this.pipelineName = pipelineName;
  }

  public String getReleaseName() {
    return releaseName;
  }

  public void setReleaseName(String releaseName) {
    this.releaseName = releaseName;
  }

  public List<String> getPipelineParameters() {
    return pipelineParameters;
  }

  public void setPipelineParameters(List<String> pipelineParameters) {
    this.pipelineParameters = pipelineParameters;
  }

  public List<String> getStartStages() {
    return startStages;
  }

  public void setStartStages(List<String> startStages) {
    Collections.sort(startStages, String::compareTo);
    this.startStages = startStages;
  }
}
