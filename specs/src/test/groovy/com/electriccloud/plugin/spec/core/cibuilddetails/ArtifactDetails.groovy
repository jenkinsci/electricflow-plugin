package com.electriccloud.plugin.spec.core.cibuilddetails

import com.electriccloud.plugin.spec.core.ResponseDecorator

class ArtifactDetails implements ResponseDecorator {

    final Map dslObject

    @Lazy
    String artifactName = { dslObject['artifactName'] }()
    @Lazy
    String artifactVersion = { dslObject['artifactVersion'] }()
    @Lazy
    String artifactVersionName = { dslObject['artifactVersionName'] }()
    @Lazy
    String displayPath = { dslObject['displayPath'] }()
    @Lazy
    String fileName = { dslObject['fileName'] }()
    @Lazy
    String href = { dslObject['href'] }()
    @Lazy
    String repositoryName = { dslObject['repositoryName'] }()
    @Lazy
    String repositoryType = { dslObject['repositoryType'] }()
    @Lazy
    String size = { dslObject['size'] }()
    @Lazy
    String url = { dslObject['url'] }()

    ArtifactDetails(Map dslObject) {
        this.dslObject = dslObject
    }

}
