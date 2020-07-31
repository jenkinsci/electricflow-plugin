package com.electriccloud.plugin.spec.core.pipeline

import com.electriccloud.plugin.spec.core.ResponseDecorator
import com.electriccloud.plugin.spec.core.cibuilddetails.CiBuildDetailInfo
import com.electriccloud.plugin.spec.core.cibuilddetails.WithCiBuildDetails
import com.electriccloud.spec.PluginSpockTestSupport

class PipelineRun implements ResponseDecorator, WithCiBuildDetails {

    final Map dslObject

    @Lazy
    String id = { dslObject['flowRuntimeId'] }()

    private Pipeline parentPipeline

    @Lazy
    int number = {
        // flowRuntimeName contains <pipelineName>_<number>_<timestamp>
        String runName = dslObject['flowRuntimeName']

        String parentName = parentPipeline.getPartName()
        String part = runName.substring(parentName.size())

        String number = (part =~ /_(\d+)_.*/)[0][1]
        return Integer.parseInt(number)
    }()

    PipelineRun(Map dslObject) {
        this.dslObject = dslObject
    }

    PipelineRun(Map dslObject, Pipeline parentPipeline) {
        this.dslObject = dslObject
        this.parentPipeline = parentPipeline
    }

    Pipeline getParentPipeline() {
        if (parentPipeline == null) {
            parentPipeline = new Pipeline((String) dslObject['pipelineId'])
        }
        return parentPipeline
    }

    ArrayList<CiBuildDetailInfo> getCiBuildDetails() {
        def response = (new PluginSpockTestSupport()).dsl("""
            getCIBuildDetails(flowRuntimeId: '$id')
        """)

        ArrayList<CiBuildDetailInfo> result = response['ciBuildDetailInfo'].collect({ it ->
            return new CiBuildDetailInfo(it['displayName'], this)
        })

        return result
    }
}
