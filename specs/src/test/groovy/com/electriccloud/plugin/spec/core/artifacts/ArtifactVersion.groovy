package com.electriccloud.plugin.spec.core.artifacts

import com.electriccloud.plugin.spec.core.ResponseDecorator
import com.electriccloud.spec.PluginSpockTestSupport

class ArtifactVersion implements ResponseDecorator {

    Artifact parentArtifact

    @Lazy
    String groupId = { parentArtifact.getGroupId() }()

    @Lazy
    String artifactKey = { parentArtifact.getArtifactKey() }()

    final String version
    final Map dslObject

    ArtifactVersion(Artifact parentArtifact, Map dslObject) {
        this.parentArtifact = parentArtifact
        this.dslObject = dslObject
        this.version = dslObject['version']
    }

    ArtifactVersion(String groupId, String artifactKey, String version) {
        this.parentArtifact = new Artifact(groupId, artifactKey)
        this.version = version
        this.dslObject = _retrieveDetails()

    }

    Map _retrieveDetails() {
        def response = (new PluginSpockTestSupport()).dsl("""
            getArtifactVersion( artifactVersionName: '$groupId:$artifactKey:$version' )
        """)
        return response['artifactVersion']
    }
}
