package org.opengis.cite.geomatics;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.xml.bind.JAXBException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.cite.geomatics.gml.GmlUtils;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.locationtech.jts.geom.Polygon;

public class VerifyExtents extends CommonTestFixture {

    private static final String GML_NS = "http://www.opengis.net/gml/3.2";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void getEnvelopeAsGML_epsg4326() throws FactoryException {
        GeneralEnvelope envelope = new GeneralEnvelope(CRS.forCode("EPSG:4326"));
        envelope.setEnvelope(49.05478, -123.49675, 50.78534, -122.88663);
        Document gmlEnv = Extents.envelopeAsGML(envelope);
        Element docElem = gmlEnv.getDocumentElement();
        assertEquals("Document element has unexpected [local name].", "Envelope", docElem.getLocalName());
        assertEquals("Unexpected srsName.", "urn:ogc:def:crs:EPSG::4326", docElem.getAttribute("srsName"));
        String[] upperOrds = docElem.getElementsByTagNameNS(GML_NS, "upperCorner").item(0).getTextContent()
                .split("\\s");
        assertEquals("Unexpected ordinate[1] for upper corner.", -122.88, Double.parseDouble(upperOrds[1]), 0.0005);
    }

    @Test
    public void getEnvelopeAsGMLInOtherLocale() throws FactoryException {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        GeneralEnvelope envelope = new GeneralEnvelope(CRS.forCode("EPSG:4326"));
        envelope.setEnvelope(49.05478, -123.49675, 50.78534, -122.88663);
        Document gmlEnv = Extents.envelopeAsGML(envelope);
        Locale.setDefault(defaultLocale);
        Element docElem = gmlEnv.getDocumentElement();
        assertEquals("Document element has unexpected [local name].", "Envelope", docElem.getLocalName());
        assertEquals("Unexpected srsName.", "urn:ogc:def:crs:EPSG::4326", docElem.getAttribute("srsName"));
        String[] upperOrds = docElem.getElementsByTagNameNS(GML_NS, "upperCorner").item(0).getTextContent()
                .split("\\s");
        assertEquals("Unexpected ordinate[1] for upper corner.", -122.88, Double.parseDouble(upperOrds[1]), 0.0005);
    }

    @Test
    public void getEnvelopeAsGML_epsg32610() throws FactoryException {
        CoordinateReferenceSystem epsg32610 = CRS.forCode("EPSG:32610");
        Envelope areaOfUse = CRS.getDomainOfValidity(epsg32610);
        GeneralEnvelope envelope = new GeneralEnvelope(areaOfUse);
        Document gmlEnv = Extents.envelopeAsGML(envelope);
        Element docElem = gmlEnv.getDocumentElement();
        assertEquals("Document element has unexpected [local name].", "Envelope", docElem.getLocalName());
        assertEquals("Unexpected srsName.", "urn:ogc:def:crs:EPSG::32610", docElem.getAttribute("srsName"));
        String[] upperOrds = docElem.getElementsByTagNameNS(GML_NS, "upperCorner").item(0).getTextContent()
                .split("\\s");
        assertEquals("Unexpected ordinate[1] for upper corner.", 9329005, Double.parseDouble(upperOrds[1]), 0.5);
    }

    @Test
    public void getEnvelopeAsPolygon_epsg32610() throws FactoryException {
        CoordinateReferenceSystem epsg32610 = CRS.forCode("EPSG:32610");
        Envelope areaOfUse = CRS.getDomainOfValidity(epsg32610);
        GeneralEnvelope envelope = new GeneralEnvelope(areaOfUse);
        Polygon polygon = Extents.envelopeAsPolygon(envelope);
        assertEquals("Polygon has unexpected CRS as user data.", polygon.getUserData(), epsg32610);
        assertThat("Unexpected Polygon WKT content.", polygon.toText(),
                StringContains.containsString("POLYGON ((166021"));
    }

    @Test
    public void getExtentOfMultiGeometry() throws SAXException, IOException, XPathExpressionException, JAXBException {
        Document multiGeom = docBuilder.parse(this.getClass().getResourceAsStream("/gml/MultiGeometry.xml"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new GmlUtils.NodeNamespaceContext(multiGeom));
        NodeList nodes = (NodeList) xpath.evaluate("//gml:geometryMember/*", multiGeom, XPathConstants.NODESET);
        Envelope envelope = Extents.calculateEnvelope(nodes);
        assertNotNull("Envelope is null.", envelope);
        assertTrue("Expected CRS 'WGS 84'.",
                envelope.getCoordinateReferenceSystem().getName().getCode().contains("WGS 84"));
        DirectPosition upperCorner = envelope.getUpperCorner();
        assertEquals("Unexpected ordinate[0] for upper corner.", 50.55, upperCorner.getOrdinate(0), 0.005);
        assertEquals("Unexpected ordinate[1] for upper corner.", -122.22, upperCorner.getOrdinate(1), 0.005);
    }

    @Test
    public void getExtentOfCurveGeometry() throws SAXException, IOException, XPathExpressionException, JAXBException {
        Document multiGeom = docBuilder.parse(this.getClass().getResourceAsStream("/gml/CurveGeometry.xml"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new GmlUtils.NodeNamespaceContext(multiGeom));
        NodeList nodes = (NodeList) xpath.evaluate("//gml:geometryMember/*", multiGeom, XPathConstants.NODESET);
        Envelope envelope = Extents.calculateEnvelope(nodes);
        assertNotNull("Envelope is null.", envelope);
        assertTrue("Expected CRS 'WGS 84'.",
                envelope.getCoordinateReferenceSystem().getName().getCode().contains("WGS 84"));
        DirectPosition upperCorner = envelope.getUpperCorner();
        assertEquals("Unexpected ordinate[0] for upper corner.", 49.281191, upperCorner.getOrdinate(0), 0.005);
        assertEquals("Unexpected ordinate[1] for upper corner.", -123.125993, upperCorner.getOrdinate(1), 0.005);
    }

    @Test
    public void getExtentOfSurfaceGeometry() throws SAXException, IOException, XPathExpressionException, JAXBException {
        Document multiGeom = docBuilder.parse(this.getClass().getResourceAsStream("/gml/SurfaceGeometry.xml"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new GmlUtils.NodeNamespaceContext(multiGeom));
        NodeList nodes = (NodeList) xpath.evaluate("//gml:geometryMember/*", multiGeom, XPathConstants.NODESET);
        Envelope envelope = Extents.calculateEnvelope(nodes);
        assertNotNull("Envelope is null.", envelope);
        assertTrue("Expected CRS 'ETRS89'.",
                envelope.getCoordinateReferenceSystem().getName().getCode().contains("ETRS89"));
        DirectPosition upperCorner = envelope.getUpperCorner();
        assertEquals("Unexpected ordinate[0] for upper corner.", 52.273881, upperCorner.getOrdinate(0), 0.005);
        assertEquals("Unexpected ordinate[1] for upper corner.", 6.934301, upperCorner.getOrdinate(1), 0.005);
    }

    @Test
    public void totalExtentOfDisjointBoxes_sameCRS()
            throws SAXException, IOException, XPathExpressionException, FactoryException, TransformException {
        Document results = docBuilder.parse(this.getClass().getResourceAsStream("/SearchResults.xml"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new GmlUtils.NodeNamespaceContext(results));
        NodeList boxes = (NodeList) xpath.evaluate(
                "//csw:Record/ows:BoundingBox[1] | //csw:Record/ows:WGS84BoundingBox[1]", results,
                XPathConstants.NODESET);
        List<Node> boxNodes = getNodeListAsList(boxes);
        Envelope envelope = Extents.coalesceBoundingBoxes(boxNodes);
        assertNotNull("Envelope is null.", envelope);
        DirectPosition lowerCorner = envelope.getLowerCorner();
        assertArrayEquals("Unexpected lower corner position.", new double[] { -117.6, 32.0 },
                lowerCorner.getCoordinate(), 0.1);
        DirectPosition upperCorner = envelope.getUpperCorner();
        assertArrayEquals("Unexpected upper corner position.", new double[] { -115.0, 34.0 },
                upperCorner.getCoordinate(), 0.1);
    }

    @Test
    public void totalExtentOfDisjointBoxes_differentCRS()
            throws SAXException, IOException, XPathExpressionException, FactoryException, TransformException {
        Document results = docBuilder.parse(this.getClass().getResourceAsStream("/SearchResults-2.xml"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new GmlUtils.NodeNamespaceContext(results));
        NodeList boxes = (NodeList) xpath.evaluate(
                "//csw:Record/ows:BoundingBox[1] | //csw:Record/ows:WGS84BoundingBox[1]", results,
                XPathConstants.NODESET);
        List<Node> boxNodes = getNodeListAsList(boxes);
        Envelope envelope = Extents.coalesceBoundingBoxes(boxNodes);
        assertNotNull("Envelope is null.", envelope);
        DirectPosition lowerCorner = envelope.getLowerCorner();
        assertArrayEquals("Unexpected lower corner position.", new double[] { 32.0, -117.6 },
                lowerCorner.getCoordinate(), 0.1);
        DirectPosition upperCorner = envelope.getUpperCorner();
        assertArrayEquals("Unexpected upper corner position.", new double[] { 34.0, -115.0 },
                upperCorner.getCoordinate(), 0.1);
    }

    @Test
    public void createEnvelopeFromBoundingBox_EPSG4326() throws SAXException, IOException, FactoryException {
        Document bbox = docBuilder.parse(this.getClass().getResourceAsStream("/envelopes/BoundingBox-4326.xml"));
        Envelope envelope = Extents.createEnvelope(bbox);
        assertNotNull("Envelope is null.", envelope);
        DirectPosition lowerCorner = envelope.getLowerCorner();
        assertArrayEquals("Unexpected lower corner position.", new double[] { 32.0, -117.6 },
                lowerCorner.getCoordinate(), 0.1);
        assertNotNull("CRS is null.", envelope.getCoordinateReferenceSystem());
        assertThat("Unexpected CRS.", envelope.getCoordinateReferenceSystem().getIdentifiers().toString(),
                StringContains.containsString("\"EPSG\", 4326"));
    }

    @Test
    public void createEnvelopeFromGMLEnvelope_EPSG32610() throws SAXException, IOException, FactoryException {
        Document bbox = docBuilder.parse(this.getClass().getResourceAsStream("/envelopes/Envelope-UTM.xml"));
        Envelope envelope = Extents.createEnvelope(bbox);
        assertNotNull("Envelope is null.", envelope);
        DirectPosition lowerCorner = envelope.getLowerCorner();
        assertArrayEquals("Unexpected lower corner position.", new double[] { 514432, 5429689 },
                lowerCorner.getCoordinate(), 0.1);
        assertNotNull("CRS is null.", envelope.getCoordinateReferenceSystem());
        assertThat("Unexpected CRS.", envelope.getCoordinateReferenceSystem().getIdentifiers().toString(),
                StringContains.containsString("\"EPSG\", 32610"));
    }

    @Test
    public void createEnvelopeFromWGS84BoundingBox() throws SAXException, IOException, FactoryException {
        Document bbox = docBuilder.parse(this.getClass().getResourceAsStream("/envelopes/WGS84BoundingBox.xml"));
        Envelope envelope = Extents.createEnvelope(bbox);
        assertNotNull("Envelope is null.", envelope);
        DirectPosition upperCorner = envelope.getUpperCorner();
        assertArrayEquals("Unexpected upper corner position.", new double[] { -115.0, 34.0 },
                upperCorner.getCoordinate(), 0.1);
        assertNotNull("CRS is null.", envelope.getCoordinateReferenceSystem());
        assertThat("Unexpected CRS.", envelope.getCoordinateReferenceSystem().getName().toString(),
                StringContains.containsString("WGS 84"));
    }

    @Test
    public void writeWGS84BoundingBoxToString() {
        GeneralEnvelope envelope = new GeneralEnvelope(CommonCRS.defaultGeographic());
        envelope.setEnvelope(new double[] { -116.0, 32.6, -115.0, 34.0 });
        String kvp = Extents.envelopeToString(envelope);
        assertEquals(kvp, "-116.0,32.6,-115.0,34.0");
    }

    @Test
    public void writeEPSG4326BoundingBoxToString() throws FactoryException {
        GeneralEnvelope envelope = new GeneralEnvelope(CRS.forCode("EPSG:4326"));
        envelope.setEnvelope(new double[] { 32.0, -117.6, 33.5, -116.2 });
        String kvp = Extents.envelopeToString(envelope);
        assertEquals(kvp, "32.0,-117.6,33.5,-116.2,urn:ogc:def:crs:EPSG::4326");
    }

    @Test
    public void envelopeFromLineStrings() throws SAXException, IOException, XPathExpressionException, JAXBException {
        Document multiCurve = docBuilder.parse(this.getClass().getResourceAsStream("/gml/MultiCurve-2.xml"));
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new GmlUtils.NodeNamespaceContext(multiCurve));
        NodeList nodes = (NodeList) xpath.evaluate("//gml:curveMembers/*", multiCurve, XPathConstants.NODESET);
        Envelope envelope = Extents.calculateEnvelope(nodes);
        assertNotNull("Envelope is null.", envelope);
        assertTrue("Expected CRS 'WGS 84'.",
                envelope.getCoordinateReferenceSystem().getName().getCode().contains("WGS 84"));
        DirectPosition upperCorner = envelope.getUpperCorner();
        assertEquals("Unexpected ordinate[0] for upper corner.", 51.92, upperCorner.getOrdinate(0), 0.005);
        assertEquals("Unexpected ordinate[1] for upper corner.", 9.70, upperCorner.getOrdinate(1), 0.005);
    }

    @Test
    public void antipodeOfVancouver() {
        double[] yvr = new double[] { 49.19, -123.18 };
        double[] antipode = Extents.getAntipode(yvr);
        assertArrayEquals(new double[] { -49.19, -123.18 + 180 }, antipode, 0.01);
    }

    @Test
    public void antipodeOfVienna() {
        double[] vie = new double[] { 48.11, 16.57 };
        double[] antipode = Extents.getAntipode(vie);
        assertArrayEquals(new double[] { -48.11, 16.57 - 180 }, antipode, 0.01);
    }

    @Test
    public void antipodeOfPerth() {
        double[] per = new double[] { -31.94, 115.97 };
        double[] antipode = Extents.getAntipode(per);
        assertArrayEquals(new double[] { 31.94, 115.97 - 180 }, antipode, 0.01);
    }

    @Test
    public void antipodalEnvelopeFrom4326() throws SAXException, IOException, FactoryException {
        Document bbox = docBuilder.parse(this.getClass().getResourceAsStream("/envelopes/BoundingBox-4326.xml"));
        Envelope envelope = Extents.createEnvelope(bbox);
        Envelope apEnvelope = Extents.antipodalEnvelope(envelope);
        double[] lowerCorner = apEnvelope.getLowerCorner().getCoordinate();
        assertArrayEquals(new double[] { -33.5, -117.6 + 180 }, lowerCorner, 0.01);
    }

    @Test
    public void antipodalEnvelopeFrom32610() throws SAXException, IOException, FactoryException {
        Document bbox = docBuilder.parse(this.getClass().getResourceAsStream("/envelopes/Envelope-UTM.xml"));
        Envelope envelope = Extents.createEnvelope(bbox);
        Envelope apEnvelope = Extents.antipodalEnvelope(envelope);
        double[] lowerCorner = apEnvelope.getLowerCorner().getCoordinate();
        assertArrayEquals(new double[] { -49.22, 57.20 }, lowerCorner, 0.01);
    }

    List<Node> getNodeListAsList(NodeList nodeList) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            nodes.add(nodeList.item(i));
        }
        return nodes;
    }

}
