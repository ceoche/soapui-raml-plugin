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
import com.eviware.soapui.impl.wsdl.support.PathUtils;
import com.eviware.soapui.plugins.ActionConfiguration;
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
import com.smartbear.soapui.raml.RamlUpdater;

import java.awt.*;
import java.io.File;

/**
 * Shows a simple dialog for importing a RAML definition
 *
 * @author Ole Lensmar
 */

@ActionConfiguration( actionGroup = "RestServiceActions" )
public class UpdateFromRamlAction extends AbstractSoapUIAction<RestService> {
    private XFormDialog dialog;

    public UpdateFromRamlAction() {
        super("Update from RAML Definition", "Updates this REST Service from a RAML definition");
    }

    public void perform(final RestService restService, Object param) {
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
                    String expUrl = PathUtils.expandPath(url, restService);

                    // if this is a file - convert it to a file URL
                    if (new File(expUrl).exists())
                        expUrl = new File(expUrl).toURI().toURL().toString();

                    final XProgressDialog dlg = UISupport.getDialogs().createProgressDialog("Importing API", 0, "", false);
                    final String finalExpUrl = expUrl;
                    dlg.run(new Worker.WorkerAdapter() {
                        public Object construct(XProgressMonitor monitor) {

                            try {
                                // create the updater and import!
                                RamlUpdater updater = new RamlUpdater(restService.getProject());
                                updater.setCreateSampleRequests( dialog.getBooleanValue( Form.CREATE_REQUESTS ));
                                updater.setUpdateParameters( dialog.getBooleanValue( Form.UPDATE_PARAMS ));
                                SoapUI.log( "Updating RAML from [" + finalExpUrl + "]");

                                RamlUpdater.UpdateInfo updateInfo = updater.updateFromRaml(restService,finalExpUrl);
                                UISupport.select(updateInfo.getRestService());

                                UISupport.showExtendedInfo(  "RAML Update", "The following was added from the specified RAML file",
                                        "Added Resources: " + updateInfo.getAddedResources().size() + "<br/>" +
                                        "Added Methods: " + updateInfo.getAddedMethods().size() + "<br/>" +
                                        "Added Parameters: " + updateInfo.getAddedParameters().size(),
                                        new Dimension( 300, 200 ));

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

    @AForm(name = "Update From RAML Definition", description = "Updates the selected REST API from the specified RAML definition")
    public interface Form {

        @AField(name = "RAML Definition", description = "Location or URL of RAML definition", type = AFieldType.FILE)
        public final static String RAML_URL = "RAML Definition";

        @AField(name = "Create Requests", description = "Create sample requests for imported methods", type = AFieldType.BOOLEAN)
        public final static String CREATE_REQUESTS = "Create Requests";

        @AField( name = "Update Parameters", description = "Will update and add new parameter definitions", type = AField.AFieldType.BOOLEAN )
        public final static String UPDATE_PARAMS = "Update Parameters";
    }

}
