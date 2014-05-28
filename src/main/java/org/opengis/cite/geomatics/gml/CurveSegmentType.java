package org.opengis.cite.geomatics.gml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.geotoolkit.gml.xml.AbstractCurveSegment;
import org.geotoolkit.gml.xml.DirectPosition;
import org.geotoolkit.gml.xml.LineStringSegment;
import org.geotoolkit.gml.xml.v321.ArcStringType;
import org.geotoolkit.gml.xml.v321.DirectPositionType;
import org.geotoolkit.gml.xml.v321.GeodesicStringType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Curve segment type. A curve is composed of one or more contiguous curve
 * segments, each of which may be defined using a different interpolation
 * method.
 * 
 */
public enum CurveSegmentType {

    /** gml:LineStringSegment */
    LINE_STRING {
        @Override
        public List<Coordinate> getCoordinateList(AbstractCurveSegment segment,
                CoordinateReferenceSystem crs) {
            if (null == crs) {
                throw new IllegalArgumentException(
                        "No CRS supplied for curve segment.");
            }
            LineStringSegment lineSegment = LineStringSegment.class
                    .cast(segment);
            List<Double> posList = new ArrayList<Double>();
            if (null == lineSegment.getPosList()) {
                for (DirectPosition pos : lineSegment.getPos()) {
                    posList.addAll(pos.getValue());
                }
            } else {
                posList.addAll(lineSegment.getPosList().getValue());
            }
            List<Coordinate> coords = new ArrayList<Coordinate>();
            GmlUtils.extractCoordinatesFromPosList(posList, crs
                    .getCoordinateSystem().getDimension(), coords);
            return coords;
        }
    },
    /** gml:GeodesicString, gml:Geodesic */
    GEODESIC_STRING {
        @Override
        public List<Coordinate> getCoordinateList(AbstractCurveSegment segment,
                CoordinateReferenceSystem crs) {
            if (null == crs) {
                throw new IllegalArgumentException(
                        "No CRS supplied for curve segment.");
            }
            GeodesicStringType geodesic = GeodesicStringType.class
                    .cast(segment);
            List<Coordinate> coords = new ArrayList<Coordinate>();
            GmlUtils.extractCoordinatesFromPosList(geodesic.getPosList()
                    .getValue(), crs.getCoordinateSystem().getDimension(),
                    coords);
            return coords;
        }
    },
    /** gml:ArcByCenterPoint, gml:CircleByCenterPoint */
    ARC_BY_CENTER {
        @Override
        public List<Coordinate> getCoordinateList(AbstractCurveSegment segment,
                CoordinateReferenceSystem crs) {
            if (null == crs) {
                throw new IllegalArgumentException(
                        "No CRS supplied for curve segment.");
            }
            List<Coordinate> coords = new ArrayList<Coordinate>();
            GmlUtils.inferPointsOnArc(segment, crs, coords);
            return coords;
        }
    },
    /** gml:ArcString, gml:Arc, gml:Circle */
    ARC_STRING {
        @Override
        public List<Coordinate> getCoordinateList(AbstractCurveSegment segment,
                CoordinateReferenceSystem crs) {
            if (null == crs) {
                throw new IllegalArgumentException(
                        "No CRS supplied for curve segment.");
            }
            List<Coordinate> coords = new ArrayList<Coordinate>();
            ArcStringType arcType = ArcStringType.class.cast(segment);
            if (null != arcType.getPosList()) {
                GmlUtils.extractCoordinatesFromPosList(arcType.getPosList()
                        .getValue(), crs.getCoordinateSystem().getDimension(),
                        coords);
            } else { // at least 3 gml:pos elements
                List<JAXBElement<?>> points = arcType
                        .getPosOrPointPropertyOrPointRep();
                for (JAXBElement<?> elem : points) {
                    DirectPositionType pos = (DirectPositionType) elem
                            .getValue();
                    coords.add(new Coordinate(pos.getOrdinate(0), pos
                            .getOrdinate(1)));
                }
            }
            return coords;
        }
    };

    /**
     * Returns a list of points on a curve segment. Some points may be computed
     * if not given explicitly (e.g. ArcByCenterPoint).
     * 
     * @param segment
     *            A GML curve segment.
     * @param crs
     *            The coordinate reference system associated with the curve.
     * @return A list of JTS Coordinate objects (ordered from start to end).
     */
    public abstract List<Coordinate> getCoordinateList(
            AbstractCurveSegment segment, CoordinateReferenceSystem crs);
}
