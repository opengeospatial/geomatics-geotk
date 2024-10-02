package org.opengis.cite.geomatics;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.DoubleStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.xml.MarshallerPool;

import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.geometry.jts.JTSEnvelope2D;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.GMLMarshallerPool;

import org.opengis.cite.geomatics.gml.GmlUtils;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

/**
 * Provides utility methods to create or operate on envelope representations.
 *
 */
public class Extents {

	private static final String CRSREF_OWS = "crs";

	private static final String CRSREF_GML = "srsName";

	private static final String GML_NS = "http://www.opengis.net/gml/3.2";

	private static final GeometryFactory JTS_GEOM_FACTORY = new GeometryFactory();

	private Extents() {
	}

	/**
	 * Calculates the envelope that covers the given collection of GML geometry elements.
	 * @param geomNodes A NodeList containing GML geometry elements; it is assumed these
	 * all refer to the same CRS.
	 * @return An Envelope object representing the overall spatial extent (MBR) of the
	 * geometries.
	 * @throws JAXBException If a node cannot be unmarshalled to a geometry object.
	 */
	@SuppressWarnings("unchecked")
	public static Envelope calculateEnvelope(NodeList geomNodes) throws JAXBException {
		Unmarshaller unmarshaller = null;
		try {
			MarshallerPool pool = GMLMarshallerPool.getInstance();
			unmarshaller = pool.acquireUnmarshaller();
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		org.locationtech.jts.geom.Envelope envelope = new org.locationtech.jts.geom.Envelope();
		CoordinateReferenceSystem crs = null;
		for (int i = 0; i < geomNodes.getLength(); i++) {
			Element geom = (Element) geomNodes.item(i);
			if (geom.getAttribute("srsName").isEmpty()) {
				// check ancestor nodes for CRS reference
				GmlUtils.findCRSReference(geom);
			}
			if (geom.getLocalName().startsWith("Multi")) {
				// explicitly set srsName on all members of geometry collection
				GmlUtils.setSrsNameOnCollectionMembers(geom);
			}

			// Convert to MultiCurve or MultiSurface from Curve or Surface node resp.
			// As geotoolkit(3.21) is not supporting Curve and Surface geometry type.
			if (geom.getLocalName().equals("Curve") || geom.getLocalName().equals("Surface")) {
				geom = GmlUtils.convertToMultiType(geomNodes.item(i));
			}
			JAXBElement<AbstractGeometry> result = (JAXBElement<AbstractGeometry>) unmarshaller.unmarshal(geom);
			AbstractGeometry gmlGeom = result.getValue();
			String srsName = gmlGeom.getSrsName();
			if (srsName.startsWith("http")) {
				// not recognized in Geotk v3
				gmlGeom.setSrsName(GeodesyUtils.convertSRSNameToURN(srsName));
			}
			crs = gmlGeom.getCoordinateReferenceSystem(false);
			Geometry jtsGeom;
			try {
				jtsGeom = GeometrytoJTS.toJTS(gmlGeom);
			}
			catch (FactoryException e) {
				throw new RuntimeException(
						String.format("Failed to create JTS geometry from GML geometry: %s \nCause: %s",
								gmlGeom.toString(), e.getMessage()));
			}
			envelope.expandToInclude(jtsGeom.getEnvelopeInternal());
		}
		return new JTSEnvelope2D(envelope, crs);
	}

	/**
	 * Calculates the envelope using single GML geometry element.
	 * @param geomNodes A NodeList containing GML geometry elements; it is assumed these
	 * all refer to the same CRS.
	 * @return An Envelope object representing the overall spatial extent (MBR) of the
	 * geometries.
	 * @throws JAXBException If a node cannot be unmarshalled to a geometry object.
	 */
	@SuppressWarnings("unchecked")
	public static Envelope calculateEnvelopeUsingSingleGeometry(NodeList geomNodes) throws JAXBException {
		Unmarshaller unmarshaller = null;
		try {
			MarshallerPool pool = GMLMarshallerPool.getInstance();
			unmarshaller = pool.acquireUnmarshaller();
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		org.locationtech.jts.geom.Envelope envelope = new org.locationtech.jts.geom.Envelope();
		CoordinateReferenceSystem crs = null;
		for (int i = 0; i < 1; i++) {
			Element geom = (Element) geomNodes.item(i);
			if (geom.getAttribute("srsName").isEmpty()) {
				// check ancestor nodes for CRS reference
				GmlUtils.findCRSReference(geom);
			}
			if (geom.getLocalName().startsWith("Multi")) {
				// explicitly set srsName on all members of geometry collection
				GmlUtils.setSrsNameOnCollectionMembers(geom);
			}

			// Convert to MultiCurve or MultiSurface from Curve or Surface node resp.
			// As geotoolkit(3.21) is not supporting Curve and Surface geometry type.
			if (geom.getLocalName().equals("Curve") || geom.getLocalName().equals("Surface")) {
				geom = GmlUtils.convertToMultiType(geomNodes.item(i));
			}

			Node geomNode = geomNodes.item(i);
			if (GmlUtils.checkForAbstractSurfacePatchTypes(geomNode)) {
				geom = GmlUtils.handleAbstractSurfacePatch(geomNode);
			}
			JAXBElement<AbstractGeometry> result = (JAXBElement<AbstractGeometry>) unmarshaller.unmarshal(geom);
			AbstractGeometry gmlGeom = result.getValue();
			String srsName = gmlGeom.getSrsName();
			if (srsName.startsWith("http")) {
				// not recognized in Geotk v3
				gmlGeom.setSrsName(GeodesyUtils.convertSRSNameToURN(srsName));
			}
			crs = gmlGeom.getCoordinateReferenceSystem(false);
			Geometry jtsGeom;
			try {
				jtsGeom = GeometrytoJTS.toJTS(gmlGeom);
			}
			catch (FactoryException e) {
				throw new RuntimeException(
						String.format("Failed to create JTS geometry from GML geometry: %s \nCause: %s",
								gmlGeom.toString(), e.getMessage()));
			}
			envelope.expandToInclude(jtsGeom.getEnvelopeInternal());
		}
		return new JTSEnvelope2D(envelope, crs);
	}

	/**
	 * Generates a standard GML representation (gml:Envelope) of an Envelope object.
	 * Ordinates are rounded down to 2 decimal places.
	 * @param envelope An Envelope defining a bounding rectangle (or prism).
	 * @return A DOM Document with gml:Envelope as the document element.
	 */
	public static Document envelopeAsGML(Envelope envelope) {
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		}
		catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		Element gmlEnv = doc.createElementNS(GML_NS, "gml:Envelope");
		doc.appendChild(gmlEnv);
		gmlEnv.setAttribute("srsName", GeodesyUtils.getCRSIdentifier(envelope.getCoordinateReferenceSystem()));
		NumberFormat numFormat = NumberFormat.getNumberInstance(Locale.ROOT);
		DecimalFormat decFormat = DecimalFormat.class.cast(numFormat);
		decFormat.applyPattern("#.##");
		decFormat.setRoundingMode(RoundingMode.DOWN);
		StringBuffer lowerCoord = new StringBuffer();
		StringBuffer upperCoord = new StringBuffer();
		for (int i = 0; i < envelope.getDimension(); i++) {
			lowerCoord.append(decFormat.format(envelope.getMinimum(i)));
			upperCoord.append(decFormat.format(envelope.getMaximum(i)));
			if (i < (envelope.getDimension() - 1)) {
				lowerCoord.append(' ');
				upperCoord.append(' ');
			}
		}
		Element lowerCorner = doc.createElementNS(GML_NS, "gml:lowerCorner");
		lowerCorner.setTextContent(lowerCoord.toString());
		gmlEnv.appendChild(lowerCorner);
		Element upperCorner = doc.createElementNS(GML_NS, "gml:upperCorner");
		upperCorner.setTextContent(upperCoord.toString());
		gmlEnv.appendChild(upperCorner);
		return doc;
	}

	/**
	 * Creates a JTS Polygon having the same extent as the given envelope.
	 * @param envelope An Envelope defining a bounding rectangle.
	 * @return A Polygon with the relevant CoordinateReferenceSystem set as a user data
	 * object.
	 */
	public static Polygon envelopeAsPolygon(Envelope envelope) {
		DirectPosition lowerCorner = envelope.getLowerCorner();
		DirectPosition upperCorner = envelope.getUpperCorner();
		LinearRing ring = JTS_GEOM_FACTORY
			.createLinearRing(new Coordinate[] { new Coordinate(lowerCorner.getOrdinate(0), lowerCorner.getOrdinate(1)),
					new Coordinate(upperCorner.getOrdinate(0), lowerCorner.getOrdinate(1)),
					new Coordinate(upperCorner.getOrdinate(0), upperCorner.getOrdinate(1)),
					new Coordinate(lowerCorner.getOrdinate(0), upperCorner.getOrdinate(1)),
					new Coordinate(lowerCorner.getOrdinate(0), lowerCorner.getOrdinate(1)) });
		Polygon polygon = JTS_GEOM_FACTORY.createPolygon(ring);
		JTS.setCRS(polygon, envelope.getCoordinateReferenceSystem());
		return polygon;
	}

	/**
	 * Coalesces a sequence of bounding boxes so as to create an envelope that covers all
	 * of them. The resulting envelope will use the same CRS as the first bounding box in
	 * the list; the remaining bounding boxes will be transformed to this CRS if
	 * necessary.
	 * @param bboxNodes A list of elements representing common bounding boxes
	 * (ows:BoundingBox, ows:WGS84BoundingBox, or gml:Envelope).
	 * @return An Envelope encompassing the total extent of the given bounding boxes.
	 * @throws FactoryException If an unrecognized CRS reference is encountered or a
	 * corresponding CoordinateReferenceSystem cannot be constructed.
	 * @throws TransformException If an attempt to perform a coordinate transformation
	 * fails for some reason.
	 */
	public static Envelope coalesceBoundingBoxes(List<Node> bboxNodes) throws FactoryException, TransformException {
		GeneralEnvelope totalExtent = null;
		for (Node bboxNode : bboxNodes) {
			Envelope nextEnv = createEnvelope(bboxNode);
			if (null == totalExtent) { // first box
				totalExtent = (GeneralEnvelope) nextEnv;
			}
			else {
				CoordinateReferenceSystem crs = nextEnv.getCoordinateReferenceSystem();
				if (!crs.equals(totalExtent.getCoordinateReferenceSystem())) {
					nextEnv = Envelopes.transform(nextEnv, totalExtent.getCoordinateReferenceSystem());
				}
				totalExtent.add(nextEnv);
			}
		}
		return totalExtent;
	}

	/**
	 * Creates an Envelope from the given XML representation of a spatial extent
	 * (ows:BoundingBox, ows:WGS84BoundingBox, or gml:Envelope).
	 * @param envelopeNode A DOM Node (Document or Element) representing a spatial
	 * envelope.
	 * @return An envelope defining a spatial extent in some coordinate reference system.
	 * @throws FactoryException If an unrecognized CRS reference is encountered or a
	 * corresponding CoordinateReferenceSystem cannot be constructed for some reason.
	 */
	public static Envelope createEnvelope(Node envelopeNode) throws FactoryException {
		Element envElem;
		if (Document.class.isInstance(envelopeNode)) {
			envElem = Document.class.cast(envelopeNode).getDocumentElement();
		}
		else {
			envElem = Element.class.cast(envelopeNode);
		}
		CoordinateReferenceSystem crs = null;
		String crsRef = (envElem.hasAttribute(CRSREF_OWS)) ? envElem.getAttribute(CRSREF_OWS)
				: envElem.getAttribute(CRSREF_GML);
		if (crsRef.isEmpty() || crsRef.equals(GeodesyUtils.OGC_CRS84)) {
			// lon,lat axis order
			crs = CommonCRS.defaultGeographic();
		}
		else {
			String id = GeodesyUtils.getAbbreviatedCRSIdentifier(crsRef);
			crs = CRS.forCode(id);
		}
		GeneralEnvelope env = new GeneralEnvelope(crs);
		String namespaceURI = envElem.getNamespaceURI();
		String lowerCornerName = (namespaceURI.equals(GML_NS)) ? "lowerCorner" : "LowerCorner";
		String[] lowerCoords = envElem.getElementsByTagNameNS(namespaceURI, lowerCornerName)
			.item(0)
			.getTextContent()
			.trim()
			.split("\\s");
		String upperCornerName = (namespaceURI.equals(GML_NS)) ? "upperCorner" : "UpperCorner";
		String[] upperCoords = envElem.getElementsByTagNameNS(namespaceURI, upperCornerName)
			.item(0)
			.getTextContent()
			.trim()
			.split("\\s");
		int dim = lowerCoords.length;
		double[] coords = new double[dim * 2];
		for (int i = 0; i < dim; i++) {
			coords[i] = Double.parseDouble(lowerCoords[i]);
			coords[i + dim] = Double.parseDouble(upperCoords[i]);
		}
		env.setEnvelope(coords);
		return env;
	}

	/**
	 * Returns a String representation of a bounding box suitable for use as a query
	 * parameter value (KVP syntax). The value consists of a comma-separated sequence of
	 * data items as indicated below:
	 *
	 * <pre>
	 * LowerCorner coordinate 1
	 * LowerCorner coordinate 2
	 * LowerCorner coordinate N
	 * ...
	 * UpperCorner coordinate 1
	 * UpperCorner coordinate 2
	 * UpperCorner coordinate N
	 * crs URI (optional - default "urn:ogc:def:crs:OGC:1.3:CRS84")
	 * </pre>
	 *
	 * <dl>
	 * <dt>Examples:</dt>
	 * <dd>49.25,-123.1,50.0,-122.5,urn:ogc:def:crs:EPSG::4326</dd>
	 * <dd>472944,5363287,516011,5456383,urn:ogc:def:crs:EPSG::32610
	 * <dd>-123.1,49.25,-122.5,50.0</dd>
	 * </dl>
	 *
	 * <p>
	 * Note: The colon character (":") is allowed in the query component of a URI and thus
	 * does not need to be escaped. See
	 * <a target="_blank" href= "https://tools.ietf.org/html/rfc3986#section-3.4">RFC
	 * 3986, sec. 3.4</a>.
	 * </p>
	 * @param envelope An envelope specifying a geographic extent.
	 * @return A String suitable for use as a query parameter value (KVP syntax).
	 *
	 * @see <a target="_blank" href=
	 * "http://portal.opengeospatial.org/files/?artifact_id=38867">OGC 06-121r9,
	 * 10.2.3</a>
	 */
	public static String envelopeToString(Envelope envelope) {
		StringBuilder kvp = new StringBuilder();
		double[] lowerCorner = envelope.getLowerCorner().getCoordinate();
		for (int i = 0; i < lowerCorner.length; i++) {
			kvp.append(lowerCorner[i]).append(',');
		}
		double[] upperCorner = envelope.getUpperCorner().getCoordinate();
		for (int i = 0; i < upperCorner.length; i++) {
			kvp.append(upperCorner[i]).append(',');
		}
		CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
		if (!crs.equals(CommonCRS.defaultGeographic())) {
			kvp.append(GeodesyUtils.getCRSIdentifier(crs));
		}
		else {
			kvp.deleteCharAt(kvp.lastIndexOf(","));
		}
		return kvp.toString();
	}

	/**
	 * Returns an envelope that is diametrically opposite to the specified envelope. The
	 * resulting envelope uses the CRS with EPSG code 4326 ("WGS 84").
	 * @param envelope An envelope (rectangle or cuboid).
	 * @return A new Envelope that is located on the opposite side of the earth.
	 */
	public static Envelope antipodalEnvelope(Envelope envelope) {
		GeneralEnvelope antipodalEnv;
		try {
			CoordinateReferenceSystem epsg4326 = CRS.forCode("EPSG:4326");
			if (!envelope.getCoordinateReferenceSystem().equals(epsg4326)) {
				antipodalEnv = new GeneralEnvelope(Envelopes.transform(envelope, epsg4326));
			}
			else {
				antipodalEnv = new GeneralEnvelope(envelope);
			}
		}
		catch (FactoryException | TransformException e) {
			throw new RuntimeException(e);
		}
		double[] apLowerCorner = getAntipode(antipodalEnv.getLowerCorner().getCoordinate());
		double[] apUpperCorner = getAntipode(antipodalEnv.getUpperCorner().getCoordinate());
		// swap first value so corner positions are correct
		double lower0 = apLowerCorner[0];
		apLowerCorner[0] = apUpperCorner[0];
		apUpperCorner[0] = lower0;
		DoubleStream corners = DoubleStream.concat(Arrays.stream(apLowerCorner), Arrays.stream(apUpperCorner));
		antipodalEnv.setEnvelope(corners.toArray());
		return antipodalEnv;
	}

	/**
	 * Returns the antipode of the specified coordinate tuple. The antipode of the point
	 * (&#x3C6;, &#x3B8;) is (-&#x3C6;, &#x3B8; &#xB1; 180).
	 * @param coordTuple An array containing a sequence of coordinate values.
	 * @return A new array representing the antipodal position.
	 */
	public static double[] getAntipode(double[] coordTuple) {
		double[] antipode = Arrays.copyOf(coordTuple, coordTuple.length);
		antipode[0] = -antipode[0];
		if (antipode[1] < 0) {
			antipode[1] += 180;
		}
		else {
			antipode[1] -= 180;
		}
		return antipode;
	}

}
