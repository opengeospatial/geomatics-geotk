package org.opengis.cite.geomatics.gml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.AbstractRing;
import org.geotoolkit.gml.xml.AbstractRingProperty;
import org.geotoolkit.gml.xml.v321.AbstractRingType;
import org.geotoolkit.gml.xml.v321.AbstractSurfacePatchType;
import org.geotoolkit.gml.xml.v321.PolygonPatchType;
import org.geotoolkit.gml.xml.v321.PolygonType;
import org.geotoolkit.gml.xml.v321.RectangleType;
import org.geotoolkit.gml.xml.v321.SurfacePatchArrayPropertyType;
import org.geotoolkit.gml.xml.v321.SurfaceType;
import org.geotoolkit.gml.xml.v321.TriangleType;
import org.geotoolkit.xml.MarshallerPool;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Creates a sequence containing the coordinates comprising the exterior surface
 * boundary.
 */
public class SurfaceCoordinateListFactory implements CoordinateListFactory {

    private static final Unmarshaller GML_UNMARSHALLER = initGmlUnmarshaller();

    private static Unmarshaller initGmlUnmarshaller() {
        Unmarshaller unmarshaller = null;
        try {
            MarshallerPool pool = new MarshallerPool(
                    "org.geotoolkit.gml.xml.v321");
            unmarshaller = pool.acquireUnmarshaller();
        } catch (JAXBException je) {
            throw new RuntimeException(je);
        }
        return unmarshaller;
    }

    private static CurveCoordinateListFactory curveCoordFactory = new CurveCoordinateListFactory();
    private static Map<String, SurfacePatchType> patchTypeMap = loadPatchTypeMap();

    private static Map<String, SurfacePatchType> loadPatchTypeMap() {
        patchTypeMap = new HashMap<String, SurfacePatchType>();
        patchTypeMap.put(PolygonPatchType.class.getName(),
                SurfacePatchType.POLYGON);
        patchTypeMap.put(RectangleType.class.getName(),
                SurfacePatchType.RECTANGLE);
        patchTypeMap.put(TriangleType.class.getName(),
                SurfacePatchType.TRIANGLE);
        return patchTypeMap;
    }

    @Override
    public List<Coordinate> createCoordinateList(AbstractGeometry gmlGeom) {
        List<Coordinate> coordList = null;
        if (SurfaceType.class.isInstance(gmlGeom)) {
            SurfaceType surface = SurfaceType.class.cast(gmlGeom);
            coordList = exteriorBoundaryOfSurface(surface.getPatches()
                    .getValue(), surface.getSrsName());
        } else if (PolygonType.class.isInstance(gmlGeom)) {
            coordList = exteriorBoundaryOfPolygon(PolygonType.class
                    .cast(gmlGeom));
        } else {
            throw new RuntimeException("Unsupported surface type: "
                    + gmlGeom.getClass().getName());
        }
        return coordList;
    }

    /**
     * Creates a list of coordinates on the exterior boundary of a GML surface
     * geometry.
     * 
     * @param gmlSurface
     *            A gml:Surface or gml:Polygon element (including extension
     *            elements that can substitute for gml:Surface).
     * @return A list of 2D coordinates.
     */
    @SuppressWarnings("unchecked")
    public List<Coordinate> createCoordinateList(Element gmlSurface) {
        List<Coordinate> coordList = null;
        try {
            if (GmlUtils
                    .hasChildElement(gmlSurface, GmlUtils.GML_NS, "patches")) {
                Node patchesNode = gmlSurface.getElementsByTagNameNS(
                        GmlUtils.GML_NS, "patches").item(0);
                JAXBElement<SurfacePatchArrayPropertyType> patchArray;
                patchArray = (JAXBElement<SurfacePatchArrayPropertyType>) GML_UNMARSHALLER
                        .unmarshal(patchesNode);
                coordList = exteriorBoundaryOfSurface(patchArray.getValue(),
                        gmlSurface.getAttribute("srsName"));
            } else if (GmlUtils.hasChildElement(gmlSurface, GmlUtils.GML_NS,
                    "exterior")) {
                JAXBElement<PolygonType> polygon = (JAXBElement<PolygonType>) GML_UNMARSHALLER
                        .unmarshal(gmlSurface);
                coordList = exteriorBoundaryOfPolygon(polygon.getValue());
            } else {
                throw new RuntimeException("Unsupported surface type: "
                        + gmlSurface.getNodeName());
            }
        } catch (JAXBException je) {
            throw new RuntimeException(je);
        }
        return coordList;
    }

    /**
     * Returns a set containing sequences of points on the interior boundaries
     * of a surface geometry. Each list in the set corresponds to a distinct
     * interior boundary.
     * 
     * @param gmlGeom
     *            A GML surface geometry (substitutes for gml:AbstractSurface).
     * @return A set of Coordinate lists representing interior boundary curves;
     *         it may be empty.
     */
    public Set<List<Coordinate>> interiorCoordinatesSet(AbstractGeometry gmlGeom) {
        Set<List<Coordinate>> set = null;
        if (SurfaceType.class.isInstance(gmlGeom)) {
            SurfaceType surface = SurfaceType.class.cast(gmlGeom);
            set = interiorBoundariesOfSurface(surface.getPatches().getValue(),
                    surface.getSrsName());
        } else if (PolygonType.class.isInstance(gmlGeom)) {
            set = interiorBoundariesOfPolygon(PolygonType.class.cast(gmlGeom));
        } else {
            throw new RuntimeException("Unsupported surface type: "
                    + gmlGeom.getClass().getName());
        }
        return set;
    }

    /**
     * Returns a set containing sequences of points on the interior boundaries
     * of a surface geometry. Each list in the set corresponds to a distinct
     * interior boundary.
     * 
     * @param gmlSurface
     *            A GML surface geometry (or an element in its substitution
     *            group).
     * @return A set of Coordinate lists representing interior boundary curves;
     *         it may be empty.
     */
    @SuppressWarnings("unchecked")
    public Set<List<Coordinate>> interiorCoordinatesSet(Element gmlSurface) {
        Set<List<Coordinate>> set = null;
        try {
            if (GmlUtils
                    .hasChildElement(gmlSurface, GmlUtils.GML_NS, "patches")) {
                Node patchesNode = gmlSurface.getElementsByTagNameNS(
                        GmlUtils.GML_NS, "patches").item(0);
                JAXBElement<SurfacePatchArrayPropertyType> patchArray;
                patchArray = (JAXBElement<SurfacePatchArrayPropertyType>) GML_UNMARSHALLER
                        .unmarshal(patchesNode);
                set = interiorBoundariesOfSurface(patchArray.getValue(),
                        gmlSurface.getAttribute("srsName"));
            } else if (GmlUtils.hasChildElement(gmlSurface, GmlUtils.GML_NS,
                    "interior")) {
                JAXBElement<PolygonType> polygon = (JAXBElement<PolygonType>) GML_UNMARSHALLER
                        .unmarshal(gmlSurface);
                set = interiorBoundariesOfPolygon(polygon.getValue());
            } else {
                throw new RuntimeException("Unsupported surface type: "
                        + gmlSurface.getNodeName());
            }
        } catch (JAXBException je) {
            throw new RuntimeException(je);
        }
        return set;
    }

    /**
     * Returns a list of points on the exterior boundary of a Surface geometry.
     * The constituent patches are merged.
     * 
     * @param patchArray
     *            An array property containing a sequence of surface patches; it
     *            corresponds to the gml:patches element.
     * @param srsName
     *            A CRS identifier that applies to the surface patches.
     * @return A list of Coordinate objects representing a boundary curve
     *         (ring).
     */
    List<Coordinate> exteriorBoundaryOfSurface(
            SurfacePatchArrayPropertyType patchArray, String srsName) {
        List<JAXBElement<? extends AbstractSurfacePatchType>> patchList = patchArray
                .getAbstractSurfacePatch();
        Iterator<JAXBElement<? extends AbstractSurfacePatchType>> patchItr = patchList
                .iterator();
        GeometryFactory geomFactory = new GeometryFactory();
        Set<Geometry> geomSet = new HashSet<Geometry>();
        while (patchItr.hasNext()) {
            AbstractSurfacePatchType patch = patchItr.next().getValue();
            String className = patch.getClass().getName();
            SurfacePatchType patchType = patchTypeMap.get(className);
            if (null == patchType) {
                throw new RuntimeException("Unsupported surface patch type: "
                        + className);
            }
            AbstractRingType exterior = patchType.getExteriorBoundary(patch);
            // a ring is not a geometry type in GML but it is in ISO 19107
            exterior.setSrsName(srsName);
            List<Coordinate> extCoords = curveCoordFactory
                    .createCoordinateList(exterior);
            Polygon polygon = geomFactory.createPolygon(extCoords
                    .toArray(new Coordinate[0]));
            geomSet.add(polygon);
        }
        GeometryCollection geomColl = geomFactory
                .createGeometryCollection(geomSet.toArray(new Geometry[0]));
        Geometry surface = geomColl.union();
        List<Coordinate> coordList = new ArrayList<Coordinate>();
        coordList.addAll(Arrays.asList(surface.getCoordinates()));
        return coordList;
    }

    /**
     * Returns a list of points on the exterior boundary of a Polygon geometry.
     * 
     * @param gmlPolygon
     *            A gml:Polygon geometry instance.
     * @return A list of Coordinate objects representing a boundary curve.
     */
    List<Coordinate> exteriorBoundaryOfPolygon(PolygonType gmlPolygon) {
        AbstractRing exterior = gmlPolygon.getExterior().getAbstractRing();
        if (null == exterior.getSrsName()) {
            exterior.setSrsName(gmlPolygon.getSrsName());
        }
        return curveCoordFactory.createCoordinateList(exterior);
    }

    /**
     * Returns a set containing sequences of points on the interior boundaries
     * of a Polygon geometry.
     * 
     * @param gmlPolygon
     *            A gml:Polygon geometry instance.
     * @return A set of Coordinate lists representing interior boundary curves.
     */
    Set<List<Coordinate>> interiorBoundariesOfPolygon(PolygonType gmlPolygon) {
        Set<List<Coordinate>> set = new HashSet<List<Coordinate>>();
        Iterator<? extends AbstractRingProperty> ringItr = gmlPolygon
                .getInterior().iterator();
        while (ringItr.hasNext()) {
            AbstractRing interior = ringItr.next().getAbstractRing();
            if (null == interior.getSrsName()) {
                interior.setSrsName(gmlPolygon.getSrsName());
            }
            set.add(curveCoordFactory.createCoordinateList(interior));
        }
        return set;
    }

    /**
     * Returns a set containing sequences of points on the interior boundary
     * curves of a Surface geometry.
     * 
     * @param patchArray
     *            An array property containing a sequence of surface patches; it
     *            corresponds to the gml:patches element.
     * @param srsName
     *            A CRS identifier that applies to the surface patches.
     * @return A set of Coordinate lists representing interior boundary curves.
     */
    Set<List<Coordinate>> interiorBoundariesOfSurface(
            SurfacePatchArrayPropertyType patchArray, String srsName) {
        List<JAXBElement<? extends AbstractSurfacePatchType>> patches = patchArray
                .getAbstractSurfacePatch();
        Iterator<JAXBElement<? extends AbstractSurfacePatchType>> patchItr = patches
                .iterator();
        Set<List<Coordinate>> set = new HashSet<List<Coordinate>>();
        while (patchItr.hasNext()) {
            AbstractSurfacePatchType patch = patchItr.next().getValue();
            String className = patch.getClass().getName();
            SurfacePatchType patchType = patchTypeMap.get(className);
            if (null == patchType) {
                throw new RuntimeException("Unsupported surface patch type: "
                        + className);
            }
            Set<AbstractRingType> interiorRings = patchType
                    .getInteriorBoundaries(patch);
            for (AbstractRing interior : interiorRings) {
                if (null == interior.getSrsName()) {
                    interior.setSrsName(srsName);
                }
                set.add(curveCoordFactory.createCoordinateList(interior));
            }
        }
        return set;
    }
}
