package com.electriccloud.plugin.spec.core.application;

import com.electriccloud.plugin.spec.core.Job;
import java.util.Map;

public class ProcessRun extends Job {

  public ProcessRun(String jobId) {
    super(jobId);
  }

  public ProcessRun(Map<String, Object> dslObject) {
    super(dslObject);
  }

}
