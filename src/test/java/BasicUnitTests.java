import static org.junit.Assert.assertTrue;

import hudson.util.FormValidation;
import java.util.regex.Pattern;
import org.jenkinsci.plugins.electricflow.Utils;
import org.jenkinsci.plugins.electricflow.ui.FieldValidationStatus;
import org.jenkinsci.plugins.electricflow.ui.SelectItemValidationWrapper;
import org.junit.Test;

public class BasicUnitTests {

    @Test
    public void testSelectItemValidationWrapper() {
        SelectItemValidationWrapper selectItemValidationWrapper =
                new SelectItemValidationWrapper(FieldValidationStatus.OK, "validation message", "value");
        System.out.println(selectItemValidationWrapper.getJsonStr());
        String selectItemValidationWrapperJsonStr = selectItemValidationWrapper.getJsonStr();

        assertTrue(Pattern.compile(".*\"validationMessage\":\"validation message\".*", Pattern.DOTALL)
                .matcher(selectItemValidationWrapperJsonStr)
                .matches());
        assertTrue(Pattern.compile(".*\"validationStatus\":\"OK\".*", Pattern.DOTALL)
                .matcher(selectItemValidationWrapperJsonStr)
                .matches());
        assertTrue(Pattern.compile(".*\"value\":\"value\".*", Pattern.DOTALL)
                .matcher(selectItemValidationWrapperJsonStr)
                .matches());
    }

    @Test
    public void checkValidateValueOnEmpty() {
        FormValidation formValidation;

        formValidation = Utils.validateValueOnEmpty("", "Field name");
        System.out.println(formValidation.getMessage());
        assertTrue(formValidation.getMessage().equals("Field name field should not be empty."));
        assertTrue(formValidation.kind == FormValidation.Kind.WARNING);

        formValidation = Utils.validateValueOnEmpty("test", "Field name");
        assertTrue(formValidation.kind == FormValidation.Kind.OK);
    }
}
