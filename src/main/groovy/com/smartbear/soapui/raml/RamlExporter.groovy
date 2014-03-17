package com.smartbear.soapui.raml

import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.wsdl.WsdlProject
import com.eviware.soapui.support.StringUtils
import org.raml.emitter.RamlEmitter
import org.raml.model.Raml

class RamlExporter {

    private final WsdlProject project

    public RamlExporter( WsdlProject project ) {
        this.project = project
    }

    String exportToFolder(String path, String title, RestService [] services, String baseUri ) {

        Raml raml = new Raml()

        raml.title = title
        raml.baseUri = baseUri

        services.each { exportRestService( it, raml ) }

        RamlEmitter emitter = new RamlEmitter()
        String fileName = createFileName( path, title )
        String ramlText = emitter.dump( raml )
        new File( fileName ).write( ramlText )

        return fileName
    }

    def exportRestService(RestService restService, Raml raml) {


    }

    private String createFileName( String path, String title )
    {
        return path + File.separatorChar + StringUtils.createFileName( title, '-' ) + ".raml"
    }
}
