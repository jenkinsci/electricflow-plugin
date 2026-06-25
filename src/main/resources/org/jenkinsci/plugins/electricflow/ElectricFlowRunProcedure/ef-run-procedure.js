function deleteProcedureParameterRows() {
    var paramRows = document.querySelectorAll("._ef_rp_row");
    [].forEach.call(paramRows, function (row) {
        row.parentNode.removeChild(row);
    });
}

function fillProcedureParameters() {
    deleteProcedureParameterRows();

    if (!document.getElementById('ef_rp_parameters')) {
        return;
    }

    var option = document.getElementById('ef_rp_parameters').options[0];
    var param = '{}';
    if (option) {
        param = option.value;
    }
    var json = JSON.parse(param);

    var table = document.querySelector("[descriptorid='electricFlowRunProcedure'] table");
    if (table == null) {
        table = getElementByXpath("//select[@checkdependson='validationTrigger']/ancestor::table[@name]");
    }

    if (json) {

        if (json.procedure) {
            var parameters;
            try {
                parameters = JSON.parse(json.procedure.parameters);
            }
            catch (error) {
                parameters = json.procedure.parameters;
            }

            if (table == null) {
                if (parameters.length) {
                    var el = document.createElement("div");
                    el.innerHTML = "&lt;br&gt;&lt;b&gt;Procedure Parameters&lt;/b&gt;";
                    el.className = "_ef_rp_row";
                    var div = document.getElementById("ef_rp_procedureName").closest('.jenkins-select');
                    div = cbcd_insertAfter(div, el);
                    parameters.forEach(function (elem) {
                        var el_param = document.createElement("div");
                        el_param.className = "_ef_rp_parameters _ef_rp_row";
                        div = cbcd_insertAfter(div, el_param);
                        addParameterRowForRunProcedure(el_param, elem.actualParameterName, elem.value, false, false);
                    });
                }
            } else {
                var row1 = table.insertRow(table.rows.length - 2);
                row1.className = "_ef_rp_row";
                if (parameters.length) {
                    row1.appendChild(createTitleForRunProcedure("Procedure Parameters"));
                    parameters.forEach(function (elem) {
                        var row = table.insertRow(table.rows.length - 2);
                        row.className = "_ef_rp_parameters _ef_rp_row";
                        addParameterRowForRunProcedure(row, elem.actualParameterName, elem.value, false, true);
                    });
                }
            }
        }
    }
}

function cbcd_insertAfter(referenceNode, newNode) {
    return referenceNode.parentNode.insertBefore(newNode, referenceNode.nextSibling);
}

function updateProcedureJson() {
    var procedureName = document.getElementById("ef_rp_procedureName").value;
    var json = JSON.parse('{"procedure":{"procedureName":"' + procedureName + '", "parameters":[]}}');

    var parameters = document.querySelectorAll("._ef_rp_parameters");

    [].forEach.call(parameters, function (elem) {
        json.procedure.parameters.push({
            'actualParameterName': elem.querySelector(".setting-name").textContent,
            'value': elem.querySelector(".setting-input").value
        })
    });


    document.getElementById('ef_rp_parameters').options[0].value = JSON.stringify(json);
    document.getElementById('ef_rp_parameters').options[0].textContent = JSON.stringify(json);
}

function createTitleForRunProcedure(title) {
    var td = document.createElement('td');
    td.colSpan = 4;
    var div = document.createElement('div');
    div.className = "section-header";
    div.textContent = title;
    td.appendChild(div);
    return td;
}

function addParameterRowForRunProcedure(row, label, value, isCheckbox, isTable) {
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
        if (value === "true") {
            input1.checked = value;
        }
        input1.style = "width: auto;";
    }
    input1.value = value;
    input1.onchange = updateProcedureJson;
    input1.className = 'setting-input';
    td3.appendChild(input1);
    row.appendChild(td3);

    var td4 = document.createElement(isTable ? 'td' : 'div');
    td4.className = 'setting-no-help';
    row.appendChild(td4);
}

function triggerExtraValidationForRunProcedure() {
    document.getElementById("ef_rp_validationTrigger").click();
}

function getElementByXpath(path) {
    return document.evaluate(path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
}

const behaviourConfig = {
    '.ef-procedure-config': {
        name: 'config',
        event: 'change',
        handler: deleteProcedureParameterRows
    },
    '.ef-procedure-project': {
        name: 'project',
        event: 'change',
        handler: deleteProcedureParameterRows
    },
    '#ef_rp_procedureName': {
        name: 'procedure',
        event: 'change',
        handler: deleteProcedureParameterRows
    },
    '#ef_rp_parameters': {
        name: 'params',
        event: 'change',
        handler: fillProcedureParameters
    },
    '.validate-before-apply-btn': {
        name: 'validate',
        event: 'click',
        handler: triggerExtraValidationForRunProcedure
    }
};

Object.entries(behaviourConfig).forEach(([selector, config]) => {
    Behaviour.specify(selector, config.name, 0, function(element) {
        element.addEventListener(config.event, config.handler);
    });
});

fillProcedureParameters();
