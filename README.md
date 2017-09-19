# README #

RODA OpenCMIS Server, or RODA-OCS, for short, is an OpenCMIS server prototype developed for the MSc thesis "Strategies 
for Digital Preservation of Information" from Escola Superior de Tecnologias e Gestão de Leiria, Portugal. This 
implementation uses the [Apache Chemistry OpenCMIS](https://chemistry.apache.org/java/opencmis.html), a collection of 
Java libraries, frameworks and tools around the [CMIS 1.0](https://docs.oasis-open.org/cmis/CMIS/v1.0/cmis-spec-v1.0.html) 
and [CMIS 1.1](https://docs.oasis-open.org/cmis/CMIS/v1.1/CMIS-v1.1.html) specifications to implement an OpenCMIS server. 

The prototype allows a [RODA repository](https://demo.roda-community.org/) to expose contents stored inside it publicly, 
under a controlled environment, in an authenticated and secure way. This goal is achieved by integrating our prototype 
with RODA’s native permissions system. RODA-OCS not only implements a file browser mechanism for content navigation but 
also a query engine capable of searching and retrieving contents based on their metadata, either technical or descriptive. 

### Set up and Run this project ###

This project is the standalone version of the RODA-OCS prototype. The prototype's code is intended to coexist with other 
modules inside RODA's code. However, and since this prototype is a standard web application it can be run in any 
application server i.e.[Apache Tomcat](http://tomcat.apache.org/).

##### Requisites #####

* A RODA instance.
* [Apache Tomcat](http://tomcat.apache.org/) server or equivalent.
* (Recommended) [CMIS Workbench](https://chemistry.apache.org/java/developing/tools/dev-tools-workbench.html) client 
application to test the server.

##### Dependencies #####

This project is a [maven project](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html). 
Any IDE supporting maven projects should automatically recognise and download the dependencies needed to run the 
application.

##### Log4J Logs Folder Configuration #####

1. Open the file ```/src/main/resources/log4j.properties```.
2. Edit the variable ```log``` and point it to an existing directory, i.e. ```log = /path/to/logs_directory```. 
This is the folder where the application's logs will be written to.


##### Running the project in IntelliJ #####

1. Open the project in [IntelliJ](https://www.jetbrains.com/idea/).
2. Edit the configuration and add a new ```Tomcat Server > Local``` from the available new configurations templates. 
3. In the ```Run/Debug Configurations``` window and give a name to the configuration (recommended). In the ```Server``` 
tab, choose a Tomcat server available from the ```Application Server``` list.
4. In the ```Deployment``` tab, add a new ```Artifact``` by clicking the plus icon at the bottom of the list. Choose 
```roda-ocs-prototype:war exploded```. Click the ```Apply``` or ```OK``` button at the bottom of the window.
5. Run the configuration by clicking the ```play``` icon in the upper right corner of the IDE window. Confirm the correct 
 server bootstrap in the server output window. If the server bootstrap was successful a new browser window should open 
 and show the server welcome page.

## Author

**André Rosa**

* <https://bitbucket.org/candrelsrosa>
* <https://github.com/andreros/>
* <https://facebook.com/candrelsrosa>
* <https://twitter.com/candrelsrosa>


## License

Code released under [the GPL 3.0 license](https://opensource.org/licenses/GPL-3.0). Docs released under [Creative Commons](https://creativecommons.org/licenses/by/4.0/legalcode).