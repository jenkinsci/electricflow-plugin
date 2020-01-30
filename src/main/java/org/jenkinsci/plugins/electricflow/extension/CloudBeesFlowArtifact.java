package org.jenkinsci.plugins.electricflow.extension;

import hudson.model.Run;
import hudson.model.Run.ArtifactList;
import hudson.model.Run.Artifact;

public class CloudBeesFlowArtifact {
    protected String relativePath;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    protected String displayPath;
    protected String name;
    protected String href;
    protected String length;
    protected long size;

    public CloudBeesFlowArtifact() {};

    public static CloudBeesFlowArtifact build (Artifact obj) {
        CloudBeesFlowArtifact cloudBeesFlowArtifact = new CloudBeesFlowArtifact();

        cloudBeesFlowArtifact.setDisplayPath(obj.getDisplayPath());
        // todo: switch to relativePath
        cloudBeesFlowArtifact.setRelativePath(obj.getDisplayPath());
        cloudBeesFlowArtifact.setName(obj.getFileName());
        cloudBeesFlowArtifact.setHref(obj.getHref());
        cloudBeesFlowArtifact.setLength(obj.getLength());
        cloudBeesFlowArtifact.setSize(obj.getFileSize());
        return cloudBeesFlowArtifact;
    }
    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getDisplayPath() {
        return displayPath;
    }

    public void setDisplayPath(String displayPath) {
        this.displayPath = displayPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }
}
