package org.opengis.cite.geomatics.gml;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.xml.bind.JAXBElement;
import org.geotoolkit.gml.xml.AbstractCurveSegment;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.Curve;
import org.geotoolkit.gml.xml.DirectPosition;
import org.geotoolkit.gml.xml.LineString;
import org.geotoolkit.gml.xml.v321.AbstractCurveType;
import org.geotoolkit.gml.xml.v321.AbstractRingType;
import org.geotoolkit.gml.xml.v321.ArcByCenterPointType;
import org.geotoolkit.gml.xml.v321.ArcStringType;
import org.geotoolkit.gml.xml.v321.ArcType;
import org.geotoolkit.gml.xml.v321.CircleByCenterPointType;
import org.geotoolkit.gml.xml.v321.CircleType;
import org.geotoolkit.gml.xml.v321.CompositeCurveType;
import org.geotoolkit.gml.xml.v321.CurvePropertyType;
import org.geotoolkit.gml.xml.v321.DirectPositionType;
import org.geotoolkit.gml.xml.v321.GeodesicStringType;
import org.geotoolkit.gml.xml.v321.LineStringSegmentType;
import org.geotoolkit.gml.xml.v321.LinearRingType;
import org.geotoolkit.gml.xml.v321.OrientableCurveType;
import org.geotoolkit.gml.xml.v321.RingType;

import org.locationtech.jts.geom.Coordinate;

/**
 * Creates a sequence containing the coordinates of a curve. The list will
 * generally include the constituent vertices or control points that lie on the
 * curve.
 */
public class CurveCoordinateListFactory implements CoordinateListFactory {

    /**
     * Indicates that an orientable curve has a negative orientation with
     * respect to its base curve.
     */
    public static final String ORIENT_NEG = "-";
    private static final Logger LOGR = Logger
            .getLogger(CurveCoordinateListFactory.class.getPackage().getName());
    public static Map<String, CurveSegmentType> segmentTypeMap = loadSegmentTypeMap();

    private static Map<String, CurveSegmentType> loadSegmentTypeMap() {
        segmentTypeMap = new HashMap<String, CurveSegmentType>();
        segmentTypeMap.put(LineStringSegmentType.class.getName(),
                CurveSegmentType.LINE_STRING);
        segmentTypeMap.put(GeodesicStringType.class.getName(),
                CurveSegmentType.GEODESIC_STRING);
        segmentTypeMap.put(ArcByCenterPointType.class.getName(),
                CurveSegmentType.ARC_BY_CENTER);
        segmentTypeMap.put(CircleByCenterPointType.class.getName(),
                CurveSegmentType.ARC_BY_CENTER);
        segmentTypeMap.put(ArcStringType.class.getName(),
                CurveSegmentType.ARC_STRING);
        segmentTypeMap
                .put(ArcType.class.getName(), CurveSegmentType.ARC_STRING);
        segmentTypeMap.put(CircleType.class.getName(),
                CurveSegmentType.ARC_STRING);
        return segmentTypeMap;
    }

    @Override
    public List<Coordinate> createCoordinateList(AbstractGeometry gmlGeometry) {
        List<Coordinate> coordList = null;
        if (Curve.class.isInstance(gmlGeometry)) {
            coordList = getCoordinateList(Curve.class.cast(gmlGeometry));
        } else if (LineString.class.isInstance(gmlGeometry)) {
            coordList = getCoordinateList(LineString.class.cast(gmlGeometry));
        } else if (CompositeCurveType.class.isInstance(gmlGeometry)) {
            coordList = getCoordinateList(CompositeCurveType.class
                    .cast(gmlGeometry));
        } else if (OrientableCurveType.class.isInstance(gmlGeometry)) {
            coordList = getCoordinateList(OrientableCurveType.class
                    .cast(gmlGeometry));
        } else if (AbstractRingType.class.isInstance(gmlGeometry)) {
            try {
                coordList = getCoordinateList(AbstractRingType.class
                        .cast(gmlGeometry));
            } catch (Exception x) {
                throw new RuntimeException("In GML ring: " + x.getMessage());
            }
        } else {
            throw new RuntimeException("Unsupported curve type: "
                    + gmlGeometry.getClass().getName());
        }
        return coordList;
    }

    /**
     * Returns a list of points on a curve consisting of one or more segments.
     *
     * @param curve
     *            A gml:Curve geometry instance.
     * @return A list of coordinates on the curve.
     */
    @SuppressWarnings("unchecked")
    List<Coordinate> getCoordinateList(Curve curve) {
        List<Coordinate> coordList = new ArrayList<Coordinate>();
        List<AbstractCurveSegment> segments = (List<AbstractCurveSegment>) curve
                .getSegments().getAbstractCurveSegment();
        for (AbstractCurveSegment segment : segments) {
            String className = segment.getClass().getName();
            CurveSegmentType segmentType = segmentTypeMap.get(className);
            if (null == segmentType) {
                throw new RuntimeException("Unsupported curve segment type: "
                        + className);
            }
            coordList.addAll(segmentType.getCoordinateList(segment,
                    curve.getCoordinateReferenceSystem(false)));
        }
        return coordList;
    }

    /**
     * Returns a list of points representing the vertices of a LineString
     * geometry.
     *
     * @param lineString
     *            A gml:LineString geometry instance.
     * @return The list of vertices.
     */
    List<Coordinate> getCoordinateList(LineString lineString) {
        List<Coordinate> coords = new ArrayList<Coordinate>();
        if (null != lineString.getPosList()) {
            GmlUtils.extractCoordinatesFromPosList(lineString.getPosList()
                    .getValue(), lineString.getCoordinateDimension(), coords);
        } else { // sequence of two or more direct positions
            for (DirectPosition pos : lineString.getPos()) {
                coords.add(new Coordinate(pos.getOrdinate(0), pos
                        .getOrdinate(1)));
            }
        }
        return coords;
    }

    /**
     * Returns a list of points on a composite curve consisting of one or more
     * curve members.
     *
     * @param compCurve
     *            A gml:CompositeCurve geometry instance.
     * @return A list of points on the entire curve; the interpolation method
     *         may vary along its length.
     */
    List<Coordinate> getCoordinateList(CompositeCurveType compCurve) {
        List<Coordinate> coords = new ArrayList<Coordinate>();
        for (CurvePropertyType member : compCurve.getCurveMember()) {
            // WARNING: Assume in-line curve and ignore href
            AbstractCurveType curveType = member.getAbstractCurve();
            if (null == curveType.getSrsName()) {
                curveType.setSrsName(compCurve.getSrsName());
            }
            coords.addAll(createCoordinateList(curveType));
        }
        return coords;
    }

    /**
     * Returns a list of points on an orientable curve. If the orientation is
     * positive ("+"), then the orientable curve is identical to the base curve;
     * otherwise the base curve is traversed in reverse order.
     *
     * @param orientableCurve
     *            A gml:OrientableCurve geometry instance.
     * @return A list of points on the (base) curve; the order reflects the
     *         orientation with respect to the base curve.
     */
    List<Coordinate> getCoordinateList(OrientableCurveType orientableCurve) {
        CurvePropertyType baseCurve = orientableCurve.getBaseCurve();
        // WARNING: Assume base curve is in-line and ignore href
        List<Coordinate> coords = createCoordinateList(baseCurve
                .getAbstractCurve());
        if (orientableCurve.getOrientation().equals(ORIENT_NEG)) {
            Collections.reverse(coords);
        }
        return coords;
    }

    /**
     * Returns a list of points on a ring representing a single connected
     * component of a surface boundary. A ring is structurally similar to a
     * composite curve.
     *
     * @param ring
     *            A ring (gml:Ring or gml:LinearRing element).
     * @return A list of points on a ring (closed curve).
     * @throws Exception
     *             If a curve member could not be accessed or parsed.
     */
    List<Coordinate> getCoordinateList(AbstractRingType ring) throws Exception {
        List<Coordinate> coords = new ArrayList<Coordinate>();
        if (LinearRingType.class.isInstance(ring)) {
            LinearRingType linearRing = LinearRingType.class.cast(ring);
            if (null != linearRing.getPosList()) {
                GmlUtils.extractCoordinatesFromPosList(linearRing.getPosList()
                        .getValue(), linearRing.getCoordinateDimension(),
                        coords);
            } else { // sequence of 4 or more direct positions
                List<JAXBElement<?>> points = linearRing
                        .getPosOrPointPropertyOrPointRep();
                for (JAXBElement<?> elem : points) {
                    DirectPositionType pos = (DirectPositionType) elem
                            .getValue();
                    coords.add(new Coordinate(pos.getOrdinate(0), pos
                            .getOrdinate(1)));
                }
            }
        } else { // gml:Ring
            RingType gmlRing = RingType.class.cast(ring);
            for (CurvePropertyType member : gmlRing.getCurveMember()) {
                AbstractCurveType curveType = null;
                if (null != member.getHref()) {
                    URI memberRef = URI.create(member.getHref());
                    AbstractGeometry geom = GmlUtils
                            .unmarshalGMLGeometry(memberRef);
                    if (AbstractCurveType.class.isInstance(geom)) {
                        curveType = AbstractCurveType.class.cast(geom);
                    }
                } else { // in-line curve
                    curveType = member.getAbstractCurve();
                }
                if (LOGR.isLoggable(Level.FINE)) {
                    LOGR.fine("Processing ring member: " + curveType.getId());
                }
                if (null == curveType.getSrsName()) {
                    curveType.setSrsName(ring.getSrsName());
                }
                coords.addAll(createCoordinateList(curveType));
            }
        }
        return coords;
    }

}
