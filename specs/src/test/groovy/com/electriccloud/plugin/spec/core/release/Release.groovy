package com.electriccloud.plugin.spec.core.release

import com.electriccloud.plugin.spec.JenkinsHelper
import com.electriccloud.plugin.spec.core.cibuilddetails.CiBuildDetailInfo
import com.electriccloud.plugin.spec.core.cibuilddetails.WithCiBuildDetails
import com.electriccloud.plugin.spec.core.pipeline.Pipeline
import com.electriccloud.spec.PluginSpockTestSupport

class Release implements WithCiBuildDetails {

    public final String projectName
    public final String name

    @Lazy
    Map dslObject = { _retrieveReleaseDetails() }()

    @Lazy
    ReleasePipeline releasePipeline = { new ReleasePipeline(this) }()

    Release(String projectName, String name) {
        this.projectName = projectName
        this.name = name
    }

    private HashMap<String, Object> _retrieveReleaseDetails() {
        def result = (new JenkinsHelper()).dsl("""
            getRelease projectName: '$projectName', releaseName: '${name}'
        """)

        return result['release'] as HashMap<String, Object>
    }

    String getProjectName() {
        return projectName
    }

    String getName() {
        return name
    }

    @Override
    ArrayList<CiBuildDetailInfo> getCiBuildDetails() {
        def cbds = (new PluginSpockTestSupport()).dsl("""
            getCIBuildDetails(releaseName: '$name', projectName: '$projectName')
        """)

        ArrayList<CiBuildDetailInfo> result = []

        for (def it : cbds['ciBuildDetailInfo']) {
            String name = (String) it['ciBuildDetail']['ciBuildDetailName']
            result.push(new CiBuildDetailInfo(name, this))
        }

        return result
    }
}
