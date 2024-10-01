package org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs;

import static org.jenkinsci.plugins.electricflow.Utils.formatJsonOutput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetJobStatusResponseData implements FlowRuntimeResponseData {

    @JsonProperty()
    private String jobId;

    @JsonProperty()
    private CdJobStatus status = CdJobStatus.unknown;

    @JsonProperty()
    private CdJobOutcome outcome = CdJobOutcome.unknown;

    private String content;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public CdJobStatus getStatus() {
        return status;
    }

    public void setStatus(CdJobStatus status) {
        this.status = status;
    }

    public CdJobOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(CdJobOutcome outcome) {
        this.outcome = outcome;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        if ((getStatus() == CdJobStatus.unknown || getOutcome() == CdJobOutcome.unknown) && getContent() != null) {
            try {
                return "CD Job Status Response (unexpected json): " + formatJsonOutput(getContent());
            } catch (IOException e) {
                return "CD Job Status Response (unexpected content): " + getContent();
            }
        }
        return "CD Job Status Response Data: {"
                + "jobId="
                + jobId
                + ", status="
                + status
                + ", outcome="
                + outcome
                + '}';
    }

    @Override
    public Boolean isCompleted() {
        return status.equals(CdJobStatus.completed);
    }

    @Override
    public String getRuntimeOutcome() {
        return getOutcome().toString();
    }

    @Override
    public String getRuntimeStatus() {
        return getStatus().toString();
    }

    @Override
    public String getRuntimeId() {
        return getJobId();
    }
}
