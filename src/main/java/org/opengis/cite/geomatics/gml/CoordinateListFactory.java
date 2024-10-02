package org.opengis.cite.geomatics.gml;

import java.util.List;
import org.geotoolkit.gml.xml.AbstractGeometry;

import org.locationtech.jts.geom.Coordinate;

/**
 * This interface defines a factory for generating a sequence of points from a geometry
 * object. An implementation of this interface should handle a particular geometry type
 * (point, curve, surface).
 *
 */
public interface CoordinateListFactory {

	/**
	 * Creates a list of coordinates from a GML geometry representation. The sequence
	 * typically includes the constituent vertices.
	 * @param gml A GML geometry object (constructed from its XML representation).
	 * @return A list of 2D coordinates that constitute the geometry.
	 */
	public List<Coordinate> createCoordinateList(AbstractGeometry gml);

}
