# Release Notes

## 1.15 (2020-10-16)

* [#3](https://github.com/opengeospatial/geomatics-geotk/issues/3): Add support for GML geometry types - Curve and Surface
* [#2](https://github.com/opengeospatial/geomatics-geotk/pull/2): Added new method transformRingToRightHandedCSKeepAllCoords.
* [#5](https://github.com/opengeospatial/geomatics-geotk/pull/5): Bump junit from 4.12 to 4.13.1
* GeodesyUtils::removeConsecutiveDuplicates: Change data type of tolerancePPM parameter to double

## 1.14 (2016-11-16)
This maintenance release includes the following changes:

* Extents::envelopeAsGML: Format decimal values using the root locale (Locale.ROOT)
* GmlUtils::findCRSReference: Check child gml:pos, gml:posList elements for a CRS reference (@srsName)


## 1.13 (2016-09-19)
This release includes the following changes:

* Add support for spatial predicates: DWithin, Beyond (based on orthodromic distance)
* In `Extents::calculateEnvelope`, explicitly set srsName on all members of a geometry collection
* In Extents, add utility method to calculate antipode of coordinate tuple


## 1.12 (2016-08-02)
This release includes the following enhancements:

* Support all named spatial relationship predicates based on the DE-9IM
* Support all named temporal relationship predicates from ISO 19108 (TemporalUtils)
* Add GmlUtils::gmlToTemporalGeometricPrimitive


## 1.11 (2016-05-03)
This maintenance release includes the following changes:

* Allow CRS identifiers based on 'http' URI (Extents)


## 1.10 (2015-10-23)
This maintenance release includes the following changes:

* Fix [issue #1](https://github.com/opengeospatial/geomatics-geotk/issues/1): 
do not remove final coordinate (sequence may form a closed curve).
* Fix malformed Javadoc comments (JDK 8 doclint).


## 1.9 (2015-07-07)
This maintenance release includes the following fixes:

* Fix NoSuchAuthorityCodeException when srsName is an 'http' URI (not 
recognized by Geotk v3).
* Update Maven plugins.

## 1.8 (2015-03-18)
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
