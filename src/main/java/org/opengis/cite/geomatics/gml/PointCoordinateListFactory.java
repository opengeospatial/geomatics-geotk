/**
 *
 */
package org.opengis.cite.geomatics.gml;

import java.util.ArrayList;
import java.util.List;

import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.Point;

import org.locationtech.jts.geom.Coordinate;

/**
 * Creates a sequence containing a single coordinate tuple denoting the position of a
 * geometric point.
 */
public class PointCoordinateListFactory implements CoordinateListFactory {

	@Override
	public List<Coordinate> createCoordinateList(AbstractGeometry gmlPoint) {
		List<Coordinate> coordSet = new ArrayList<Coordinate>();
		Point point = Point.class.cast(gmlPoint);
		double[] pos = point.getPos().getCoordinate();
		coordSet.add(new Coordinate(pos[0], pos[1]));
		return coordSet;
	}

}
