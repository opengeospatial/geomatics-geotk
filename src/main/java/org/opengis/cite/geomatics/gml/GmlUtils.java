package org.opengis.cite.geomatics.gml;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.geotoolkit.geometry.GeneralDirectPosition;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gml.xml.AbstractCurveSegment;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.Curve;
import org.geotoolkit.gml.xml.v321.AngleType;
import org.geotoolkit.gml.xml.v321.ArcByCenterPointType;
import org.geotoolkit.gml.xml.v321.LengthType;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.temporal.factory.DefaultTemporalFactory;
import org.geotoolkit.temporal.object.DefaultPosition;
import org.geotoolkit.xml.MarshallerPool;
import org.opengis.cite.geomatics.GeodesyUtils;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.temporal.Instant;
import org.opengis.temporal.TemporalFactory;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.FactoryException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Provides utility methods for processing representations of GML elements.
 * 
 */
public class GmlUtils {

    public static final String SRS_NAME = "srsName";
    public static final String GML_NS = "http://www.opengis.net/gml/3.2";
    /** Total number of arc points to be computed (including end points). */
    static final int TOTAL_ARC_POINTS = 5;
    private static final Unmarshaller GML_UNMARSHALLER = initGmlUnmarshaller();

    private static Unmarshaller initGmlUnmarshaller() {
        Unmarshaller unmarshaller = null;
        try {
            MarshallerPool pool = new MarshallerPool("org.geotoolkit.gml.xml.v321");
            unmarshaller = pool.acquireUnmarshaller();
        } catch (JAXBException je) {
            throw new RuntimeException(je);
        }
        return unmarshaller;
    }

    /**
     * Computes the positions of at least three points on a curve segment
     * representing an arc: the two end points and one or more intermediate
     * points. The total number of points on the arc is specified by
     * {@link #TOTAL_ARC_POINTS}; the approximation improves as the number of
     * points increases. The calculated positions are added to the given list as
     * JTS {@code Coordinate} objects.
     * 
     * @param segment
     *            A curve segment representing an arc (gml:ArcByCenterPoint or
     *            an allowable substitution).
     * @param crs
     *            The CRS used by the curve to which this segment belongs.
     * @param coordList
     *            The collection to which the coordinates will be added.
     */
    public static void inferPointsOnArc(AbstractCurveSegment segment, CoordinateReferenceSystem crs,
            List<Coordinate> coordList) {
        ArcByCenterPointType arc = (ArcByCenterPointType) segment;
        // WARNING: Ignore @srsName on pos, posList elements
        List<Double> centerCoords = (null != arc.getPos()) ? arc.getPos().getValue() : arc.getPosList().getValue();
        GeneralDirectPosition center = new GeneralDirectPosition(crs);
        // can only be used in 2D
        center.setLocation(new double[] { centerCoords.get(0), centerCoords.get(1) });
        AngleType startAngle = arc.getStartAngle();
        AngleType endAngle = arc.getEndAngle();
        if (null == startAngle) { // is CircleByCenterPoint
            startAngle = new AngleType();
            startAngle.setValue(0);
            endAngle = new AngleType();
            endAngle.setValue(360);
        }
        if (endAngle.getValue() == 0)
            endAngle.setValue(360.0); // ensure endAngle > startAngle
        LengthType radius = arc.getRadius();
        double radiusInMeters = lengthInMeters(radius);
        DirectPosition startPos = GeodesyUtils.calculateDestination(center, startAngle.getValue(), radiusInMeters);
        coordList.add(new Coordinate(startPos.getOrdinate(0), startPos.getOrdinate(1)));
        double delta = (endAngle.getValue() - startAngle.getValue()) / (TOTAL_ARC_POINTS - 1);
        for (int i = 1; i < TOTAL_ARC_POINTS - 1; i++) {
            double angle = startAngle.getValue() + (delta * i);
            DirectPosition arcPos = GeodesyUtils.calculateDestination(center, angle, radiusInMeters);
            coordList.add(new Coordinate(arcPos.getOrdinate(0), arcPos.getOrdinate(1)));
        }
        DirectPosition endPos = GeodesyUtils.calculateDestination(center, endAngle.getValue(), radiusInMeters);
        coordList.add(new Coordinate(endPos.getOrdinate(0), endPos.getOrdinate(1)));
    }

    /**
     * Calculates the planar convex hull of the given GML geometry element. The
     * convex hull is the smallest convex geometry that contains the input
     * geometry.
     * 
     * @param gmlGeom
     *            A GML geometry element.
     * @return A JTS Geometry object. This will be a {@code Polygon} if the hull
     *         contains 3 or more points; fewer points will produce a
     *         {@code LineString} or a {@code Point}.
     */
    public static Geometry computeConvexHull(AbstractGeometry gmlGeom) {
        GeometryCoordinateList coordSet = new GeometryCoordinateList();
        Coordinate[] pointSet = coordSet.getCoordinateList(gmlGeom);
        ConvexHull hull = new ConvexHull(pointSet, new GeometryFactory());
        return hull.getConvexHull();
    }

    /**
     * Sets the srsName attribute on all members of a GML geometry collection if
     * it is specified for the collection. If a geometry member already has a
     * srsName attribute it is left as is.
     * 
     * @see "ISO 19136, cl. 10.1.3.2: SRSReferenceGroup"
     * 
     * @param geometryNodes
     *            A sequence of GML geometry elements.
     */
    public static void setSrsNameOnCollectionMembers(Node... geometryNodes) {
        for (Node geomNode : geometryNodes) {
            Element geom = (Element) geomNode;
            String geomType = geom.getLocalName();
            String srsName = geom.getAttribute(SRS_NAME);
            if (!geomType.startsWith("Multi") || srsName.isEmpty()) {
                continue;
            }
            String memberType = geomType.substring(5).toLowerCase();
            // both kinds of properties may appear in the same collection
            String expr = String.format("gml:%sMember/* | gml:%<sMembers/*", memberType);
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(new GmlNamespaceContext());
            NodeList members;
            try {
                members = (NodeList) xpath.evaluate(expr, geom, XPathConstants.NODESET);
            } catch (XPathExpressionException xpe) {
                throw new RuntimeException(xpe);
            }
            for (int i = 0; i < members.getLength(); i++) {
                Element member = (Element) members.item(i);
                if (member.getAttribute(SRS_NAME).isEmpty()) {
                    member.setAttribute(SRS_NAME, srsName);
                }
            }
        }
    }

    /**
     * Converts the given length measurement to meters. The unit of measurement
     * is identified by its symbol or by URI reference (containing the symbol in
     * the fragment part). Common units of length are shown in the following
     * table. Standard SI prefix symbols may also be used to specify decimal
     * multiples and submultiples of the unit of length (e.g. km).
     * 
     * <table border="1">
     * <caption>Units of length</caption>
     * <tr>
     * <th>Unit</th>
     * <th>Symbol(s)</th>
     * <th>Length /m</th>
     * </tr>
     * <tr>
     * <td>meter</td>
     * <td>m</td>
     * <td>1</td>
     * </tr>
     * <tr>
     * <td>mile</td>
     * <td>mi</td>
     * <td>1609.34</td>
     * </tr>
     * <tr>
     * <td>nautical mile</td>
     * <td>M, NM, [nmi_i]</td>
     * <td>1852</td>
     * </tr>
     * </table>
     * 
     * @see <a href="http://www.bipm.org/en/si/si_brochure/">SI brochure (8th
     *      edition)</a>
     * @see <a href="http://unitsofmeasure.org/ucum.html">The Unified Code for
     *      Units of Measure</a>
     * 
     * @param length
     *            A length measurement.
     * @return The length in meters.
     * 
     */
    public static double lengthInMeters(LengthType length) {
        String uom = length.getUom();
        String symbol = uom.indexOf('#') >= 0 ? uom.substring(uom.indexOf('#') + 1) : uom;
        double lengthInMeters;
        if (symbol.equals("m")) {
            lengthInMeters = length.getValue();
        } else if (symbol.equals("km")) {
            lengthInMeters = length.getValue() * 1000;
        } else if (symbol.equals("M") | symbol.equals("NM") | symbol.equals("[nmi_i]")) {
            lengthInMeters = length.getValue() * 1852.0;
        } else if (symbol.equals("mi")) {
            lengthInMeters = length.getValue() * 1609.34;
        } else {
            throw new RuntimeException("Unrecognized unit of length: " + uom);
        }
        return lengthInMeters;
    }

    /**
     * Extracts (2D) coordinates from a sequence of coordinate tuples and adds
     * them to a list.
     * 
     * @param tupleList
     *            A sequence of coordinate tuples within the same coordinate
     *            reference system (CRS).
     * @param crsDim
     *            The dimension of the CRS.
     * @param coords
     *            The list to which the coordinates will be added.
     */
    public static void extractCoordinatesFromPosList(List<Double> tupleList, int crsDim, List<Coordinate> coords) {
        if (null == tupleList || tupleList.isEmpty()) {
            return;
        }
        Double[] values = tupleList.toArray(new Double[0]);
        for (int i = 0; i < values.length; i = i + crsDim) {
            coords.add(new Coordinate(values[i], values[i + 1]));
        }
    }

    /**
     * Indicates the minimum number of direct positions required to specify a
     * GML curve segment. The value depends on the type of curve segment, but
     * falls in the range 1-3.
     * 
     * @param segmentTypeName
     *            The local name of element representing a the curve segment.
     * @return An integer value &gt; 0.
     */
    public static int minCurveSegmentLength(String segmentTypeName) {
        int minLength = 2;
        if (segmentTypeName.endsWith("ByCenterPoint")) {
            minLength = 1;
        } else if (segmentTypeName.equals("ArcString") || segmentTypeName.equals("Arc")
                || segmentTypeName.equals("Circle")) {
            minLength = 3;
        }
        return minLength;
    }

    /**
     * Returns the value of the srsName attribute for the given geometry
     * element. If a geometry element does not explicitly carry the srsName
     * attribute, then it shall be inherited from either:
     * <ol>
     * <li>the nearest ancestor geometry (aggregate) that has the srsName
     * attribute, or</li>
     * <li>the gml:boundedBy/gml:Envelope element in the containing feature
     * instance.</li>
     * </ol>
     * 
     * <p>
     * As a side effect, an implicit CRS reference will be added to the element
     * using the inherited srsName value.
     * </p>
     * 
     * @see "ISO 19136, cl. 9.10, 10.1.3.2"
     * 
     * @param geom
     *            An Element representing a GML geometry object.
     * @return A String denoting a CRS reference (an absolute URI value), or an
     *         empty string if no reference was found.
     */
    public static String findCRSReference(Element geom) {
        String expr = "./ancestor-or-self::*[@srsName][1]/@srsName";
        XPath xpath = XPathFactory.newInstance().newXPath();
        String srsName;
        try {
            srsName = (String) xpath.evaluate(expr, geom, XPathConstants.STRING);
            if (srsName.isEmpty()) {
                // Look for envelope of containing feature
                NamespaceContext nsContext = new NodeNamespaceContext(geom);
                String gmlPrefix = nsContext.getPrefix(GML_NS);
                expr = String.format("./ancestor::*[%s:boundedBy][1]/%1$s:boundedBy/%1$s:Envelope/@srsName", gmlPrefix);
                xpath.setNamespaceContext(nsContext);
                srsName = (String) xpath.evaluate(expr, geom, XPathConstants.STRING);
                if (srsName.isEmpty()) {
                    // look at child gml:posList, gml:pos elements
                    expr = String.format("(./%s:posList | ./%1$s:pos)[1]/@srsName", gmlPrefix);
                    srsName = (String) xpath.evaluate(expr, geom, XPathConstants.STRING);
                }
            }
        } catch (XPathExpressionException xpe) {
            throw new RuntimeException(xpe);
        }

        if (!srsName.isEmpty()) {
            geom.setAttribute("srsName", srsName);
        }
        return srsName;
    }

    /**
     * A NamespaceContext that only recognizes the conventional "gml" namespace
     * prefix.
     */
    public static class GmlNamespaceContext implements NamespaceContext {
        @Override
        public String getNamespaceURI(String prefix) {
            String nsName;
            if (prefix.equals("gml")) {
                nsName = GML_NS;
            } else {
                nsName = XMLConstants.NULL_NS_URI;
            }
            return nsName;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            String prefix = null;
            if (namespaceURI.equals(GML_NS)) {
                prefix = "gml";
            }
            return prefix;
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            return null;
        }
    }

    /**
     * A NamespaceContext that provides the in-scope namespace bindings for a
     * given DOM Node.
     */
    public static class NodeNamespaceContext implements NamespaceContext {

        private Node sourceNode;

        public NodeNamespaceContext(Node node) {
            sourceNode = node;
        }

        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
                return sourceNode.lookupNamespaceURI(null);
            } else {
                return sourceNode.lookupNamespaceURI(prefix);
            }
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return sourceNode.lookupPrefix(namespaceURI);
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            return null;
        }
    }

    /**
     * Checks if a DOM Element has a child element with the given qualified
     * name.
     * 
     * @param elem
     *            A DOM Element.
     * @param namespace
     *            A namespace name (absolute URI).
     * @param localName
     *            A String representing the local name of an element.
     * 
     * @return {@code true} if one or more matching child elements are present;
     *         {@code false} otherwise.
     */
    public static boolean hasChildElement(Element elem, String namespace, String localName) {
        return elem.getElementsByTagNameNS(namespace, localName).getLength() > 0;
    }

    /**
     * Deserializes an XML resource into a GML geometry representation.
     * 
     * @param uriRef
     *            An absolute URI that specifies the location of the resource.
     * @return A GML geometry object.
     * @throws JAXBException
     *             If any unexpected errors occur while unmarshalling.
     */
    public static AbstractGeometry unmarshalGMLGeometry(URI uriRef) throws JAXBException {
        if (!uriRef.isAbsolute()) {
            throw new IllegalArgumentException("Not an absolute URI: " + uriRef);
        }
        Source source = new StreamSource(uriRef.toString());
        return unmarshalGMLGeometry(source);
    }

    /**
     * Deserializes an XML source into a GML geometry representation.
     * 
     * @param source
     *            The source to read from (providers are only required to
     *            support SAXSource, DOMSource, and StreamSource).
     * @return A GML geometry object.
     * @throws JAXBException
     *             If any unexpected errors occur while unmarshalling.
     */
    @SuppressWarnings("unchecked")
    public static AbstractGeometry unmarshalGMLGeometry(Source source) throws JAXBException {
        JAXBElement<AbstractGeometry> gmlGeom = (JAXBElement<AbstractGeometry>) GML_UNMARSHALLER.unmarshal(source);
        return gmlGeom.getValue();
    }

    /**
     * Creates a JTS LineString geometry from a GML Curve geometry. Some points
     * may be inferred if not given explicitly (e.g. on arc-based segments).
     * 
     * @param gmlCurve
     *            A GML curve.
     * @return A LineString, or null if one could not be constructed.
     */
    public static LineString buildLineString(Curve gmlCurve) {
        CoordinateListFactory coordFactory = new CurveCoordinateListFactory();
        List<Coordinate> coordList = coordFactory.createCoordinateList(gmlCurve);
        GeodesyUtils.removeConsecutiveDuplicates(coordList, 1);
        Coordinate[] coords = coordList.toArray(new Coordinate[coordList.size()]);
        GeometryFactory jtsFactory = new GeometryFactory();
        LineString line = jtsFactory.createLineString(coords);
        // add CRS to user data
        CoordinateReferenceSystem crs = null;
        try {
            crs = CRS.decode(GeodesyUtils.convertSRSNameToURN(gmlCurve.getSrsName()));
            JTS.setCRS(line, crs);
        } catch (FactoryException e) {
            throw new RuntimeException(e.getMessage());
        }
        return line;
    }

    /**
     * Creates a TemporalGeometricPrimitive instance from a GML temporal value
     * representation.
     * 
     * @param gmlTime
     *            A gml:TimeInstant or gml:TimePeriod element.
     * @return A TemporalGeometricPrimitive object (instant or period).
     */
    public static TemporalGeometricPrimitive gmlToTemporalGeometricPrimitive(Element gmlTime) {
        List<ZonedDateTime> instants = new ArrayList<>();
        String frame = gmlTime.getAttribute("frame");
        if (gmlTime.getLocalName().equals("TimeInstant")) {
            Element timePosition = (Element) gmlTime.getElementsByTagNameNS(GML_NS, "timePosition").item(0);
            if (!timePosition.getAttribute("frame").isEmpty()) {
                frame = timePosition.getAttribute("frame");
            }
            if (frame.isEmpty() || frame.contains("8601")) {
                try {
                    ZonedDateTime zdt = ZonedDateTime.parse(timePosition.getTextContent(),
                            DateTimeFormatter.ISO_DATE_TIME);
                    instants.add(zdt);
                } catch (DateTimeParseException dtpe) {
                    // TODO: use starting instant of date, gYearMonth, gYear
                    throw new RuntimeException("Not an ISO instant: " + timePosition.getTextContent());
                }
            } else {
                throw new RuntimeException("Unsupported temporal reference frame: " + frame);
            }
        } else { // gml:TimePeriod
            Element beginPosition = (Element) gmlTime.getElementsByTagNameNS(GML_NS, "beginPosition").item(0);
            instants.add(ZonedDateTime.parse(beginPosition.getTextContent(), DateTimeFormatter.ISO_DATE_TIME));
            Element endPosition = (Element) gmlTime.getElementsByTagNameNS(GML_NS, "endPosition").item(0);
            instants.add(ZonedDateTime.parse(endPosition.getTextContent(), DateTimeFormatter.ISO_DATE_TIME));
        }
        TemporalFactory tmFactory = new DefaultTemporalFactory();
        TemporalGeometricPrimitive timePrimitive = null;
        if (instants.size() == 1) {
            timePrimitive = tmFactory.createInstant(new DefaultPosition(Date.from(instants.get(0).toInstant())));
        } else {
            Instant beginInstant = tmFactory.createInstant(new DefaultPosition(Date.from(instants.get(0).toInstant())));
            Instant endInstant = tmFactory.createInstant(new DefaultPosition(Date.from(instants.get(1).toInstant())));
            timePrimitive = tmFactory.createPeriod(beginInstant, endInstant);
        }
        return timePrimitive;
    }

    /**
     * Convert Surface or Curve geometry to MultiSurface or MultiCurve.
     * 
     * @param geomNode
     *            Element with Surface/Curve details.
     * 
     * @return {@link Element} Returns converted element to Multi geometry type.
     */
    public static Element convertToMultiType(Node geomNode) {
        String typeName = "Multi" + geomNode.getLocalName();
        String geomMemberType = geomNode.getLocalName().equalsIgnoreCase("Curve") ? "curveMember" : "surfaceMember";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;

        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage());
        }

        Document document = db.newDocument();
        Element multiGeom = document.createElementNS(geomNode.getNamespaceURI(), typeName);
        Element memberType = document.createElementNS(geomNode.getNamespaceURI(), geomMemberType);
        Node importNode = document.importNode(geomNode, true);
        NamedNodeMap attributes = geomNode.getAttributes();

        for (Integer i = 0; i < attributes.getLength(); i++) {
            String attributeNamespace = attributes.item(i).getNamespaceURI();
            String attributeName = attributes.item(i).getLocalName();
            String attributeValue = attributes.item(i).getNodeValue();

            multiGeom.setAttributeNS(attributeNamespace, attributeName, attributeValue);
        }

        // In case of Surface geometry type
        if (typeName.equalsIgnoreCase("MultiSurface")) {
            Element polygon = document.createElementNS(geomNode.getNamespaceURI(), "Polygon");

            NodeList nodeList = importNode.getChildNodes();
            for (Integer i = 0; i < nodeList.getLength(); i++) {
                Node currentNode = nodeList.item(i);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (currentNode.getLocalName().equalsIgnoreCase("exterior")) {
                        importNode = currentNode;
                        break;
                    } else {
                        nodeList = currentNode.getChildNodes();
                        i = -1;
                    }
                }
            }
            polygon.appendChild(importNode);
            importNode = polygon;
        }

        memberType.appendChild(importNode);
        multiGeom.appendChild(memberType);
        return multiGeom;
    }
}
