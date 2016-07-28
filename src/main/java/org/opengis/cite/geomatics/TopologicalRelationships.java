package org.opengis.cite.geomatics;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.transform.dom.DOMSource;

import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.Curve;
import org.geotoolkit.referencing.CRS;
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
	 *             If any coordinate operation (conversion or transformation)
	 *             fails.
	 */
	public static boolean intersects(Node node1, Node node2)
			throws TransformException {
		Geometry g1 = toJTSGeometry(unmarshal(node1));
		Geometry g2 = toJTSGeometry(unmarshal(node2));
		try {
			g1 = setCRS(g1, JTS.findCoordinateReferenceSystem(g2));
		} catch (FactoryException fe) {
			throw new RuntimeException(fe);
		}
		return g1.intersects(g2);
	}

	/**
	 * 
	 * Builds a JTS geometry object from a GML geometry object.
	 * 
	 * @param gmlGeom
	 *            A GML geometry.
	 * @return A JTS geometry, or null if one could not be constructed.
	 */
	public static Geometry toJTSGeometry(AbstractGeometry gmlGeom) {
		Geometry jtsGeom = null;
		try {
			jtsGeom = GeometrytoJTS.toJTS(gmlGeom);
		} catch (FactoryException e1) {
			throw new RuntimeException(e1);
		} catch (IllegalArgumentException e2) {
			// Unsupported in Geotk v3
			if (Curve.class.isInstance(gmlGeom)) {
				jtsGeom = GmlUtils.buildLineString(Curve.class.cast(gmlGeom));
			}
		}
		LOGR.fine(String.format("Resulting JTS geometry:\n  %s",
				jtsGeom.toText()));
		return jtsGeom;
	}

	/**
	 * Determines whether or not two GML geometry representations are disjoint.
	 * 
	 * @param g1
	 *            An Element node representing a GML geometry object.
	 * @param g2
	 *            An Element node representing another GML geometry object.
	 * @return true if the geometries are disjoint; false otherwise.
	 * @throws TransformException
	 *             If an attempted coordinate operation fails.
	 */
	public static boolean disjoint(Node g1, Node g2) throws TransformException {
		return !intersects(g1, g2);
	}

	public static boolean touches(Node g1, Node g2) {
		throw new UnsupportedOperationException();
	}

	public static boolean within(Node g1, Node g2) {
		throw new UnsupportedOperationException();
	}

	public static boolean overlaps(Node g1, Node g2) {
		throw new UnsupportedOperationException();
	}

	public static boolean crosses(Node g1, Node g2) {
		throw new UnsupportedOperationException();
	}

	public static boolean contains(Node g1, Node g2) {
		throw new UnsupportedOperationException();
	}

	public static boolean beyond(Node g1, Node g2, Node distance) {
		throw new UnsupportedOperationException();
	}

	public static boolean withinDistance(Node g1, Node g2, Node distance) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates a GML geometry object from a DOM node.
	 * 
	 * @param geomNode
	 *            A node representing a GML geometry instance.
	 * @return A geometry object.
	 */
	static AbstractGeometry unmarshal(Node geomNode) {
		if (!geomNode.getNamespaceURI().equals(GmlUtils.GML_NS)) {
			throw new IllegalArgumentException(String.format(
					"Node not in GML namespace: ", geomNode.getNamespaceURI()));
		}
		GmlUtils.setSrsNameOnCollectionMembers(geomNode);
		AbstractGeometry gmlGeom;
		try {
			gmlGeom = GmlUtils.unmarshalGMLGeometry(new DOMSource(geomNode));
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		// NoSuchAuthorityCodeException if srsName is 'http' URI (Geotk v3)
		gmlGeom.setSrsName(GeodesyUtils.convertSRSNameToURN(gmlGeom
				.getSrsName()));
		return gmlGeom;
	}

	/**
	 * Checks that the given geometry object uses the specified CRS. If this is
	 * not the case, an attempt is made to change it.
	 * 
	 * @param g1
	 *            A JTS geometry object.
	 * @param crs
	 *            The target CRS.
	 * @return A Geometry object that uses the indicated CRS (the original
	 *         geometry if its CRS was unchanged).
	 * @throws FactoryException
	 *             If a CRS cannot be identified (e.g. a missing or invalid
	 *             reference).
	 * @throws TransformException
	 *             If any coordinate operation (conversion or transformation)
	 *             fails.
	 */
	static Geometry setCRS(Geometry g1, CoordinateReferenceSystem crs)
			throws FactoryException, TransformException {
		CoordinateReferenceSystem crs1 = JTS.findCoordinateReferenceSystem(g1);
		Geometry g2 = null;
		if (!crs1.getName().equals(crs.getName())) {
			LOGR.fine(String.format("Attempting to change CRS %s to %s",
					crs1.getName(), crs.getName()));
			MathTransform transform = CRS.findMathTransform(crs1, crs);
			g2 = JTS.transform(g1, transform);
			JTS.setCRS(g2, crs);
		}
		return (null != g2) ? g2 : g1;
	}
}
