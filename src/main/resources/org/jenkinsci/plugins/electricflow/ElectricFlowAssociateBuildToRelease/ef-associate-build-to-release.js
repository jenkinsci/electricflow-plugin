function updateReleaseJson() {
    var procedureName = document.getElementById("ef_abtr_releaseName").value;
    var json = JSON.parse('{"release":{"releaseName":"' + releaseName + '"}}');
}

function triggerExtraValidationForSetJenkinsBuildDetails() {
    document.getElementById("ef_abtr_validationTrigger").click();
}

const behaviourConfig = {
    '.validate-before-apply-btn': {
        name: 'validate',
        event: 'click',
        handler: triggerExtraValidationForSetJenkinsBuildDetails
    }
};

Object.entries(behaviourConfig).forEach(([selector, config]) => {
    Behaviour.specify(selector, config.name, 0, function(element) {
        element.addEventListener(config.event, config.handler);
    });
});
