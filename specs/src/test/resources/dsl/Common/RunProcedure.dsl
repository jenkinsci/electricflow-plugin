package dsl

def projName = args.projectName
def procName = args.procedureName
def resName = args.resourceName ?: 'local'
def params = args.params

project projName, {
    procedure procName, {
        resourceName = resName
        params.each { k, defaultValue ->
            formalParameter k, defaultValue: defaultValue, {
                type = 'textarea'
            }
        }

        step procName, {
            resourceName = resName
            subproject = '/plugins/EC-Jenkins/project'
            subprocedure = procName

            params.each { k, v ->
                actualParameter k, '$[' + k + ']'
            }
        }
    }
}