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

import com.eviware.soapui.impl.rest.*
import com.eviware.soapui.impl.rest.support.RestParameter
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle
import com.eviware.soapui.impl.rest.support.RestUtils
import com.eviware.soapui.impl.wsdl.WsdlProject
import org.apache.xmlbeans.*
import org.raml.model.*
import org.raml.model.parameter.AbstractParam
import org.raml.model.parameter.UriParameter
import org.raml.parser.visitor.RamlDocumentBuilder

/**
 * A simple RAML updater - this initial version only adds new resources/methods/parameters
 *
 * @author Ole Lensmar
 */

class RamlUpdater {

    private static final String MEDIA_TYPE_EXTENSION = "{mediaTypeExtension}"
    private final WsdlProject project
    private String defaultMediaType
    private def baseUriParams = [:]
    private boolean updateParameters
    private UpdateInfo updateInfo;

    public RamlUpdater(WsdlProject project) {
        this.project = project
    }

    public void setUpdateParameters( boolean updateParameters )
    {
        this.updateParameters = updateParameters
    }

    public synchronized UpdateInfo updateFromRaml(RestService service, String url) {
        Raml raml = new RamlDocumentBuilder().build(new URL(url).openStream());
        updateInfo = new UpdateInfo( service );

        baseUriParams = extractUriParams(raml.baseUri, raml.baseUriParameters)
        if (baseUriParams.version != null)
            baseUriParams.version.defaultValue = raml.version

        // extract default media type
        if (raml.mediaType != null) {
            defaultMediaType = raml.mediaType
        }

        raml.resources.each {
            complementResource(service, it.key, it.value)
        }

        return updateInfo
    }

    def complementResource(RestService service, String path, Resource r) {

        def resource = service.resources[path]
        if (resource == null)
        {
            resource = service.addNewResource(getResourceName(r), path)
            updateInfo.addedResources.add( resource )
        }

        initResource(resource, r)

        r.resources.each {
            addChildResource(resource, it.key, it.value)
        }
    }

    String getResourceName(Resource r) {
        String name = r.displayName

        if (name == null)
            name = r.relativeUri

        if (name.endsWith(MEDIA_TYPE_EXTENSION))
            name = name.substring(0, name.length() - 20)

        return name
    }

    def addChildResource(RestResource resource, String path, Resource r) {

        def childResource = findChildResource( resource, path )
        if( childResource == null )
        {
            childResource = resource.addNewChildResource(getResourceName(r), path)
            updateInfo.addedResources.add( childResource )
        }

        initResource(childResource, r)

        r.resources.each {
            addChildResource(childResource, it.key, it.value)
        }
    }

    def findChildResource( RestResource resource, String path )
    {
        def result = null

        resource.childResourceList.each {
            if( it.path == path )
                result = it
        }

        return result
    }

    def initResource(RestResource resource, Resource r) {

        if (resource.description == null)
            resource.description = r.description

        def params = extractUriParams(r.uri, r.uriParameters)
        params.each {
            addParamFromNamedProperty(resource.params, ParameterStyle.TEMPLATE, it.key, it.value)
        }

        r.actions.each {

            def key = it.key.toString()
            if (Arrays.asList(HttpMethod.methodsAsStringArray).contains(key)) {
                def method = findRestMethod( resource, key )
                if (method == null) {
                    method = resource.addNewMethod(key.toLowerCase())
                    method.method = HttpMethod.valueOf(key.toUpperCase())
                    updateInfo.addedMethods.add( method )
                }

                initMethod(method, it.value)
            }
        }
    }

    RestMethod findRestMethod( RestResource resource, String verb )
    {
        RestMethod result = null

        resource.restMethodList.each {
            if( it.method.name().toLowerCase() == verb.toLowerCase())
                result = it
        }

        return result
    }

    public void initMethod(RestMethod method, Action action) {

        method.description = action.description

        action.queryParameters?.each {
            addParamFromNamedProperty(method.params, ParameterStyle.QUERY, it.key, it.value)
        }

        action.headers?.each {
            addParamFromNamedProperty(method.params, ParameterStyle.HEADER, it.key, it.value)
        }

        if (action.body != null)
            addRequestBody(method, action.body)

        if (action.responses != null)
            addResponses(method, action.responses)

        if (method.requestCount == 0) {
            initDefaultRequest(method.addNewRequest("Request 1"))
        }
    }

    RestRequest initDefaultRequest(RestRequest request) {
        if (defaultMediaType != null) {
            def headers = request.requestHeaders
            headers.Accept = [defaultMediaType]
            request.requestHeaders = headers
        }

        return request
    }

    private addRequestBody(RestMethod method, Map body) {
        body.each {
            def reps = method.getRepresentations( RestRepresentation.Type.REQUEST, it.value.type )
            def rep = ( reps != null && reps.length > 0 ) ? reps[0] : method.addNewRepresentation(RestRepresentation.Type.REQUEST)

            MimeType mt = it.value
            rep.mediaType = mt.type

            if (rep.mediaType.equals("application/x-www-form-urlencoded") ||
                    rep.mediaType.equals("multipart/form-data")) {
                mt.formParameters?.each {
                    def name = it.key
                    if (it.value.size() > 0)
                        addParamFromNamedProperty(method.params, ParameterStyle.QUERY, name, it.value.get(0))
                }
            }

            if (mt.example != null) {
                def request = initDefaultRequest(method.addNewRequest("Sample Request"))
                request.mediaType = mt.type
                request.requestContent = mt.example
            }
        }
    }

    private RestParameter addParamFromNamedProperty(def params, def style, def name, AbstractParam p) {

        RestParameter param = params.getProperty(name)
        if (param == null )
        {
            if( !updateParameters )
                return param

            param = params.addProperty(name)
            updateInfo.addedParameters.add( param )
        }
        else if( !updateParameters )
        {
            return param
        }

        param.style = style

        if (param.description == null || param.description == "")
            param.description = p.description

        if (param.defaultValue == null || param.defaultValue == "")
            param.defaultValue = p.defaultValue

        param.required = p.required

        if (param.options == null || param.options.length == 0)
            param.options = p.enumeration

        if (param.type == null) {
            param.type = XmlString.type.name

            switch (p.type) {
                case ParamType.NUMBER: param.type = XmlDouble.type.name; break;
                case ParamType.INTEGER: param.type = XmlInteger.type.name; break;
                case ParamType.DATE: param.type = XmlDate.type.name; break;
                case ParamType.BOOLEAN: param.type = XmlBoolean.type.name; break;
            }
        }

        return param
    }

    private addResponses(RestMethod method, Map responses) {
        responses?.each {

            int statusCode = Integer.parseInt(it.key)
            Response r = it.value

            if (r.body == null || r.body.isEmpty()) {
                def representation = method.representations.find {
                    it.status.contains(statusCode)
                }

                if (representation == null) {
                    representation = method.addNewRepresentation(
                            statusCode < 400 ? RestRepresentation.Type.RESPONSE : RestRepresentation.Type.FAULT)

                    representation.status = [statusCode]
                } else if (!representation.status.contains(statusCode)) {
                    representation.status = representation.status + statusCode
                }

                representation.description = r.description

            } else r.body?.each {
                MimeType mt = it.value

                def representation = method.representations.find {
                    it.status.contains(statusCode) && (mt.type == null || mt.type.equals(it.mediaType))
                }

                if (representation == null) {
                    representation = method.addNewRepresentation(
                            statusCode < 400 ? RestRepresentation.Type.RESPONSE : RestRepresentation.Type.FAULT)

                    representation.status = [statusCode]
                    representation.mediaType = mt.type
                } else if (!representation.status.contains(statusCode)) {
                    representation.status = representation.status + statusCode
                }

                representation.description = r.description
            }
        }
    }

    private static Map extractUriParams(def path, def uriParameters) {
        def uriParams = [:]

        RestUtils.extractTemplateParams(path).each {
            def p = new UriParameter()
            p.displayName = it
            uriParams[it] = p
        }

        uriParameters?.each {
            uriParams[it.key] = it.value
        }

        uriParams.each {
            it.value.required = true
        }

        return uriParams
    }

    public static class UpdateInfo
    {
        RestService restService;

        List<RestResource> addedResources = new ArrayList<RestResource>();
        List<RestMethod> addedMethods = new ArrayList<RestMethod>();
        List<RestParameter> addedParameters = new ArrayList<RestParameter>();

        public UpdateInfo( RestService service )
        {
            this.restService = service
        }
    }
}
