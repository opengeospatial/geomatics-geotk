package org.opengis.cite.geomatics;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.geotoolkit.geometry.Envelopes;
import org.geotoolkit.geometry.GeneralEnvelope;
import org.geotoolkit.referencing.CRS;
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

import com.vividsolutions.jts.geom.Polygon;

public class VerifyExtents extends CommonTestFixture {

	private static final String GML_NS = "http://www.opengis.net/gml/3.2";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void getEnvelopeAsGML_epsg4326() throws FactoryException {
		GeneralEnvelope envelope = new GeneralEnvelope(CRS.decode("EPSG:4326"));
		envelope.setEnvelope(49.05478, -123.49675, 50.78534, -122.88663);
		Document gmlEnv = Extents.envelopeAsGML(envelope);
		Element docElem = gmlEnv.getDocumentElement();
		assertEquals("Document element has unexpected [local name].",
				"Envelope", docElem.getLocalName());
		assertEquals("Unexpected srsName.", "urn:ogc:def:crs:EPSG::4326",
				docElem.getAttribute("srsName"));
		String[] upperOrds = docElem
				.getElementsByTagNameNS(GML_NS, "upperCorner").item(0)
				.getTextContent().split("\\s");
		assertEquals("Unexpected ordinate[1] for upper corner.", -122.88,
				Double.parseDouble(upperOrds[1]), 0.0005);
	}

	@Test
	public void getEnvelopeAsGML_epsg32610() throws FactoryException {
		CoordinateReferenceSystem epsg32610 = CRS.decode("EPSG:32610");
		Envelope areaOfUse = Envelopes.getDomainOfValidity(epsg32610);
		GeneralEnvelope envelope = new GeneralEnvelope(areaOfUse);
		Document gmlEnv = Extents.envelopeAsGML(envelope);
		Element docElem = gmlEnv.getDocumentElement();
		assertEquals("Document element has unexpected [local name].",
				"Envelope", docElem.getLocalName());
		assertEquals("Unexpected srsName.", "urn:ogc:def:crs:EPSG::32610",
				docElem.getAttribute("srsName"));
		String[] upperOrds = docElem
				.getElementsByTagNameNS(GML_NS, "upperCorner").item(0)
				.getTextContent().split("\\s");
		assertEquals("Unexpected ordinate[1] for upper corner.", 9329005,
				Double.parseDouble(upperOrds[1]), 0.5);
	}

	@Test
	public void getEnvelopeAsPolygon_epsg32610() throws FactoryException {
		CoordinateReferenceSystem epsg32610 = CRS.decode("EPSG:32610");
		Envelope areaOfUse = Envelopes.getDomainOfValidity(epsg32610);
		GeneralEnvelope envelope = new GeneralEnvelope(areaOfUse);
		Polygon polygon = Extents.envelopeAsPolygon(envelope);
		assertEquals("Polygon has unexpected CRS as user data.",
				polygon.getUserData(), epsg32610);
		assertThat("Unexpected Polygon WKT content.", polygon.toText(),
				StringContains.containsString("POLYGON ((166021"));
	}

	@Test
	public void getExtentOfMultiGeometry() throws SAXException, IOException,
			XPathExpressionException, JAXBException {
		Document multiGeom = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/MultiGeometry.xml"));
		XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(new GmlUtils.NodeNamespaceContext(multiGeom));
		NodeList nodes = (NodeList) xpath.evaluate("//gml:geometryMember/*",
				multiGeom, XPathConstants.NODESET);
		Envelope envelope = Extents.calculateEnvelope(nodes);
		assertNotNull("Envelope is null.", envelope);
		assertTrue("Expected CRS 'WGS 84'.",
				envelope.getCoordinateReferenceSystem().getName().getCode()
						.contains("WGS 84"));
		DirectPosition upperCorner = envelope.getUpperCorner();
		assertEquals("Unexpected ordinate[0] for upper corner.", 50.55,
				upperCorner.getOrdinate(0), 0.005);
		assertEquals("Unexpected ordinate[1] for upper corner.", -122.22,
				upperCorner.getOrdinate(1), 0.005);
	}

	@Test
	public void totalExtentOfDisjointBoxes_sameCRS() throws SAXException,
			IOException, XPathExpressionException, FactoryException,
			TransformException {
		Document results = docBuilder.parse(this.getClass()
				.getResourceAsStream("/SearchResults.xml"));
		XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(new GmlUtils.NodeNamespaceContext(results));
		NodeList boxes = (NodeList) xpath
				.evaluate(
						"//csw:Record/ows:BoundingBox[1] | //csw:Record/ows:WGS84BoundingBox[1]",
						results, XPathConstants.NODESET);
		List<Node> boxNodes = getNodeListAsList(boxes);
		Envelope envelope = Extents.coalesceBoundingBoxes(boxNodes);
		assertNotNull("Envelope is null.", envelope);
		DirectPosition lowerCorner = envelope.getLowerCorner();
		assertArrayEquals("Unexpected lower corner position.", new double[] {
				-117.6, 32.0 }, lowerCorner.getCoordinate(), 0.1);
		DirectPosition upperCorner = envelope.getUpperCorner();
		assertArrayEquals("Unexpected upper corner position.", new double[] {
				-115.0, 34.0 }, upperCorner.getCoordinate(), 0.1);
	}

	@Test
	public void totalExtentOfDisjointBoxes_differentCRS() throws SAXException,
			IOException, XPathExpressionException, FactoryException,
			TransformException {
		Document results = docBuilder.parse(this.getClass()
				.getResourceAsStream("/SearchResults-2.xml"));
		XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(new GmlUtils.NodeNamespaceContext(results));
		NodeList boxes = (NodeList) xpath
				.evaluate(
						"//csw:Record/ows:BoundingBox[1] | //csw:Record/ows:WGS84BoundingBox[1]",
						results, XPathConstants.NODESET);
		List<Node> boxNodes = getNodeListAsList(boxes);
		Envelope envelope = Extents.coalesceBoundingBoxes(boxNodes);
		assertNotNull("Envelope is null.", envelope);
		DirectPosition lowerCorner = envelope.getLowerCorner();
		assertArrayEquals("Unexpected lower corner position.", new double[] {
				32.0, -117.6 }, lowerCorner.getCoordinate(), 0.1);
		DirectPosition upperCorner = envelope.getUpperCorner();
		assertArrayEquals("Unexpected upper corner position.", new double[] {
				34.0, -115.0 }, upperCorner.getCoordinate(), 0.1);
	}

	@Test
	public void createEnvelopeFromBoundingBox_EPSG4326() throws SAXException,
			IOException, FactoryException {
		Document bbox = docBuilder.parse(this.getClass().getResourceAsStream(
				"/envelopes/BoundingBox-4326.xml"));
		Envelope envelope = Extents.createEnvelope(bbox);
		assertNotNull("Envelope is null.", envelope);
		DirectPosition lowerCorner = envelope.getLowerCorner();
		assertArrayEquals("Unexpected lower corner position.", new double[] {
				32.0, -117.6 }, lowerCorner.getCoordinate(), 0.1);
		assertNotNull("CRS is null.", envelope.getCoordinateReferenceSystem());
		assertThat("Unexpected CRS.", envelope.getCoordinateReferenceSystem()
				.getIdentifiers().toString(),
				StringContains.containsString("EPSG:4326"));
	}

	@Test
	public void createEnvelopeFromGMLEnvelope_EPSG32610() throws SAXException,
			IOException, FactoryException {
		Document bbox = docBuilder.parse(this.getClass().getResourceAsStream(
				"/envelopes/Envelope-UTM.xml"));
		Envelope envelope = Extents.createEnvelope(bbox);
		assertNotNull("Envelope is null.", envelope);
		DirectPosition lowerCorner = envelope.getLowerCorner();
		assertArrayEquals("Unexpected lower corner position.", new double[] {
				514432, 5429689 }, lowerCorner.getCoordinate(), 0.1);
		assertNotNull("CRS is null.", envelope.getCoordinateReferenceSystem());
		assertThat("Unexpected CRS.", envelope.getCoordinateReferenceSystem()
				.getIdentifiers().toString(),
				StringContains.containsString("EPSG:32610"));
	}

	@Test
	public void createEnvelopeFromWGS84BoundingBox() throws SAXException,
			IOException, FactoryException {
		Document bbox = docBuilder.parse(this.getClass().getResourceAsStream(
				"/envelopes/WGS84BoundingBox.xml"));
		Envelope envelope = Extents.createEnvelope(bbox);
		assertNotNull("Envelope is null.", envelope);
		DirectPosition upperCorner = envelope.getUpperCorner();
		assertArrayEquals("Unexpected upper corner position.", new double[] {
				-115.0, 34.0 }, upperCorner.getCoordinate(), 0.1);
		assertNotNull("CRS is null.", envelope.getCoordinateReferenceSystem());
		assertThat("Unexpected CRS.", envelope.getCoordinateReferenceSystem()
				.getName().toString(), StringContains.containsString("WGS84"));
	}

	List<Node> getNodeListAsList(NodeList nodeList) {
		List<Node> nodes = new ArrayList<>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			nodes.add(nodeList.item(i));
		}
		return nodes;
	}

}
