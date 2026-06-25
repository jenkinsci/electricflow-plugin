function deleteDeployApplicationParameterRows() {
    console.log('deleteDeployApplicationParameterRows');
    var paramRows = document.querySelectorAll("._ef_da_row");
    [].forEach.call(paramRows, function (row) {
        row.parentNode.removeChild(row);
    });
}

function fillDeployApplicationParameters() {
    console.log('fillDeployApplicationParameters');
    deleteDeployApplicationParameterRows();

    if (!document.getElementById('ef_da_parameters')) {
        return;
    }

    var option = document.getElementById('ef_da_parameters').options[0];
    var param = '{}';
    if (option) {
        param = option.value;
    }
    var json = JSON.parse(param);

    var table = document.querySelector("[descriptorid='electricFlowDeployApplication'] table");
    if (table == null) {
        table = getElementByXpath("//select[@checkdependson='validationTrigger']/ancestor::table[@name]");
    }

    if (json) {

        if (json.runProcess) {
            var parameters;
            try {
                parameters = JSON.parse(json.runProcess.parameter);
            }
            catch (error) {
                parameters = json.runProcess.parameter;
            }


            if (table == null) {
                if (parameters.length) {
                    var el = document.createElement("div");
                    el.innerHTML = "&lt;br&gt;&lt;b&gt;Deploy Parameters&lt;/b&gt;";
                    el.className = "_ef_da_row";
                    var div = document.getElementById("ef_da_environmentName");
                    div = cbcd_insertAfter(div, el);
                    parameters.forEach(function (elem) {
                        var el_param = document.createElement("div");
                        el_param.className = "_ef_da_parameters _ef_da_row";
                        div = cbcd_insertAfter(div, el_param);
                        addParameterRowForDeployApplication(el_param, elem.actualParameterName, elem.value, false, false);
                    });
                }
            } else {
                var row1 = table.insertRow(table.rows.length - 2);
                row1.className = "_ef_da_row";
                if (parameters.length) {
                    row1.appendChild(createTitleForDeployApplication("Deploy Parameters"));
                    parameters.forEach(function (elem) {
                        var row = table.insertRow(table.rows.length - 2);
                        row.className = "_ef_da_parameters _ef_da_row";
                        addParameterRowForDeployApplication(row, elem.actualParameterName, elem.value, false, true);
                    });
                }
            }
        }
    }
}

function cbcd_insertAfter(referenceNode, newNode) {
    return referenceNode.parentNode.insertBefore(newNode, referenceNode.nextSibling);
}

function updateDeployJson() {
    var applicationName = document.getElementById("ef_da_applicationName").value;
    var applicationProcessName = document.getElementById("ef_da_applicationProcessName").value;
    var json = JSON.parse('{"runProcess":{"applicationName":"' + applicationName + '", "applicationProcessName":"' + applicationProcessName + '", "parameter":[]}}');

    var parameters = document.querySelectorAll("._ef_da_parameters");

    [].forEach.call(parameters, function (elem) {
        json.runProcess.parameter.push({
            'actualParameterName': elem.querySelector(".setting-name").textContent,
            'value': elem.querySelector(".setting-input").value
        })
    });


    document.getElementById('ef_da_parameters').options[0].value = JSON.stringify(json);
    document.getElementById('ef_da_parameters').options[0].textContent = JSON.stringify(json);
}

function createTitleForDeployApplication(title) {
    var td = document.createElement('td');
    td.colSpan = 4;
    var div = document.createElement('div');
    div.className = "section-header";
    div.textContent = title;
    td.appendChild(div);
    return td;
}

function addParameterRowForDeployApplication(row, label, value, isCheckbox, isTable) {
    var td1 = document.createElement(isTable ? 'td' : 'div');
    td1.className = 'setting-leftspace';
    td1.textContent = '';
    row.appendChild(td1);

    var td2 = document.createElement(isTable ? 'td' : 'div');
    td2.className = 'setting-name';
    td2.textContent = label;
    row.appendChild(td2);

    var td3 = document.createElement(isTable ? 'td' : 'div');
    td3.className = 'setting-main';
    var input1 = document.createElement('input');
    input1.name = 'parameterName';
    if (isCheckbox) {
        input1.type = "checkbox";
        input1.checked = value;
        input1.style = "width: auto;";
    }
    input1.value = value;
    input1.onchange = updateDeployJson;
    input1.className = 'setting-input';
    td3.appendChild(input1);
    row.appendChild(td3);

    var td4 = document.createElement(isTable ? 'td' : 'div');
    td4.className = 'setting-no-help';
    row.appendChild(td4);
}

function triggerExtraValidationForDeployApplication() {
    console.log('triggerExtraValidationForDeployApplication');
    document.getElementById("ef_da_validationTrigger").click();
}

function getElementByXpath(path) {
    return document.evaluate(path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
}

const behaviourConfig = {
    '.ef-deploy-config': {
        name: 'config',
        event: 'change',
        handler: deleteDeployApplicationParameterRows
    },
    '.ef-deploy-project': {
        name: 'project',
        event: 'change',
        handler: deleteDeployApplicationParameterRows
    },
    '#ef_da_applicationName': {
        name: 'application',
        event: 'change',
        handler: deleteDeployApplicationParameterRows
    },
    '#ef_da_applicationProcessName': {
        name: 'process',
        event: 'change',
        handler: fillDeployApplicationParameters
    },
    '#ef_da_parameters': {
        name: 'params',
        event: 'change',
        handler: fillDeployApplicationParameters
    },
    '.validate-before-apply-btn': {
        name: 'validate',
        event: 'click',
        handler: triggerExtraValidationForDeployApplication
    }
};

Object.entries(behaviourConfig).forEach(([selector, config]) => {
    Behaviour.specify(selector, config.name, 0, function(element) {
        element.addEventListener(config.event, config.handler);
    });
});

fillDeployApplicationParameters();
