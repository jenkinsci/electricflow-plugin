package org.jenkinsci.plugins.electricflow;

public class FlowArtifactObject {

    private String artifactName;
    private String artifactVersion;
    private String filePath;
    private String repositoryName;
    private String artifactURL;

    public FlowArtifactObject(String artifactName, String artifactVersion, String filePath, String repositoryName, String artifactURL){
        this.artifactName = artifactName;
        this.artifactVersion = artifactVersion;
        this.filePath = filePath;
        this.repositoryName = repositoryName;
        this.artifactURL = artifactURL;
    }

    public String getArtifactName(){
        return artifactName;
    }
    public String getArtifactVersion(){
        return artifactVersion;
    }
    public String getFilePath(){
        return filePath;
    }
    public String getRepositoryName(){
        return repositoryName;
    }
    public String getArtifactURL(){
        return artifactURL;
    }
    public void setArtifactName(String artifactName){
        this.artifactName = artifactName;
    }
    public void setArtifactVersion(String artifactVersion){
        this.artifactVersion = artifactVersion;
    }
    public void setFilePath(String filePath){
        this.filePath = filePath;
    }
    public void setRepositoryName(String repositoryName){
        this.repositoryName = repositoryName;
    }
    public void setArtifactURL(String artifactURL){
        this.artifactURL = artifactURL;
    }
}
