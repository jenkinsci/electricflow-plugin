package com.electriccloud.plugin.spec.core.release

import com.electriccloud.plugin.spec.core.pipeline.Pipeline
import com.electriccloud.spec.PluginSpockTestSupport

class ReleasePipeline extends Pipeline {

    private final Release release

    ReleasePipeline(Release release) {
        super(release.getProjectName(), release['pipelineName'])
        this.release = release
    }

    @Override
    protected HashMap _retrievePipelineDetailsBy(String projectName, String name) {
        def result = (new PluginSpockTestSupport()).dsl("""
            getPipeline(
                projectName: '$projectName', 
                pipelineName: '${name}', 
                releaseName: '${release.getName()}'
            )
        """)

        return result['pipeline'] as HashMap<String, Object>
    }

    @Override
    String getPartName() {
        return super.getPartName() + '_' + release.getName()
    }
}
