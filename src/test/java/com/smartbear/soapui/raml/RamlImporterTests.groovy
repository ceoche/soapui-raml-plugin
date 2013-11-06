package com.smartbear.soapui.raml

import com.eviware.soapui.impl.rest.RestRequestInterface
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder
import com.eviware.soapui.impl.wsdl.WsdlProject
import org.apache.xmlbeans.XmlString

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class RamlImporterTests extends GroovyTestCase{

    public void testBitlyRaml()
    {
        def service = importRaml( "bitly.raml" )

        assertEquals( "bit.ly API", service.name);
        assertEquals( "https://api-ssl.bitly.com", service.endpoints[0])
        assertEquals( "/{version}", service.basePath )
        assertFalse( service.resourceList.empty )


        def res = service.resources["/expand"]
        assertNotNull( res )
        assertTrue( res.params.hasProperty("version"))
        assertEquals( RestParamsPropertyHolder.ParameterStyle.TEMPLATE, res.getParams().getProperty("version").style  )
        assertEquals( "v3", res.params.version.defaultValue )
        assertTrue( res.params.version.required )

    }

    def importRaml( def path )
    {
        WsdlProject project = new WsdlProject()
        RamlImporter importer = new RamlImporter( project )

        return importer.importRaml( new File( "src/test/resources/" + path ).toURI().toURL().toString());
    }

    public void testTwitterRaml()
    {
        def service = importRaml("twitter-short.raml");

        assertEquals( "Twitter API", service.name);
        assertEquals( "https://api.twitter.com", service.endpoints[0])
        assertEquals( "/{version}", service.basePath )
        assertFalse( service.resourceList.empty )


        def res = service.resources["/statuses"]
        assertNotNull( res )
        assertTrue( res.params.hasProperty("version"))
        assertEquals( RestParamsPropertyHolder.ParameterStyle.TEMPLATE, res.getParams().getProperty("version").style  )
        assertEquals( "1.1", res.params.version.defaultValue )

        res = res.getChildResourceByName( "/mentions_timeline" )
        assertNotNull( res )
        assertTrue( res.getParams().hasProperty("mediaTypeExtension"))
        assertEquals( RestParamsPropertyHolder.ParameterStyle.TEMPLATE, res.getParams().getProperty("mediaTypeExtension").style  )
        assertEquals( ".json", res.getParams().getProperty("mediaTypeExtension").defaultValue )

        // version should only be defined on root resources
        assertFalse( res.params.hasProperty("version"))

        def method = res.getRestMethodByName( "get")
        assertNotNull( method )
        assertEquals(RestRequestInterface.RequestMethod.GET, method.method)

        assertTrue( method.params.hasProperty( "count"))
        assertTrue( method.params.hasProperty( "since_id"))
        assertTrue( method.params.hasProperty( "max_id"))
        assertTrue( method.params.hasProperty( "contributor_details"))
        assertEquals( XmlString.type.name, method.params.getProperty( "contributor_details").type )

        assertTrue( method.representations[0].status.contains( 200 ))

        // these come from traits
        assertTrue( method.params.hasProperty( "include_entities"))
        assertEquals( 6, method.params.getProperty( "include_entities").options.length)
        assertTrue( method.params.getProperty( "include_entities").options.contains( "true"))
        assertTrue( method.params.hasProperty( "trim_user"))
    }
}
