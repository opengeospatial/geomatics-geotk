package org.opengis.cite.geomatics;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class VerifyTopologicalRelationships {

	private static DocumentBuilder docBuilder;

	@BeforeClass
	public static void initFixture() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		docBuilder = dbf.newDocumentBuilder();
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void pointIntersectsPolygon() throws FactoryException, SAXException,
			IOException, TransformException {
		Document point = docBuilder.parse(this.getClass().getResourceAsStream(
				"/gml/Point-UTM32N.xml"));
		Document polygon = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/Polygon.xml"));
		boolean intersects = TopologicalRelationships.isSpatiallyRelated(
				SpatialRelationship.INTERSECTS, point.getDocumentElement(),
				polygon.getDocumentElement());
		Assert.assertTrue("Expected point to intersect polygon.", intersects);
	}

	@Test
	public void curveDoesNotIntersectPolygon() throws SAXException,
			IOException, TransformException {
		Document curve = docBuilder.parse(this.getClass().getResourceAsStream(
				"/gml/LineString.xml"));
		Document polygon = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/Polygon.xml"));
		boolean intersects = TopologicalRelationships.isSpatiallyRelated(
				SpatialRelationship.INTERSECTS, curve.getDocumentElement(),
				polygon.getDocumentElement());
		Assert.assertFalse("Expected curve and polygon to be disjoint.",
				intersects);
	}

	@Test
	public void pointIntersectsMultiSurface() throws SAXException, IOException,
			TransformException {
		Document mSurface = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/MultiSurface.xml"));
		Document point = docBuilder.parse(this.getClass().getResourceAsStream(
				"/gml/Point.xml"));
		boolean intersects = TopologicalRelationships.isSpatiallyRelated(
				SpatialRelationship.INTERSECTS, mSurface.getDocumentElement(),
				point.getDocumentElement());
		Assert.assertTrue("Expected Point and MultiSurface to intersect.",
				intersects);
	}

	@Test
	public void pointWithHttpCRSRefIntersectsMultiSurface()
			throws SAXException, IOException, TransformException {
		Document mSurface = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/MultiSurface.xml"));
		Document point = docBuilder.parse(this.getClass().getResourceAsStream(
				"/gml/Point-srsName-http.xml"));
		boolean intersects = TopologicalRelationships.isSpatiallyRelated(
				SpatialRelationship.INTERSECTS, mSurface.getDocumentElement(),
				point.getDocumentElement());
		Assert.assertTrue("Expected Point and MultiSurface to intersect.",
				intersects);
	}

	@Test
	public void envelopeOperandRaisesException() throws FactoryException,
			SAXException, IOException, TransformException {
		thrown.expect(ClassCastException.class);
		thrown.expectMessage("cannot be cast to org.geotoolkit.gml.xml.AbstractGeometry");
		Document point = docBuilder.parse(this.getClass().getResourceAsStream(
				"/gml/Point-UTM10N.xml"));
		Document envelope = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/Envelope.xml"));
		boolean intersects = TopologicalRelationships.isSpatiallyRelated(
				SpatialRelationship.INTERSECTS, point.getDocumentElement(),
				envelope.getDocumentElement());
		Assert.assertFalse(intersects);
	}

	@Test
	public void multiCurveWithCrsIntersectsUTMLineString() throws SAXException,
			IOException, TransformException {
		Document multiCurve = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/MultiCurve.xml"));
		Document line2 = docBuilder.parse(this.getClass().getResourceAsStream(
				"/gml/LineString-2.xml"));
		boolean intersects = TopologicalRelationships.isSpatiallyRelated(
				SpatialRelationship.INTERSECTS,
				multiCurve.getDocumentElement(), line2.getDocumentElement());
		Assert.assertTrue(
				"Expected MultiCurve and LineString (UTM) to intersect.",
				intersects);
	}

	@Test
	public void disjointCurves() throws SAXException, IOException {
		Document curve1 = docBuilder.parse(this.getClass().getResourceAsStream(
				"/gml/Curve-LineString.xml"));
		Document curve2 = docBuilder.parse(this.getClass().getResourceAsStream(
				"/gml/LineString.xml"));
		boolean disjoint = TopologicalRelationships.isSpatiallyRelated(
				SpatialRelationship.DISJOINT, curve1.getDocumentElement(),
				curve2.getDocumentElement());
		Assert.assertTrue("Expected curves to be disjoint.", disjoint);
	}

	@Test
	public void polygonContainsPoint() throws SAXException, IOException {
		Document point = docBuilder.parse(this.getClass().getResourceAsStream(
				"/gml/Point-2.xml"));
		Document polygon = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/Polygon.xml"));
		boolean contains = TopologicalRelationships.isSpatiallyRelated(
				SpatialRelationship.CONTAINS, polygon.getDocumentElement(),
				point.getDocumentElement());
		Assert.assertTrue("Expected polygon CONTAINS point.", contains);
	}

	@Test
	public void pointWithinPolygon() throws SAXException, IOException {
		Document point = docBuilder.parse(this.getClass().getResourceAsStream(
				"/gml/Point-2.xml"));
		Document polygon = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/Polygon.xml"));
		boolean within = TopologicalRelationships.isSpatiallyRelated(
				SpatialRelationship.WITHIN, point.getDocumentElement(),
				polygon.getDocumentElement());
		Assert.assertTrue("Expected point WITHIN polygon.", within);
	}

}
