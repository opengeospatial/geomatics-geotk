package org.opengis.cite.geomatics;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.transform.dom.DOMSource;

import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.AbstractGeometry;
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
	 *             If an attempted coordinate transformation operation fails.
	 */
	public static boolean intersects(Node node1, Node node2)
			throws TransformException {
		AbstractGeometry gmlGeom1 = unmarshal(node1);
		AbstractGeometry gmlGeom2 = unmarshal(node2);
		Geometry jtsGeom1;
		Geometry jtsGeom2;
		try {
			jtsGeom1 = GeometrytoJTS.toJTS(gmlGeom1);
			jtsGeom2 = GeometrytoJTS.toJTS(gmlGeom2);
			// add CRS as user data to JTS geometry
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
}
