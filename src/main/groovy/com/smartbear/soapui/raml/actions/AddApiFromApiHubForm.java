/**
 *  Copyright 2013 SmartBear Software, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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