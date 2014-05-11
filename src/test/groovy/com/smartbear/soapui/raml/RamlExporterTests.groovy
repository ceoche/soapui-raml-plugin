package com.smartbear.soapui.raml

import com.eviware.soapui.impl.rest.RestMethod
import com.eviware.soapui.impl.rest.RestRepresentation
import com.eviware.soapui.impl.rest.RestRequest
import com.eviware.soapui.impl.rest.RestRequestInterface
import com.eviware.soapui.impl.rest.RestResource
import com.eviware.soapui.impl.rest.RestService
import com.eviware.soapui.impl.rest.RestServiceFactory
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder
import com.eviware.soapui.impl.wsdl.WsdlProject
import org.raml.model.Raml
import org.raml.parser.visitor.RamlDocumentBuilder

/**
 * Created by ole on 17/03/14.
 */
class RamlExporterTests extends GroovyTestCase {

    public void testExport()
    {
        WsdlProject project = new WsdlProject()
        RestService restService = project.addNewInterface( "Test API", RestServiceFactory.REST_TYPE)
        restService.setBasePath( "/api/{version}" )

        RestResource resource = restService.addNewResource( "Cars", "/cars")
        resource.addProperty( "version").style = RestParamsPropertyHolder.ParameterStyle.TEMPLATE

        resource = resource.addNewChildResource( "Car", "{make}" )
        resource.addProperty( "make").style = RestParamsPropertyHolder.ParameterStyle.TEMPLATE

        RestMethod method = resource.addNewMethod( "Get Car")
        method.setMethod( RestRequestInterface.HttpMethod.GET )
        RestRepresentation representation = method.addNewRepresentation( RestRepresentation.Type.RESPONSE )
        representation.mediaType = "application/json"
        representation.status = [200]

        method = resource.addNewMethod( "Create Car")
        method.setMethod( RestRequestInterface.HttpMethod.POST )
        representation = method.addNewRepresentation( RestRepresentation.Type.REQUEST )
        representation.mediaType = "application/json"
        representation = method.addNewRepresentation( RestRepresentation.Type.RESPONSE )
        representation.mediaType = "application/json"
        representation.status = [200]

        RestRequest request = method.addNewRequest( "Test");
        request.mediaType = "application/json"
        request.requestContent = "{ \"test\" : \"value\" }"

        RamlExporter exporter = new RamlExporter( project )
        exporter.setCreateSampleBodies( true )
        String str = exporter.createRaml( restService.name, restService, restService.getBasePath(), "v1" )

        Console.println( str )

        assertNotNull( str )

        Raml raml = new RamlDocumentBuilder().build( new StringReader(str));
        assertEquals( "Test API", raml.title)
        assertEquals( "/api/{version}", raml.baseUri )
        assertTrue( raml.baseUriParameters.containsKey( "version"))
        assertTrue( raml.baseUriParameters.get("version").required)
        assertTrue( raml.getResource( "/cars").baseUriParameters.isEmpty())
    }
}
