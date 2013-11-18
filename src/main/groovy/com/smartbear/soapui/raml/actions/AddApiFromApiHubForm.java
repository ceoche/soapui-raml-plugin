package com.smartbear.soapui.raml.actions;

import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;

@AForm(name = "Add an API from ApiHub", description = "Imports an API from the ApiHub Directory")
public interface AddApiFromApiHubForm {
    @AField(description = "Status", type = AField.AFieldType.LABEL)
    public final static String STATUS = "Status";

    @AField(description = "API Name", type = AField.AFieldType.COMPONENT)
    public final static String NAME = "Name";

    @AField(description = "API Description", type = AField.AFieldType.INFORMATION)
    public final static String DESCRIPTION = "Description";

    @AField(description = "API Definition", type = AField.AFieldType.LABEL)
    public final static String SPEC = "Definition";
}