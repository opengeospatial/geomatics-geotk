package org.opengis.cite.geomatics;

/**
 * Named spatial relationship predicates based on the dimensionally extended
 * nine-intersection matrix (DE-9IM) and ISO 19143 (OGC 09-026r2). The
 * <em>Beyond</em> and <em>DWithin</em> predicates are derived from the analytic
 * method <code>Distance(Geometry g1, Geometry g2)</code>, which returns the
 * minimum distance between two geometries.
 * 
 * @see <a href="http://portal.opengeospatial.org/files/?artifact_id=25355"
 *      target="_blank">OpenGIS Implementation Specification for Geographic
 *      information -- Simple feature access -- Part 1: Common architecture</a>
 * @see <a href="http://docs.opengeospatial.org/is/09-026r2/09-026r2.html"
 *      target="_blank">OGC Filter Encoding 2.0 Encoding Standard, Version
 *      2.0.2</a>
 */
public enum SpatialOperator {
    BBOX, CONTAINS, CROSSES, DISJOINT, INTERSECTS, EQUALS, OVERLAPS, TOUCHES, WITHIN, BEYOND, DWITHIN;
}
