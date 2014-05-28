package org.opengis.cite.geomatics;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.geometry.jts.JTSEnvelope2D;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.xml.MarshallerPool;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Provides utility methods to create or operate on envelope representations.
 * 
 */
public class Extents {

    private static final String GML_NS = "http://www.opengis.net/gml/3.2";
    private static final GeometryFactory JTS_GEOM_FACTORY = new GeometryFactory();

    private Extents() {
    }

    /**
     * Calculates the envelope that covers the given collection of GML geometry
     * elements.
     * 
     * @param geomNodes
     *            A NodeList containing GML geometry elements; it is assumed
     *            these all refer to the same CRS.
     * @return An Envelope object representing the overall spatial extent (MBR)
     *         of the geometries.
     * @throws JAXBException
     *             If a node cannot be unmarshalled to a geometry object.
     */
    @SuppressWarnings("unchecked")
    public static Envelope calculateEnvelope(NodeList geomNodes)
            throws JAXBException {
        Unmarshaller unmarshaller = null;
        try {
            MarshallerPool pool = new MarshallerPool(
                    "org.geotoolkit.gml.xml.v321");
            unmarshaller = pool.acquireUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        com.vividsolutions.jts.geom.Envelope envelope = new com.vividsolutions.jts.geom.Envelope();
        CoordinateReferenceSystem crs = null;
        for (int i = 0; i < geomNodes.getLength(); i++) {
            Node node = geomNodes.item(i);
            JAXBElement<AbstractGeometry> result = (JAXBElement<AbstractGeometry>) unmarshaller
                    .unmarshal(node);
            AbstractGeometry gmlGeom = result.getValue();
            crs = gmlGeom.getCoordinateReferenceSystem();
            Geometry jtsGeom;
            try {
                jtsGeom = GeometrytoJTS.toJTS(gmlGeom);
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
            envelope.expandToInclude(jtsGeom.getEnvelopeInternal());
        }
        return new JTSEnvelope2D(envelope, crs);
    }

    /**
     * Generates a standard GML representation (gml:Envelope) of an Envelope
     * object. Ordinates are rounded down to 2 decimal places.
     * 
     * @param envelope
     *            An Envelope defining a bounding rectangle (or prism).
     * @return A DOM Document with gml:Envelope as the document element.
     */
    public static Document envelopeAsGML(Envelope envelope) {
        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Element gmlEnv = doc.createElementNS(GML_NS, "gml:Envelope");
        doc.appendChild(gmlEnv);
        gmlEnv.setAttribute("srsName", GeodesyUtils.getCRSIdentifier(envelope
                .getCoordinateReferenceSystem()));
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.DOWN);
        StringBuffer lowerCoord = new StringBuffer();
        StringBuffer upperCoord = new StringBuffer();
        for (int i = 0; i < envelope.getDimension(); i++) {
            lowerCoord.append(df.format(envelope.getMinimum(i)));
            upperCoord.append(df.format(envelope.getMaximum(i)));
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
     * 
     * @param envelope
     *            An Envelope defining a bounding rectangle.
     * @return A Polygon with the relevant CoordinateReferenceSystem set as a
     *         user data object.
     */
    public static Polygon envelopeAsPolygon(Envelope envelope) {
        DirectPosition lowerCorner = envelope.getLowerCorner();
        DirectPosition upperCorner = envelope.getUpperCorner();
        LinearRing ring = JTS_GEOM_FACTORY.createLinearRing(new Coordinate[] {
                new Coordinate(lowerCorner.getOrdinate(0), lowerCorner
                        .getOrdinate(1)),
                new Coordinate(upperCorner.getOrdinate(0), lowerCorner
                        .getOrdinate(1)),
                new Coordinate(upperCorner.getOrdinate(0), upperCorner
                        .getOrdinate(1)),
                new Coordinate(lowerCorner.getOrdinate(0), upperCorner
                        .getOrdinate(1)),
                new Coordinate(lowerCorner.getOrdinate(0), lowerCorner
                        .getOrdinate(1)) });
        Polygon polygon = JTS_GEOM_FACTORY.createPolygon(ring);
        JTS.setCRS(polygon, envelope.getCoordinateReferenceSystem());
        return polygon;
    }
}
