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

package com.smartbear.soapui.apihub

import groovy.json.JsonSlurper

class ApiHubApi {
    def get( String title, String format, int start )
    {
        def response = new URL( "http://api.apihub.com/v1/apis?title=" + URLEncoder.encode(title) +
                "&specFormat=" + URLEncoder.encode(format) +
                "&start=" + start ).text;

        return new JsonSlurper().parseText(response)
    }

    def getAllApis( String title, String format )
    {
        def result = get( title, format, 1 )

        while( result.total > result.items.size() )
        {
           def next = get( title, format, result.items.size()+1 )

           result.items.addAll( next.items )
        }

        return result
    }

    def getAllApis( String title )
    {
        def result1 = getAllApis( title, "swagger" )
        def result2 = getAllApis( title, "RAML" )

        def result = new ArrayList()
        def ids = new HashSet()

        result1.items.each{
            if( !ids.contains( it.id )){
                result.add( it )
                ids.add( it.id )
            }
        }

        result2.items.each{
            if( !ids.contains( it.id )){
                result.add( it )
                ids.add( it.id )
            }
        }

        result.sort(){ a,b->
            a.title.toLowerCase().compareTo( b.title.toLowerCase() )
        }

        return result
    }
}
