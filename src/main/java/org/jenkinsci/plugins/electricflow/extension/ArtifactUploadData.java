package org.jenkinsci.plugins.electricflow.extension;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class ArtifactUploadData {

    // ~ Instance fields --------------------------------------------------------

    protected String artifactName;
    protected String artifactVersion;
    protected String artifactVersionName;
    protected String artifactUrl;
    protected String repositoryName;
    protected String repositoryType;
    protected String filePath;

    // ~ Methods ----------------------------------------------------------------

    @Exported
    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    @Exported
    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    @Exported
    public String getArtifactVersionName() {
        return artifactVersionName;
    }

    public void setArtifactVersionName(String artifactVersionName) {
        this.artifactVersionName = artifactVersionName;
    }

    @Exported
    public String getArtifactUrl() {
        return artifactUrl;
    }

    public void setArtifactUrl(String artifactUrl) {
        this.artifactUrl = artifactUrl;
    }

    @Exported
    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    @Exported
    public String getRepositoryType() {
        return repositoryType;
    }

    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType;
    }

    @Exported
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
