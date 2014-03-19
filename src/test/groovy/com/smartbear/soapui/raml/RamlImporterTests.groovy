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

package com.smartbear.soapui.raml

import com.eviware.soapui.impl.rest.HttpMethod
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder
import com.eviware.soapui.impl.wsdl.WsdlProject
import org.apache.xmlbeans.XmlString

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

        res = service.resources["/highvalue"]
        assertNotNull( res )
        def method = res.getRestMethodAt( 0 )
        assertNotNull( method )
        assertNotNull( method.params.limit )

        assertNotNull( method.representations )
        assertTrue( method.representations.length > 0 )

        def reps = method.representations.findAll { it.status.contains( 200 ) }

        assertEquals( 1, reps.size() )
        assertEquals( "Successful request.", reps[0].description )

        reps = method.representations.findAll { it.status.contains( 500 ) }

        assertEquals( 1, reps.size() )
        assertEquals( "Any other error.", reps[0].description )
    }

    public void testAllRamls()
    {
        new File( "src/test/resources").eachFile {
            if( it.name.endsWith( ".raml"))
            {
                Console.println( "Importing $it.name" )
                importRaml( it.name )
            }
        }
    }

    public void testComplexTypesAndTraits()
    {
        def service = importRaml( "muse.raml" )
        def resource = service.getResourceByFullPath( "/Pg/{version}/products")

        assertNotNull( resource )

        def method = resource.getRestMethodByName( "delete" )
        assertNull( method )

        resource = service.getResourceByFullPath( "/Pg/{version}/products/{productId}")

        assertNotNull( resource )

        method = resource.getRestMethodByName( "delete" )
        assertNotNull( method )

        assertNotNull( method.representations.find { it.status.contains( 200 )} )
        assertNotNull( method.representations.find { it.status.contains( 401 )} )
    }

    public static def importRaml( def path )
    {
        WsdlProject project = new WsdlProject()
        NativeRamlImporter importer = new NativeRamlImporter( project )
        importer.setRestMockService( project.addNewRestMockService( "TestRESTMock"))

        return importer.importRaml( new File( "src/test/resources/" + path ).toURI().toURL().toString());
    }

    public void testBaseType()
    {
        def service = importRaml( "ramlwithbasetype.raml")

        def resource = service.getResourceByFullPath( "/books")
        def method = resource.getRestMethodAt( 0 )
        assertEquals( 1, method.representations.length )
    }

    public void testTwitterRaml()
    {
        def service = importRaml("twitter.raml");

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
        assertEquals(HttpMethod.GET, method.method)

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

    public void testRamlWithParameters()
    {
        def service = importRaml("ramlwithparameters.raml");

        def resource = service.getResourceByFullPath( "/books")
        assertNotNull( resource )

        def method = resource.getRestMethodByName( "get" )
        assertNotNull( method )
        assertNotNull( method.params.getProperty( "numPages"))
        assertEquals( "The number of pages to return, not to exceed 10", method.params.numPages.description )
        assertNotNull( method.params.getProperty( "access_token"))
        assertEquals( "A valid access_token is required in get", method.params.access_token.description )

        assertNotNull( method.params.title )
        assertEquals( method.params.title.description, "Return books that have their title matching the given value")
    }

    public void testNeo4JRaml()
    {
        def service = importRaml("neo4j.raml")

        def res = service.resources["/node"]
        assertNotNull( res )

        res = res.getChildResourceByName("/{node_id}")
        assertNotNull( res )

        res = res.getChildResourceByName("/relationships")
        assertNotNull( res )
    }
}
