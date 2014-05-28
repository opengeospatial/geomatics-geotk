package org.opengis.cite.geomatics;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.xml.MarshallerPool;
import org.opengis.cite.geomatics.gml.GmlUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.w3c.dom.Node;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Provides methods to test for the existence of a specified topological spatial
 * relationship between two geometric objects.
 * 
 * @see <a
 *      href="http://portal.opengeospatial.org/files/?artifact_id=25355">OpenGIS
 *      Implementation Specification for Geographic information - Simple feature
 *      access - Part 1: Common architecture</a>
 * 
 */
public class TopologicalRelationships {

    private static final Logger LOGR = Logger
            .getLogger(TopologicalRelationships.class.getPackage().getName());

    /**
     * Determines whether or not two GML geometry representations spatially
     * intersect. The result indicates whether or not the two geometries have at
     * least one point in common; more specifically:
     * 
     * <pre>
     * a.Intersects(b) &lt;==&gt; ! a.Disjoint(b)
     * </pre>
     * 
     * If the geometry representations have different CRS references, a
     * coordinate transformation operation is attempted.
     * 
     * @param node1
     *            An Element node representing a GML geometry object.
     * @param node2
     *            An Element node representing another GML geometry object.
     * @return {@code true} if the geometries are not disjoint; {@code false}
     *         otherwise.
     * @throws TransformException
     *             If a coordinate transformation operation fails.
     */
    @SuppressWarnings("unchecked")
    public static boolean intersects(Node node1, Node node2)
            throws TransformException {
        if (!node1.getNamespaceURI().equals(GmlUtils.GML_NS)
                || !node2.getNamespaceURI().equals(GmlUtils.GML_NS)) {
            throw new IllegalArgumentException(
                    "Supplied nodes must be in the GML 3.2 namespace "
                            + GmlUtils.GML_NS);
        }
        GmlUtils.setSrsNameOnCollectionMembers(node1, node2);
        // unmarshal GML geometry representations
        AbstractGeometry gmlGeom1;
        AbstractGeometry gmlGeom2;
        try {
            MarshallerPool pool = new MarshallerPool(
                    "org.geotoolkit.gml.xml.v321");
            Unmarshaller unmarshaller = pool.acquireUnmarshaller();
            JAXBElement<AbstractGeometry> result = (JAXBElement<AbstractGeometry>) unmarshaller
                    .unmarshal(node1);
            gmlGeom1 = result.getValue();
            result = (JAXBElement<AbstractGeometry>) unmarshaller
                    .unmarshal(node2);
            gmlGeom2 = result.getValue();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        Geometry jtsGeom1;
        Geometry jtsGeom2;
        try {
            jtsGeom1 = GeometrytoJTS.toJTS(gmlGeom1);
            jtsGeom2 = GeometrytoJTS.toJTS(gmlGeom2);
            // CRS added as user data to JTS geometry
            CoordinateReferenceSystem crs1 = JTS
                    .findCoordinateReferenceSystem(jtsGeom1);
            CoordinateReferenceSystem crs2 = JTS
                    .findCoordinateReferenceSystem(jtsGeom2);
            if (!crs1.getName().equals(crs2.getName())) {
                if (LOGR.isLoggable(Level.FINE)) {
                    LOGR.fine(String
                            .format("Attempting coordinate transformation from CRS %s to %s",
                                    crs1.getName(), crs2.getName()));
                }
                MathTransform transform = CRS.findMathTransform(crs1, crs2);
                jtsGeom1 = JTS.transform(jtsGeom1, transform);
                JTS.setCRS(jtsGeom1, crs2);
            }
        } catch (FactoryException fe) {
            throw new RuntimeException(fe);
        }
        if (LOGR.isLoggable(Level.FINE)) {
            LOGR.fine(String.format("JTS geometry objects:\n  %s\n  %s",
                    jtsGeom1.toText(), jtsGeom2.toText()));
        }
        return jtsGeom1.intersects(jtsGeom2);
    }
}
