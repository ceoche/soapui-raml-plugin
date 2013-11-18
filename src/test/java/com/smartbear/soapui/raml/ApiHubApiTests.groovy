package com.smartbear.soapui.raml

import com.smartbear.soapui.apihub.ApiHubApi

/**
 * Created with IntelliJ IDEA.
 * User: ole
 * Date: 18/11/13
 * Time: 09:41
 * To change this template use File | Settings | File Templates.
 */
class ApiHubApiTests extends GroovyTestCase {

    public void testApi()
    {
        def api = new ApiHubApi()
        def result1 = api.getAllApis( "", "swagger" )

        assertTrue result1.items.size() == result1.total

        def result2 = api.getAllApis( "", "RAML" )

        assertTrue result2.items.size() == result2.total

        def result = new ArrayList()
        def ids = new HashSet()
        def doubles = 0

        result1.items.each{
            if( !ids.contains( it.id )){
                result.add( it )
                ids.add( it.id )
            }
            else doubles++
        }

        result2.items.each{
            if( !ids.contains( it.id )){
                result.add( it )
                ids.add( it.id )
            }
            else doubles++
        }


        assertTrue( result.size() == result1.total + result2.total - doubles )
    }
}
