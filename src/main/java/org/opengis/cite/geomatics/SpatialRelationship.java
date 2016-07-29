package org.opengis.cite.geomatics;

/**
 * Named spatial relationship predicates based on the dimensionally extended
 * nine-intersection matrix (DE-9IM).
 * 
 * @see <a href="http://portal.opengeospatial.org/files/?artifact_id=25355"
 *      target="_blank">OpenGIS Implementation Specification for Geographic
 *      information -- Simple feature access -- Part 1: Common architecture</a>
 */
public enum SpatialRelationship {
	CONTAINS, CROSSES, DISJOINT, INTERSECTS, EQUALS, OVERLAPS, TOUCHES, WITHIN;
}
