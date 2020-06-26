package org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum CdPipelineOutcome {
  success,
  error,
  warning,
  @JsonEnumDefaultValue
  unknown
}
