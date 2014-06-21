package com.smartbear.soapui.raml.actions;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.analytics.Analytics;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.rest.mock.RestMockService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.support.UISupport;
import com.eviware.x.dialogs.Worker;
import com.eviware.x.dialogs.XProgressMonitor;
import com.eviware.x.form.XFormDialog;
import com.smartbear.soapui.raml.RamlImporter;

import java.io.File;

/**
* Created by ole on 21/06/14.
*/
public class RamlImporterWorker extends Worker.WorkerAdapter {
    private final String finalExpUrl;
    private WsdlProject project;
    private XFormDialog dialog;

    public RamlImporterWorker(String finalExpUrl, WsdlProject project, XFormDialog dialog) {
        this.finalExpUrl = finalExpUrl;
        this.project = project;
        this.dialog = dialog;
    }

    public Object construct(XProgressMonitor monitor) {

        try {
            // create the importer and import!
            RamlImporter importer = new RamlImporter(project);
            importer.setCreateSampleRequests( dialog.getBooleanValue(CreateRamlProjectAction.Form.CREATE_REQUESTS));
            SoapUI.log("Importing RAML from [" + finalExpUrl + "]");
            SoapUI.log( "CWD:" + new File(".").getCanonicalPath());
            RestMockService mockService = null;

            if( dialog.getBooleanValue( CreateRamlProjectAction.Form.GENERATE_MOCK ))
            {
                mockService = project.addNewRestMockService( "Generated MockService" );
                importer.setRestMockService( mockService );
            }

            RestService restService = importer.importRaml(finalExpUrl);

            if( mockService != null )
                mockService.setName( restService.getName() + " MockService" );

            UISupport.select(restService);
            Analytics.trackAction("ImportRAML");

            return restService;
        } catch (Exception e) {
            SoapUI.logError(e);
        }

        return null;
    }
}
