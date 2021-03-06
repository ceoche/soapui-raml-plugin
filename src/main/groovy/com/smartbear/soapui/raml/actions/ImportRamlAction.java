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

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.rest.mock.RestMockService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.PathUtils;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.dialogs.Worker;
import com.eviware.x.dialogs.XProgressDialog;
import com.eviware.x.dialogs.XProgressMonitor;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AField.AFieldType;
import com.eviware.x.form.support.AForm;
import com.smartbear.soapui.raml.RamlImporter;

import java.io.File;

/**
 * Shows a simple dialog for importing a RAML definition
 *
 * @author Ole Lensmar
 */

public class ImportRamlAction extends AbstractSoapUIAction<WsdlProject> {
    private XFormDialog dialog;

    public ImportRamlAction() {
        super("Import RAML Definition", "Imports a RAML definition into SoapUI");
    }

    public void perform(final WsdlProject project, Object param) {
        // initialize form
        if (dialog == null) {
            dialog = ADialogBuilder.buildDialog(Form.class);
            dialog.setBooleanValue( Form.CREATE_REQUESTS, true );
        } else {
            dialog.setValue(Form.RAML_URL, "");
        }


        while (dialog.show()) {
            try {
                // get the specified URL
                String url = dialog.getValue(Form.RAML_URL).trim();
                if (StringUtils.hasContent(url)) {
                    // expand any property-expansions
                    String expUrl = PathUtils.expandPath(url, project);

                    // if this is a file - convert it to a file URL
                    if (new File(expUrl).exists())
                        expUrl = new File(expUrl).toURI().toURL().toString();

                    XProgressDialog dlg = UISupport.getDialogs().createProgressDialog("Importing API", 0, "", false);
                    final String finalExpUrl = expUrl;
                    dlg.run(new Worker.WorkerAdapter() {
                        public Object construct(XProgressMonitor monitor) {

                            try {
                                // create the importer and import!
                                RamlImporter importer = new RamlImporter(project);
                                importer.setCreateSampleRequests( dialog.getBooleanValue(Form.CREATE_REQUESTS));
                                SoapUI.log( "Importing RAML from [" + finalExpUrl + "]");
                                SoapUI.log( "CWD:" + new File(".").getCanonicalPath());
                                RestMockService mockService = null;

                                if( dialog.getBooleanValue( Form.GENERATE_MOCK ))
                                {
                                    mockService = project.addNewRestMockService( "Generated MockService" );
                                    importer.setRestMockService( mockService );
                                }

                                RestService restService = importer.importRaml(finalExpUrl);

                                if( mockService != null )
                                    mockService.setName( restService.getName() + " MockService" );

                                UISupport.select(restService);

                                return restService;
                            } catch (Exception e) {
                                SoapUI.logError(e);
                            }

                            return null;
                        }
                    });

                    break;
                }
            } catch (Exception ex) {
                UISupport.showErrorMessage(ex);
            }
        }
    }

    @AForm(name = "Add RAML Definition", description = "Creates a REST API from the specified RAML definition")
    public interface Form {
        @AField(name = "RAML Definition", description = "Location or URL of RAML definition", type = AFieldType.FILE)
        public final static String RAML_URL = "RAML Definition";

        @AField(name = "Create Requests", description = "Create sample requests for imported methods", type = AFieldType.BOOLEAN)
        public final static String CREATE_REQUESTS = "Create Requests";

        @AField( name = "Generate MockService", description = "Generate a REST Mock Service from the RAML definition", type = AField.AFieldType.BOOLEAN )
        public final static String GENERATE_MOCK = "Generate MockService";
    }

}
