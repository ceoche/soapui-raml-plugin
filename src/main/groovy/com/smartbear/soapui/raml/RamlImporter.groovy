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

import com.esotericsoftware.yamlbeans.YamlReader
import com.eviware.soapui.impl.rest.*
import com.eviware.soapui.impl.rest.RestRequestInterface.RequestMethod
import com.eviware.soapui.impl.rest.support.RestParameter
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle
import com.eviware.soapui.impl.rest.support.RestUtils
import com.eviware.soapui.impl.wsdl.WsdlProject
import org.apache.xmlbeans.*

/**
 * A simple RAML importer
 *
 * @author Ole Lensmar
 */

class RamlImporter {

    private static final String MEDIA_TYPE_EXTENSION = "{mediaTypeExtension}"
    private final WsdlProject project
    private String defaultMediaType
    private String defaultMediaTypeExtension
    private def baseUriParams = [:]
    private def resourceTypes = [:]
    private def traits = [:]

    public RamlImporter(WsdlProject project) {
        this.project = project
    }

    public RestService importRaml(String url) {

        String txt = new URL(url).openStream().text

        txt = expandIncludes(txt, url)

        def reader = new YamlReader(txt)
        def raml = reader.read()
        def service = createRestService(raml)

        baseUriParams = extractUriParams(raml.baseUri, raml.baseUriParameters)
        if (baseUriParams.version != null)
            baseUriParams.version.default = raml.version

        // extract default media type
        if (raml.mediaType != null) {
            defaultMediaType = raml.mediaType
            defaultMediaTypeExtension = defaultMediaType
            if (defaultMediaTypeExtension.contains('/'))
                defaultMediaTypeExtension = defaultMediaTypeExtension.substring(defaultMediaTypeExtension.lastIndexOf('/') + 1)
            if (defaultMediaTypeExtension.contains('-'))
                defaultMediaTypeExtension = defaultMediaTypeExtension.substring(defaultMediaTypeExtension.lastIndexOf('-') + 1)
        }

        raml.resourceTypes?.each {
            it.each {
                resourceTypes[it.key] = it.value
            }
        }

        raml.traits?.each {
            it.each {
                traits[it.key] = it.value
            }
        }

        raml.each {
            if (it.key.startsWith('/')) {
                addResource(service, it)
            }
        }

        return service
    }

    String expandIncludes(String s, String path) {
        def ix = s.indexOf("!include ")
        while (ix >= 0) {
            def endIx = ix + 8
            while (++endIx < s.length()) {
                if (s.charAt(endIx).isWhitespace())
                    break
            }

            // only supports relative paths for now
            def file = s.substring(ix + 9, endIx)
            def newPath = ""

            // absolute?
            if (file.toLowerCase().startsWith("http") && file.contains("://")) {
                newPath = file
            } else {
                def ix2 = path.lastIndexOf('/')
                newPath = path.substring(0, ix2 + 1) + file
            }

            try {
                def inc = new URL(newPath).text
                def ix2 = inc.indexOf("---")
                if (ix2 > 0)
                    inc = inc.substring(ix2 + 3).trim()

                s = s.substring(0, ix) + "\r\n" + inc + s.substring(endIx)
            }
            catch (e) {
                Console.println("Error expanding include: " + e.message)
                s = s.substring(0, ix) + s.substring(endIx)
            }

            ix = s.indexOf("!include ")
        }

        return s
    }

    def addResource(RestService service, def it) {

        def resource = service.addNewResource(getResourceName(it, null), it.key)
        initResource(resource, it, it.value.is, null)

        def traits = it.value.is
        it.value.each {
            if (it.key.startsWith('/')) {
                addChildResource(resource, it, traits)
            }
        }
    }

    String getResourceName(def it, Map parameters) {
        def name = replaceParameters(it.value.displayName, parameters)

        if (name == null)
            name = replaceParameters(it.key, parameters)

        if (name.endsWith(MEDIA_TYPE_EXTENSION))
            name = name.substring(0, name.length() - 20)

        return name
    }

    def addChildResource(RestResource resource, def it, def resourceTraits) {
        def childResource = resource.addNewChildResource(getResourceName(it, null), it.key)
        def traits = it.value.is == null ? resourceTraits : it.value.is + resourceTraits

        initResource(childResource, it, traits, null)

        if (baseUriParams.version != null)
            childResource.params.removeProperty("version")

        it.value.each {
            if (it.key.startsWith('/')) {
                addChildResource(childResource, it, traits)
            }
        }
    }

    def initResource(RestResource resource, def res, def resourceTraits, Map parameters) {

        //Console.println( "Initializing resource with path " + restResource.fullPath )

        if (resource.description != null)
            resource.description = replaceParameters(res.value.description, parameters)

        if (res.key.contains(MEDIA_TYPE_EXTENSION)) {
            RestParameter p = resource.params.addProperty("mediaTypeExtension")
            p.style = ParameterStyle.TEMPLATE
            p.required = true
            p.defaultValue = "." + defaultMediaTypeExtension
        }

        def params = extractUriParams(res.key, res.value.uriParmeters)
        params.putAll(baseUriParams)
        params.each {
            def p = addParamFromNamedProperty(resource.params, ParameterStyle.TEMPLATE, it, parameters)

            // workaround for bug in SoapUI 4.6.X
            if (p.style == ParameterStyle.TEMPLATE &&
                    resource.service.basePath.contains("{" + p.name + "}")) {
                resource.path = resource.path.replaceAll("\\{" + p.name + "\\}", "")
            }
        }

        res.value.each {

            def key = it.key
            def optional = key.endsWith('?')

            if (optional)
                key = key.substring(0, key.length() - 1)

            key = replaceParameters(key, parameters)

            if (Arrays.asList(RequestMethod.methodsAsStringArray).contains(key.toUpperCase())) {
                def method = resource.getRestMethodByName(key)
                if (method == null && !optional) {
                    method = resource.addNewMethod(key)
                    method.method = RequestMethod.valueOf(key.toUpperCase())
                }

                if (method != null)
                    initMethod(it, method, resourceTraits, parameters)
            }
        }

        if (res.value.type != null) {
            def type = res.value.type

            parameters = [:]
            parameters.resourcePath = resource.fullPath

            def ix = parameters.resourcePath.lastIndexOf('/')
            parameters.resourcePathName = ix == -1 ? parameters.resourcePath :
                parameters.resourcePath.substring(ix + 1)

            if (type instanceof String && resourceTypes[type] != null) {
                applyResourceType(resource, resourceTypes[type], resourceTraits, parameters)
            } else if (type instanceof Map) {
                type.each {
                    if (resourceTypes[it.key] != null) {
                        parameters.putAll(it.value)
                        applyResourceType(resource, resourceTypes[it.key], resourceTraits, parameters)
                    }
                }
            }
        }
    }

    def applyResourceType(RestResource resource, def type, List resourceTraits, Map parameters) {
        type.each {
            def key = it.key
            def optional = key.endsWith('?')

            if (optional)
                key = key.substring(0, key.length() - 1)

            key = replaceParameters(key, parameters)

            def traits = resourceTraits?.clone()
            if( traits != null && type.is != null )
                traits.add( type.is )

            if (key.startsWith('/')) {
                def name = getResourceName(it, parameters)
                def child = resource.getChildResourceByName(name)
                if (child == null && !optional) {
                    child = resource.addNewChildResource(name, key)
                }

                if (child != null)
                    initResource(child, it.value, traits, parameters)
            } else if (Arrays.asList(RequestMethod.methodsAsStringArray).contains(key.toUpperCase())) {
                def method = resource.getRestMethodByName(key)
                if (method == null && !optional) {
                    method = resource.addNewMethod(key)
                    method.method = RequestMethod.valueOf(key.toUpperCase())
                }

                if (method != null)
                    initMethod(it, method, traits, parameters)
            }
        }

        // derived from a base type?
        if (type.type != null) {
            def baseType = type.type

            if (baseType instanceof String && resourceTypes[baseType] != null) {
                applyResourceType(resource, resourceTypes[baseType], resourceTraits, parameters)
            } else if (baseType instanceof Map) {
                baseType.each {
                    if (resourceTypes[it.key] != null) {

                        it.value.each {
                            parameters.put( it.key, replaceParameters( it.value, parameters ))
                        }

                        applyResourceType(resource, resourceTypes[it.key], resourceTraits, parameters)
                    }
                }
            }
        }
    }

    public void initMethod(it, RestMethod method, resourceTraits, Map parameters) {

        if (it.value == "")
            return

        if (method.description != null)
            method.description = replaceParameters(it.value.description, parameters)

        addMethodParameters(method, it.value, parameters)

        if (it.value.body != null)
            addRequestBody(method, it.value.body, parameters)

        addResponses(method, it.value, parameters)
        applyTraits(it, method, resourceTraits)

        if (method.requestCount == 0)
            method.addNewRequest("Request 1")
    }

    def applyTraits(obj, RestMethod method, resourceTraits) {

        def parameters = [:]
        parameters.resourcePath = method.resource.fullPath

        def ix = parameters.resourcePath.lastIndexOf('/')
        parameters.resourcePathName = ix == -1 ? parameters.resourcePath :
            parameters.resourcePath.substring(ix + 1)

        parameters.methodName = method.name

        obj.value.is?.each {

            if (it instanceof String && traits[it] != null) {
                applyTrait( method, traits[it], parameters )
            } else if (it instanceof Map) {
                it.each {
                    if (traits[it.key] != null) {
                        Map p = it.value
                        p.putAll(parameters)
                        applyTrait( method, traits[it.key], p )
                    }
                }
            }
        }

        resourceTraits?.each {
            if (it instanceof String && traits[it] != null) {
                applyTrait( method, traits[it], parameters )
            } else if (it instanceof Map && traits[it.key] != null) {
                Map p = it.value
                p.putAll(parameters)
                applyTrait( method, traits[it.key], p )
            }
        }
    }

    private applyTrait( RestMethod method, def trait, Map parameters )
    {
        addMethodParameters(method, trait, parameters)

        if( trait.body != null )
            addRequestBody( method, trait.body, parameters )

        if( trait.responses != null )
            addResponses( method, trait, parameters )
    }

    private addMethodParameters(def method, def raml, Map parameters) {
        raml.queryParameters?.each {
            addParamFromNamedProperty(method.params, ParameterStyle.QUERY, it, parameters)
        }

        raml.headers?.each {
            addParamFromNamedProperty(method.params, ParameterStyle.QUERY, it, parameters)
        }
    }

    private addRequestBody(RestMethod method, def body, Map parameters) {
        body.each {
            def rep = method.addNewRepresentation(RestRepresentation.Type.REQUEST)
            rep.mediaType = replaceParameters(it.key, parameters)

            if (rep.mediaType.equals("application/x-www-form-urlencoded") ||
                    rep.mediaType.equals("multipart/form-data")) {
                it.value.formParameters?.each {
                    addParamFromNamedProperty(method.params, ParameterStyle.QUERY, it, parameters)
                }
            }

            if (it.value instanceof Map && it.value.example != null) {
                def request = method.addNewRequest("Sample Request")
                request.mediaType = it.key
                request.requestContent = it.value.example
            }
        }
    }

    private RestParameter addParamFromNamedProperty(def params, def style, def prop, Map parameters) {
        def key = prop.key
        def optional = key.endsWith('?')

        if (optional)
            key = key.substring(0, key.length() - 1)

        key = replaceParameters(key, parameters)

        RestParameter param = params.getProperty(key)
        if (param == null) {
            if (optional)
                return

            param = params.addProperty(key)
        }

        param.style = style
        try {

            if (prop.value instanceof String) {
                if (param.description == null)
                    param.description = replaceParameters(prop.value, parameters)
            } else if (prop.value instanceof Map) {
                if (prop.value.description != null && param.description == null)
                    param.description = replaceParameters(prop.value.description, parameters)

                if (prop.value.default != null && param.defaultValue?.trim().length() == 0)
                    param.defaultValue = replaceParameters(prop.value.default, parameters)

                if (prop.value.required != null)
                    param.required = Boolean.valueOf(replaceParameters(prop.value.required, parameters))

                if (prop.value.enum != null) {
                    param.options = prop.value.enum
                    param.options.eachWithIndex { String entry, int i ->
                        param.options[i] = replaceParameters(entry, parameters)
                    }
                }

                if (param.type == null) {
                    param.type = XmlString.type.name

                    switch (replaceParameters(prop.value.type, parameters)) {
                        case "number": param.type = XmlDouble.type.name; break;
                        case "integer": param.type = XmlInteger.type.name; break;
                        case "date": param.type = XmlDate.type.name; break;
                        case "boolean": param.type = XmlBoolean.type.name; break;
                    }
                }
            }
        }
        catch (e) {
            Console.println("Failed to init param " + prop + "; " + e)
        }

        return param
    }

    def replaceParameters(def str, Map parameters) {
        if (str == null || parameters == null || parameters.isEmpty())
            return str

        parameters.each {
            str = str.replaceAll("<<" + it.key + ">>", it.value)
        }

        return str
    }

    private addResponses(RestMethod method, def raml, Map parameters) {
        raml.responses?.each {

            def key = it.key
            def optional = key.endsWith('?')
            if (optional)
                key = key.substring(0, key.length() - 1)

            key = replaceParameters(key, parameters)

            // no body?
            if( it.value.body == null )
            {
                def representation = method.representations.find { r -> r.status.contains(Integer.parseInt(key)) }

                if (representation == null && !optional ) {
                    representation = method.addNewRepresentation(
                            Integer.parseInt(key) < 400 ? RestRepresentation.Type.RESPONSE : RestRepresentation.Type.FAULT)

                    representation.status = [key]
                }

                if ( representation != null && representation.description == null)
                    representation.description = replaceParameters(it.value.description, parameters)
            }
            else
            {
                // loop body definitions
                it.value.body?.each{ body ->
                    def representation = method.representations.find {
                        r -> r.status.contains(Integer.parseInt(key) && body.key.equalsIgnoreCase( r.mediaType )) }

                    if (representation == null && !optional ) {

                        representation = method.addNewRepresentation(
                                Integer.parseInt(key) < 400 ? RestRepresentation.Type.RESPONSE : RestRepresentation.Type.FAULT)

                        representation.status = [key]
                        representation.mediaType = body.key
                    }

                    if ( representation != null && representation.description == null)
                        representation.description = replaceParameters(it.value.description, parameters)
                }
            }
        }
    }

    private RestService createRestService(def raml) {
        RestService restService = project.addNewInterface(raml.title, RestServiceFactory.REST_TYPE)

        def path = raml.baseUri
        if (path != null) {
            if (path.endsWith("/"))
                path = path.substring(0, path.length() - 1)

            URL url = new URL(path)
            def pathPos = path.length() - url.path.length()

            restService.basePath = path.substring(pathPos)
            restService.addEndpoint(path.substring(0, pathPos))
        }

        return restService
    }

    private Map extractUriParams(def path, def uriParameters) {
        def uriParams = [:]

        RestUtils.extractTemplateParams(path).each {
            uriParams[it] = [:]
        }

        uriParameters?.each {
            uriParams[it.key] = it.value
        }

        uriParams.each {
            it.value.required = true
        }

        return uriParams
    }
}
