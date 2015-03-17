# Release Notes

## 1.8 (2015-03-17)
This maintenance release includes the following changes:

* Add SpatialAssert class to define custom assertions
* Add `Extents.createEnvelope` method
* Add `Extents.coalesceBoundingBoxes` method
* Add `Extents.envelopeToString` method (KVP syntax)
* Amend `GeodesyUtils.getCRSIdentifier` to recognize CRS84 (WGS 84 with lon,lat 
axis order; see ISO 19128, B.3)

## 1.7 (2014-05-28)
The project is now hosted at GitHub. This release introduces new site content, 
but the essential functionality of the library is unchanged.

* Modify POM for GitHub.
* Add site content.
* Change license to Apache License, Version 2.0.

## 1.6 (2014-04-15)
This minor release includes the following fixes and updates:

* Attempt to dereference remote curve member in GML ring.
* Fix: Use List implementation that supports ListIterator.remove() in 
SurfaceCoordinateListFactory.
* Fix: Do not use JTS LinearRing to obtain coordinates on GML ring geometry 
(limited support for GML curve segments).
* Fix Javadoc errors reported when building with JDK 8.
* Add unit tests.

## 1.5 (2014-02-06)
This release includes the following changes:

* Find interior boundary for DOM representation of gml:Surface element (or 
extensions thereof).
* Added utility method transformRingToRightHandedCS.
* Merge constituent patches to determine exterior boundary of surface 
(SurfaceCoordinateListFactory).
* Generate list of points on interior boundaries of surface.
* Create list of coordinates delimiting external boundary of surface.
* Fix: Add namespace binding to gml:Envelope (in Extents).
