**Geomatics Utilities - Geotk**
  
The `geomatics-geotk` library is used by several [OGC](http://www.opengeospatial.org/) 
conformance test suites. It provides support for processing spatial data and associated 
metadata using various [Geotk modules](http://www.geotoolkit.org/). The Geotk 3.x 
library implements the [GeoAPI v3](http://www.geoapi.org/) interfaces.

Visit the [project documentation website](http://opengeospatial.github.io/geomatics-geotk/) 
for more information, including the API documentation.

**Note**

Apache Maven is required to build the project. Some dependencies are currently 
not available in the Central repository. The POM includes the following remote 
repository entry:

    <repositories>
      <repository>
        <id>geotoolkit</id>
        <name>Geotk Modules</name>
        <url>http://maven.geotoolkit.org/</url>
        <layout>default</layout>
      </repository>
    </repositories>
