package org.opengis.cite.geomatics;

import java.util.logging.Logger;

import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.distance.DistanceOp;

/**
 * Provides methods to test for the existence of a specified spatial
 * relationship between two geometric objects.
 * 
 * @see <a target="_blank"
 *      href="http://portal.opengeospatial.org/files/?artifact_id=25355">OpenGIS
 *      Implementation Specification for Geographic information - Simple feature
 *      access - Part 1: Common architecture</a>
 * 
 */
public class TopologicalRelationships {

	private static final Logger LOGR = Logger
			.getLogger(TopologicalRelationships.class.getPackage().getName());

	/**
	 * Determines whether or not two GML geometry representations are spatially
	 * related in some manner (e.g. g1 contains g2). If the geometry
	 * representations have different CRS references, an attempt will be made to
	 * change coordinates from one CRS to another through the application of a
	 * coordinate operation (conversion or transformation).
	 *
	 * @param predicate
	 *            A spatial relationship (predicate).
	 * @param node1
	 *            An Element node representing a GML geometry object.
	 * @param node2
	 *            An Element node representing another GML geometry object.
	 * @return true if the geometries satisfy the given spatial relationship ;
	 *         false otherwise.
	 */
	public static boolean isSpatiallyRelated(SpatialRelationship predicate,
			Node node1, Node node2) {
		Geometry g1 = toJTSGeometry(unmarshal(node1));
		Geometry g2 = toJTSGeometry(unmarshal(node2));
		try {
			g1 = setCRS(g1, JTS.findCoordinateReferenceSystem(g2));
		} catch (FactoryException | TransformException e) {
			throw new RuntimeException(e);
		}
		boolean isRelated = false;
		switch (predicate) {
		case INTERSECTS:
			isRelated = g1.intersects(g2);
			break;
		case DISJOINT:
			isRelated = !g1.intersects(g2);
			break;
		case TOUCHES:
			isRelated = g1.touches(g2);
			break;
		case WITHIN:
			isRelated = g1.within(g2);
			break;
		case OVERLAPS:
			isRelated = g1.overlaps(g2);
			break;
		case CROSSES:
			isRelated = g1.crosses(g2);
			break;
		case CONTAINS:
			isRelated = g1.contains(g2);
			break;
		case EQUALS:
			isRelated = g1.equalsTopo(g2);
			break;
		default:
			throw new IllegalArgumentException(
					"Unsupported spatial predicate: " + predicate);
		}
		return isRelated;
	}

	/**
	 * Tests whether or not the minimum (orthodromic) distance between two
	 * geometry objects is less than the specified distance. That is,
	 * 
	 * <pre>
	 * DWithin(A,B,d) &#8660; Distance(A,B) < d
	 * </pre>
	 * 
	 * <p>
	 * The computed distance is the shortest distance between the two nearest
	 * points in the geometry instances, as measured along the surface of an
	 * ellipsoid.
	 * </p>
	 * <p>
	 * The unit of measure identifier for the given distance must be either a
	 * standard unit symbol (from UCUM) or an absolute URI that refers to a unit
	 * definition (unsupported). SI prefix symbols may also be used (see
	 * examples below).
	 * </p>
	 * <ul>
	 * <li>m : metre</li>
	 * <li>km : kilometre</li>
	 * <li>[mi_i] : international mile</li>
	 * <li>[nmi_i] : international nautical mile</li>
	 * <ul>
	 *
	 * @param geom1
	 *            An Element node representing a GML geometry instance.
	 * @param geom2
	 *            An Element node representing some other GML geometry instance.
	 * @param distanceWithUom
	 *            An fes:Distance element with a unit of measurement.
	 * @return true if the minimum distance between the geometries is less than
	 *         the given quantity; false otherwise.
	 *
	 * @see <a target="_blank" href="http://unitsofmeasure.org/ucum.html">The
	 *      Unified Code for Units of Measure (UCUM)</a>
	 */
	@SuppressWarnings("unchecked")
	public static boolean isWithinDistance(Node geom1, Node geom2,
			Element distanceWithUom) {
		Geometry g1 = toJTSGeometry(unmarshal(geom1));
		Geometry g2 = toJTSGeometry(unmarshal(geom2));
		double orthodromicDist;
		try {
			g1 = setCRS(g1, JTS.findCoordinateReferenceSystem(g2));
			CoordinateReferenceSystem crs1 = JTS
					.findCoordinateReferenceSystem(g1);
			Coordinate[] nearestPoints = DistanceOp.nearestPoints(g1, g2);
			orthodromicDist = JTS.orthodromicDistance(nearestPoints[0],
					nearestPoints[1], crs1);
		} catch (FactoryException | TransformException e) {
			throw new RuntimeException(e);
		}
		double maxDistance = Double.parseDouble(distanceWithUom
				.getTextContent());
		String uomId = distanceWithUom.getAttribute("uom");
		LOGR.fine(String.format(
				"Max distance = %s %s; calculated orthodromic distance = %s m",
				maxDistance, uomId, orthodromicDist));
		Unit<Length> uom = null;
		if (uomId.contains(":")) {
			// absolute URI ignored
		} else {
			uom = (Unit<Length>) Unit.valueOf(uomId);
		}
		UnitConverter converter = uom.getConverterTo(SI.METRE);
		return orthodromicDist < converter.convert(maxDistance);
	}

	/**
	 * Tests whether or not the orthodromic distance between two geometry
	 * objects is greater than or equal to the specified distance. That is,
	 * 
	 * <pre>
	 * Beyond(A,B,d) &#8660; Distance(A,B) >= d
	 * </pre>
	 * 
	 * @param g1
	 *            An Element node representing a GML geometry instance.
	 * @param g2
	 *            An Element node representing some other GML geometry instance.
	 * @param distanceWithUom
	 *            An fes:Distance element with a unit of measurement.
	 * @return true if the minimum distance between the geometries equals or
	 *         exceeds the given quantity; false otherwise.
	 */
	public static boolean isBeyond(Node g1, Node g2, Element distanceWithUom) {
		return !isWithinDistance(g1, g2, distanceWithUom);
	}

	/**
	 * 
	 * Builds a JTS geometry object from a GML geometry object.
	 * 
	 * @param gmlGeom
	 *            A GML geometry.
	 * @return A JTS geometry, or null if one could not be constructed.
	 */
	static Geometry toJTSGeometry(AbstractGeometry gmlGeom) {
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
