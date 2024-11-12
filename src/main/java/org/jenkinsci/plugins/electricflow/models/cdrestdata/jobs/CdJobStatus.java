package org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum CdJobStatus {
    pending,
    runnable,
    running,
    scheduled,
    completed,
    @JsonEnumDefaultValue
    unknown
}
