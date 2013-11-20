## soapui-raml-plugin

- Allows you to import RAML files into SoapUI for testing your REST APIs
- Allows you to browse the ApiHub directory for APIs with either RAML or Swagger definitions (Swagger requires the
  soapui-swagger-plugin to be installed also)

### Usage

Unzip this and the swagger plugin zips into the SoapUI/bin folder, this will place files in the underlying folders as follows:

```
/soapui
   /bin
      /ext
         yamlbeans-1.06.jar (from the raml plugin)
         swagger4j-1.0-beta2.jar  (from the swagger plugin)
         Javax.json.1.0-b06.jar  (from the swagger plugin)
      /plugins
         soapui-raml-plugin-0.1-plugin.jar  (from the raml plugin)
         Soapui-swagger-plugin-0.3-plugin.jar (from the swagger plugin)
```

(Re)Start SoapUI and create an empty project, you should have the following menu option on the project-popup menu:
- Import RAML Definition
- Add API from ApiHub

### Build it yourself

Clone the Git repository and run

mvn clean install assembly:single

to get the same zip as found on sourceforge

### Shortcomings

I've tested this with a number of RAML files, but I'm sure there are details I've missed - please let me know if you find
anything strange or unexpected.

### Feedback is welcome!

Comment here or get in touch on twitter; @olensmar

Thanks!