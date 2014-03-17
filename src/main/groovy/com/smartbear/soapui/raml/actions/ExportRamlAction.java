package com.smartbear.soapui.raml.actions;

import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.rest.RestServiceFactory;
import com.eviware.soapui.impl.settings.XmlBeansSettingsImpl;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormOptionsField;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;
import com.smartbear.soapui.raml.RamlExporter;

public class ExportRamlAction extends AbstractSoapUIAction<WsdlProject>
{
    private static final String BASE_URI = Form.class.getName() + Form.BASEURI;
    private static final String TARGET_PATH = Form.class.getName() + Form.FOLDER;
    private static final String TITLE = Form.class.getName() + Form.TITLE;

    private XFormDialog dialog;

    public ExportRamlAction()
    {
        super( "Export RAML", "Creates a RAML definition for selected REST APIs" );
    }

    public void perform( WsdlProject project, Object param )
    {
        if( project.getInterfaces(RestServiceFactory.REST_TYPE).isEmpty())
        {
            UISupport.showErrorMessage("Project is missing REST APIs");
            return;
        }

        // initialize form
        XmlBeansSettingsImpl settings = project.getSettings();
        if( dialog == null )
        {
            dialog = ADialogBuilder.buildDialog(Form.class);

            dialog.setValue(Form.TITLE, project.getName());
            dialog.setValue(Form.BASEURI, settings.getString(BASE_URI, "" ));
            dialog.setValue(Form.FOLDER, settings.getString(TARGET_PATH, "" ));
        }

        XFormOptionsField apis = (XFormOptionsField) dialog.getFormField(Form.APIS);
        apis.setOptions(ModelSupport.getNames(project.getInterfaces(RestServiceFactory.REST_TYPE)));

        while( dialog.show() )
        {
            try
            {
                RamlExporter exporter = new RamlExporter( project );

                Object[] options = ((XFormOptionsField) dialog.getFormField(Form.APIS)).getSelectedOptions();
                if( options.length == 0 )
                {
                    throw new Exception( "You must select at least one REST API ");
                }

                RestService[] services = new RestService[options.length];
                for( int c = 0; c < options.length; c++ )
                {
                    services[c] = (RestService) project.getInterfaceByName( String.valueOf(options[c]) );
                    if( services[c].getEndpoints().length == 0 )
                    {
                        throw new Exception( "Selected APIs must contain at least one endpoint");
                    }
                }

                // double-check
                if( services.length == 0 )
                {
                    throw new Exception( "You must select at least one REST API to export");
                }

                String path = exporter.exportToFolder( dialog.getValue( Form.FOLDER ),
                        dialog.getValue(Form.TITLE), services,
                        dialog.getValue(Form.BASEURI));

                UISupport.showInfoMessage( "RAML definition has been created at [" + path + "]" );

                settings.setString(BASE_URI, dialog.getValue(Form.BASEURI));
                settings.setString(TARGET_PATH, dialog.getValue(Form.FOLDER));
                settings.setString(TITLE, dialog.getValue(Form.TITLE));

                break;
            }
            catch( Exception ex )
            {
                UISupport.showErrorMessage( ex );
            }
        }
    }

    @AForm( name = "Export RAML Definition", description = "Creates a RAML definition for selected REST APIs in this project" )
    public interface Form
    {
        @AField( name = "APIs", description = "Select which REST APIs to include in the RAML definition", type = AField.AFieldType.MULTILIST )
        public final static String APIS = "APIs";

        @AField( name = "Target Folder", description = "Where to save the RAML definition", type = AField.AFieldType.FOLDER )
        public final static String FOLDER = "Target Folder";

        @AField( name = "Title", description = "The API Title", type = AField.AFieldType.STRING )
        public final static String TITLE = "Title";

        @AField( name = "Base URI", description = "The RAML baseUri", type = AField.AFieldType.STRING )
        public final static String BASEURI = "Base Path";
    }
}
