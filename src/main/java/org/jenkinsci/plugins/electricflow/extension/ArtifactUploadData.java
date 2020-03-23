package org.jenkinsci.plugins.electricflow.extension;

public class ArtifactUploadData
{

    //~ Instance fields --------------------------------------------------------

    protected String artifactName;
    protected String artifactVersion;
    protected String artifactUrl;
    protected String repositoryName;
    protected String repositoryType;
    protected String filePath;

    //~ Methods ----------------------------------------------------------------

    public String getArtifactName()
    {
        return artifactName;
    }

    public String getArtifactUrl()
    {
        return artifactUrl;
    }

    public String getArtifactVersion()
    {
        return artifactVersion;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public String getRepositoryName()
    {
        return repositoryName;
    }

    public String getRepositoryType()
    {
        return repositoryType;
    }

    public void setArtifactName(String artifactName)
    {
        this.artifactName = artifactName;
    }

    public void setArtifactUrl(String artifactUrl)
    {
        this.artifactUrl = artifactUrl;
    }

    public void setArtifactVersion(String artifactVersion)
    {
        this.artifactVersion = artifactVersion;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public void setRepositoryName(String repositoryName)
    {
        this.repositoryName = repositoryName;
    }

    public void setRepositoryType(String repositoryType)
    {
        this.repositoryType = repositoryType;
    }
}
