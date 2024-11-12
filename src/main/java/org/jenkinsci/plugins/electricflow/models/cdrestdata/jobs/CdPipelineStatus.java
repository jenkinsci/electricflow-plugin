package org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum CdPipelineStatus {
    running,
    success,
    warning,
    error,
    ABORT,
    FORCE_ABORT,
    @JsonEnumDefaultValue
    unknown
}
