package org.opengis.cite.geomatics.gml;

import java.util.List;

import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.v321.AbstractCurveType;
import org.geotoolkit.gml.xml.v321.AbstractSurfaceType;
import org.geotoolkit.gml.xml.v321.PointType;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Constructs an array of points from a GML geometry instance. Such a sequence
 * can be used to calculate a convex hull that approximates the spatial extent
 * of the geometry.
 * 
 */
public class GeometryCoordinateList {

    /**
     * Creates an array of 2D coordinates from the given GML geometry instance.
     * The collection may contain duplicates (arising from curve segments and
     * rings).
     * 
     * @param gmlGeometry
     *            A GML geometry element.
     * @return A JTS Coordinate[] array containing a sequence of 2D coordinate
     *         tuples.
     */
    public Coordinate[] getCoordinateList(AbstractGeometry gmlGeometry) {
        GeometryType geomType;
        if (AbstractCurveType.class.isInstance(gmlGeometry)) {
            geomType = GeometryType.CURVE;
        } else if (PointType.class.isInstance(gmlGeometry)) {
            geomType = GeometryType.POINT;
        } else if (AbstractSurfaceType.class.isInstance(gmlGeometry)) {
            geomType = GeometryType.SURFACE;
        } else {
            throw new RuntimeException("Unsupported geometry type: "
                    + gmlGeometry.getClass().getName());
        }
        CoordinateListFactory factory = geomType.getCoordinateSetFactory();
        List<Coordinate> coordSet = factory.createCoordinateList(gmlGeometry);
        return coordSet.toArray(new Coordinate[0]);
    }
}
