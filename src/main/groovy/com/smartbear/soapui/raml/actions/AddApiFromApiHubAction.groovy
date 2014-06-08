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

package com.smartbear.soapui.raml.actions

import com.eviware.soapui.SoapUI
import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.rest.mock.RestMockService
import com.eviware.soapui.impl.wsdl.WsdlProject
import com.eviware.soapui.plugins.ActionConfiguration
import com.eviware.soapui.support.UISupport
import com.eviware.soapui.support.action.support.AbstractSoapUIAction
import com.eviware.x.dialogs.Worker
import com.eviware.x.dialogs.XProgressMonitor
import com.eviware.x.form.XFormDialog
import com.eviware.x.form.support.ADialogBuilder
import com.smartbear.restplugin.SwaggerImporter
import com.smartbear.soapui.apihub.ApiHubApi
import com.smartbear.soapui.raml.RamlImporter

import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import java.awt.*

/**
 * Action that opens a dialog for browsing and adding APIs from the ApiHub directory
 */

@ActionConfiguration( actionGroup = "EnabledWsdlProjectActions", afterAction = "AddWadlAction", separatorBefore = true )
class AddApiFromApiHubAction extends AbstractSoapUIAction<WsdlProject> {

    private XFormDialog dialog = null
    private def apiEntries = [:]
    private def apiEntryList = []
    private JList apiList;

    public AddApiFromApiHubAction() {
        super("Add API from ApiHub", "Imports an API from the ApiHub API Directory");
    }

    void perform(WsdlProject project, Object o) {
        if (dialog == null) {
            dialog = ADialogBuilder.buildDialog(AddApiFromApiHubForm.class);
            def dlg = UISupport.dialogs.createProgressDialog("Loading API Directory...", 0, "", false)
            dlg.run(new Worker.WorkerAdapter() {
                Object construct(XProgressMonitor monitor) {
                    initEntries()
                }
            })

            def cnt = apiEntries.size()
            dialog.setValue(AddApiFromApiHubForm.STATUS, "$cnt APIs loaded")

            apiList = new JList( apiEntryList.toArray() )

            dialog.getFormField(AddApiFromApiHubForm.NAME).setProperty("component", new JScrollPane(apiList))
            dialog.getFormField(AddApiFromApiHubForm.NAME).setProperty("preferredSize", new Dimension(500, 150))

            dialog.getFormField(AddApiFromApiHubForm.DESCRIPTION).setProperty("preferredSize", new Dimension(500, 50))

            apiList.addListSelectionListener({ ListSelectionEvent e ->
                Object entry = apiList.selectedValue
                def apiEntry = apiEntries[entry]
                if( apiEntry != null ){
                    dialog.setValue(AddApiFromApiHubForm.DESCRIPTION, apiEntry.description)
                    dialog.setValue(AddApiFromApiHubForm.SPEC, getMetaDataEndpoint(apiEntry))
                }
            } as ListSelectionListener)
        }

        dialog.setValue(AddApiFromApiHubForm.DESCRIPTION, "")
        dialog.setValue(AddApiFromApiHubForm.SPEC, "")
        apiList.setSelectedIndex(-1)

        if (dialog.show()) {

            def apis = apiList.getSelectedValues()
            if( apis.length > 0 )
            {
                def dlg = UISupport.dialogs.createProgressDialog("Importing " + apis.length + " APIs", 0, "", false)
                dlg.run(new Worker.WorkerAdapter() {
                    Object construct(XProgressMonitor monitor) {
                        importApis( project, apis )
                    }
                })
            }
            else
                UISupport.showErrorMessage("Missing Specification to import")
        }
    }

    def importApis( WsdlProject project, def apis )
    {
        apis.each{
            def api = apiEntries[it]

            if( api.specs.RAML != null )
            {
                try {
                    // create the importer and import!
                    RamlImporter importer = new RamlImporter( project );
                    SoapUI.log( "Importing RAML from [" + api.specs.RAML.url + "]")

                    RestMockService mockService = null;

                    if( dialog.getBooleanValue( AddApiFromApiHubForm.GENERATE_MOCK ))
                    {
                        mockService = project.addNewRestMockService( "Generated MockService" );
                        importer.setRestMockService( mockService );
                    }

                    RestService restService = importer.importRaml(api.specs.RAML.url);

                    if( mockService != null )
                        mockService.setName( restService.getName() + " MockService" );

                    UISupport.select( restService );
                }
                catch( Throwable e )
                {
                    SoapUI.logError( e )
                }
            }
            else if( api.specs.swagger != null )
            {
                try {
                    SwaggerImporter importer = new SwaggerImporter( project );
                    SoapUI.log( "Importing Swagger from [" + api.specs.swagger.url + "]")

                    def result = importer.importSwagger( api.specs.swagger.url )

                    if( result.length > 0 )
                        UISupport.select( result[0] );
                }
                catch( Throwable e )
                {
                    SoapUI.logError( e )
                }
            }
        }
    }

    def initEntries() {

        boolean hasSwagger = false

        try {
            SoapUI.getSoapUICore().getExtensionClassLoader().loadClass( "com.smartbear.restplugin.SwaggerImporter" );
            hasSwagger = true
        } catch( ClassNotFoundException e ) {
            SoapUI.logError( e )
        }

        ApiHubApi api = new ApiHubApi()
        def apis = api.getAllApis( "" )
        apis.each {

            if( hasSwagger || (it.specs != null && it.specs.RAML != null && it.specs.RAML.url != null ))
            {
                def title = it.title + " ["

                if( it.specs != null )
                {
                    def hasRaml = false
                    if( it.specs.RAML != null && it.specs.RAML.url != null )
                    {
                        title += "RAML"
                        hasRaml = true
                    }

                    if( hasSwagger && it.specs.swagger != null && it.specs.swagger.url != null )
                    {
                        if( hasRaml )
                            title += ","
                        title += "swagger"
                    }
                }

                title += "]"

                apiEntryList.add( title )
                apiEntries[title] = it
            }
        }
    }

    String getMetaDataEndpoint( def entry ) {
        if( entry.specs != null )
        {
            if( entry.specs.RAML != null )
                return entry.specs.RAML.url

            if( entry.specs.swagger != null )
                return entry.specs.swagger.url

        }

        return "<missing supported metadata spec>"
    }
}
