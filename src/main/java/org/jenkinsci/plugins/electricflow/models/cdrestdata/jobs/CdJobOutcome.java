package org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum CdJobOutcome {
    success,
    error,
    warning,
    skipped,
    @JsonEnumDefaultValue
    unknown
}
