package com.smartbear.soapui.raml

/**
 * Created by ole on 17/03/14.
 */
class RamlUpdaterTests extends GroovyTestCase {

    public void testUpdate()
    {
        def service = RamlImporterTests.importRaml( "update/mythicallogistics_1.raml")
        assertEquals( 2, service.resourceList.size())

        def resource = service.getResourceByFullPath( "/api/b2bshipping/{version}/shipment/{shipment_id}")
        assertNotNull( resource )
        assertEquals( 1, resource.restMethodCount )

        RamlUpdater updater = new RamlUpdater( service.project )
        updater.setUpdateParameters( true )

        RamlUpdater.UpdateInfo info = updater.updateFromRaml( service, new File( "src/test/resources/update/mythicallogistics_2.raml").toURL().toString());

        assertEquals( 1, info.addedParameters.size())
        assertEquals( 1, info.addedResources.size())
        assertEquals( 2, info.addedMethods.size())
        assertEquals( service, info.restService )

        // added resource at root level?
        assertEquals( 3, service.resourceList.size())

        // added method?
        assertEquals( 2, resource.restMethodCount )

        // find added method
        resource = service.resources["/locations"]
        assertNotNull( resource )

        // find added parameter
        resource = service.resources["/quote"]
        def method = resource.restMethodList[0]
        assertNotNull( method.getParams().getProperty("temperature"))
    }
}
