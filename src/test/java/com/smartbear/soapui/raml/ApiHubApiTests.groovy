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

import com.smartbear.soapui.apihub.ApiHubApi

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
