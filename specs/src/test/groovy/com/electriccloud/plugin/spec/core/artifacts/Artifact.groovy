package com.electriccloud.plugin.spec.core.artifacts

import com.electriccloud.plugin.spec.core.ResponseDecorator
import com.electriccloud.spec.PluginSpockTestSupport

import java.lang.ref.SoftReference

class Artifact implements ResponseDecorator {

    final String groupId
    final String artifactKey

    @Lazy
    Map dslObject = {_retrieveDetails()}()

    ArrayList<ArtifactVersion> versions

    Artifact(String groupId, String artifactKey) {
        this.groupId = groupId
        this.artifactKey = artifactKey
    }

    void refresh(){
        versions = _retrieveArtifactVersions()
    }

    ArrayList<ArtifactVersion> getVersions(){
        if (this.versions == null){
            this.versions = _retrieveArtifactVersions()
        }
        return this.versions
    }

    Map _retrieveDetails() {
        def response = (new PluginSpockTestSupport()).dsl("""
            getArtifact( artifactName: '$groupId:$artifactKey' )
        """)
        return response['artifact']
    }

    private ArrayList<ArtifactVersion> _retrieveArtifactVersions() {
        try {
            def response = (new PluginSpockTestSupport()).dsl("""
        getArtifactVersions( artifactName: '$groupId:$artifactKey' )
    """)
            return response['artifactVersion'].collect({ new ArtifactVersion(this, it) })
        } catch (Error ex){
            log.warning("DSL call for getArtifactVersions failed. " + ex.getMessage())
        }
        return null
    }
}
