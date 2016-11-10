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
import org.w3c.dom.Element;
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
    public void pointIntersectsPolygon() throws FactoryException, SAXException, IOException, TransformException {
        Document point = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Point-UTM32N.xml"));
        Document polygon = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Polygon.xml"));
        boolean intersects = TopologicalRelationships.isSpatiallyRelated(SpatialOperator.INTERSECTS,
                point.getDocumentElement(), polygon.getDocumentElement());
        Assert.assertTrue("Expected point to intersect polygon.", intersects);
    }

    @Test
    public void curveDoesNotIntersectPolygon() throws SAXException, IOException, TransformException {
        Document curve = docBuilder.parse(this.getClass().getResourceAsStream("/gml/LineString.xml"));
        Document polygon = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Polygon.xml"));
        boolean intersects = TopologicalRelationships.isSpatiallyRelated(SpatialOperator.INTERSECTS,
                curve.getDocumentElement(), polygon.getDocumentElement());
        Assert.assertFalse("Expected curve and polygon to be disjoint.", intersects);
    }

    @Test
    public void pointIntersectsMultiSurface() throws SAXException, IOException, TransformException {
        Document mSurface = docBuilder.parse(this.getClass().getResourceAsStream("/gml/MultiSurface.xml"));
        Document point = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Point.xml"));
        boolean intersects = TopologicalRelationships.isSpatiallyRelated(SpatialOperator.INTERSECTS,
                mSurface.getDocumentElement(), point.getDocumentElement());
        Assert.assertTrue("Expected Point and MultiSurface to intersect.", intersects);
    }

    @Test
    public void pointWithHttpCRSRefIntersectsMultiSurface() throws SAXException, IOException, TransformException {
        Document mSurface = docBuilder.parse(this.getClass().getResourceAsStream("/gml/MultiSurface.xml"));
        Document point = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Point-srsName-http.xml"));
        boolean intersects = TopologicalRelationships.isSpatiallyRelated(SpatialOperator.INTERSECTS,
                mSurface.getDocumentElement(), point.getDocumentElement());
        Assert.assertTrue("Expected Point and MultiSurface to intersect.", intersects);
    }

    @Test
    public void envelopeOperandRaisesException()
            throws FactoryException, SAXException, IOException, TransformException {
        thrown.expect(ClassCastException.class);
        thrown.expectMessage("cannot be cast to org.geotoolkit.gml.xml.AbstractGeometry");
        Document point = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Point-UTM10N.xml"));
        Document envelope = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Envelope.xml"));
        boolean intersects = TopologicalRelationships.isSpatiallyRelated(SpatialOperator.INTERSECTS,
                point.getDocumentElement(), envelope.getDocumentElement());
        Assert.assertFalse(intersects);
    }

    @Test
    public void multiCurveWithCrsIntersectsUTMLineString() throws SAXException, IOException, TransformException {
        Document multiCurve = docBuilder.parse(this.getClass().getResourceAsStream("/gml/MultiCurve.xml"));
        Document line2 = docBuilder.parse(this.getClass().getResourceAsStream("/gml/LineString-2.xml"));
        boolean intersects = TopologicalRelationships.isSpatiallyRelated(SpatialOperator.INTERSECTS,
                multiCurve.getDocumentElement(), line2.getDocumentElement());
        Assert.assertTrue("Expected MultiCurve and LineString (UTM) to intersect.", intersects);
    }

    @Test
    public void disjointCurves() throws SAXException, IOException {
        Document curve1 = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Curve-LineString.xml"));
        Document curve2 = docBuilder.parse(this.getClass().getResourceAsStream("/gml/LineString.xml"));
        boolean disjoint = TopologicalRelationships.isSpatiallyRelated(SpatialOperator.DISJOINT,
                curve1.getDocumentElement(), curve2.getDocumentElement());
        Assert.assertTrue("Expected curves to be disjoint.", disjoint);
    }

    @Test
    public void polygonContainsPoint() throws SAXException, IOException {
        Document point = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Point-2.xml"));
        Document polygon = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Polygon.xml"));
        boolean contains = TopologicalRelationships.isSpatiallyRelated(SpatialOperator.CONTAINS,
                polygon.getDocumentElement(), point.getDocumentElement());
        Assert.assertTrue("Expected polygon CONTAINS point.", contains);
    }

    @Test
    public void pointWithinPolygon() throws SAXException, IOException {
        Document point = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Point-2.xml"));
        Document polygon = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Polygon.xml"));
        boolean within = TopologicalRelationships.isSpatiallyRelated(SpatialOperator.WITHIN, point.getDocumentElement(),
                polygon.getDocumentElement());
        Assert.assertTrue("Expected point WITHIN polygon.", within);
    }

    @Test
    public void pointsWithin8km_PROJCS() throws SAXException, IOException {
        Document point1 = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Point-Atkinson-32610.xml"));
        Document point2 = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Point-Jericho-32610.xml"));
        Element fesDistance = docBuilder.newDocument().createElementNS("http://www.opengis.net/fes/2.0", "Distance");
        fesDistance.setTextContent("8.0");
        fesDistance.setAttribute("uom", "km");
        boolean result = TopologicalRelationships.isWithinDistance(point1.getDocumentElement(),
                point2.getDocumentElement(), fesDistance);
        Assert.assertTrue("Expected points to be less than 8 km apart.", result);
    }

    @Test
    public void pointsNotWithin3nmi_GEOGCS() throws SAXException, IOException {
        Document point1 = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Point-Atkinson-4326.xml"));
        Document point2 = docBuilder.parse(this.getClass().getResourceAsStream("/gml/Point-Jericho-4326.xml"));
        Element fesDistance = docBuilder.newDocument().createElementNS("http://www.opengis.net/fes/2.0", "Distance");
        fesDistance.setTextContent("3.0");
        fesDistance.setAttribute("uom", "[nmi_i]");
        boolean result = TopologicalRelationships.isWithinDistance(point1.getDocumentElement(),
                point2.getDocumentElement(), fesDistance);
        Assert.assertFalse("Expected points to be more than 3 nmi apart.", result);
    }

    @Test
    public void polygonAndLineWithin5km() throws SAXException, IOException {
        Document polygon = docBuilder.parse(getClass().getResourceAsStream("/gml/Polygon-32610.xml"));
        Document line = docBuilder.parse(getClass().getResourceAsStream("/gml/LineString-3.xml"));
        Element fesDistance = docBuilder.newDocument().createElementNS("http://www.opengis.net/fes/2.0", "Distance");
        fesDistance.setTextContent("5.0");
        fesDistance.setAttribute("uom", "km");
        boolean result = TopologicalRelationships.isWithinDistance(polygon.getDocumentElement(),
                line.getDocumentElement(), fesDistance);
        Assert.assertTrue("Expected geometries to be less than 5 km apart.", result);
    }
}
