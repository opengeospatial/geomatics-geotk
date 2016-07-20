package org.opengis.cite.geomatics.gml;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.geotoolkit.gml.xml.AbstractCurveSegment;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.Point;
import org.geotoolkit.gml.xml.v321.CurveType;
import org.geotoolkit.gml.xml.v321.LengthType;
import org.geotoolkit.xml.MarshallerPool;
import org.hamcrest.core.StringContains;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.cite.geomatics.GeodesyUtils;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class VerifyGmlUtils {

	private static DocumentBuilder docBuilder;
	private static final String EPSG_32632 = "urn:ogc:def:crs:EPSG::32632";
	private static Unmarshaller gmlUnmarshaller;
	private static GeometryFactory geomFactory;

	@BeforeClass
	public static void initFixture() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		docBuilder = dbf.newDocumentBuilder();
		MarshallerPool pool = new MarshallerPool("org.geotoolkit.gml.xml.v321");
		gmlUnmarshaller = pool.acquireUnmarshaller();
		geomFactory = new GeometryFactory();
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void setSrsNameOnMultiCurveMembers() throws SAXException,
			IOException {
		Document multiGeom = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/MultiCurve.xml"));
		GmlUtils.setSrsNameOnCollectionMembers(multiGeom.getDocumentElement());
		NodeList members = multiGeom.getElementsByTagNameNS(GmlUtils.GML_NS,
				"LineString");
		Element line1 = (Element) members.item(0);
		assertEquals("LineString has unexpected @srsName value.",
				GeodesyUtils.EPSG_4326, line1.getAttribute(GmlUtils.SRS_NAME));
		Element line2 = (Element) members.item(1);
		assertEquals("LineString has unexpected @srsName value.", EPSG_32632,
				line2.getAttribute(GmlUtils.SRS_NAME));
	}

	@Test
	public void verifyFindCRSReference_fromAggregate()
			throws URISyntaxException, SAXException, IOException {
		Document gmlGeom = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/MultiPoint-1.xml"));
		NodeList points = gmlGeom.getDocumentElement().getElementsByTagNameNS(
				GmlUtils.GML_NS, "Point");
		Element p2 = (Element) points.item(1);
		assertTrue("Expected missing @srsName.", p2.getAttribute("srsName")
				.isEmpty());
		String crsRef = GmlUtils.findCRSReference(p2);
		assertEquals(
				"Point has unexpected CRS reference: "
						+ p2.getAttributeNS(GmlUtils.GML_NS, "id"),
				"urn:ogc:def:crs:EPSG::32610", crsRef);
		String srsName = p2.getAttribute("srsName");
		assertThat(srsName, StringContains.containsString("32610"));
	}

	@Test
	public void verifyFindCRSReference_fromFeatureEnvelope()
			throws URISyntaxException, SAXException, IOException {
		Document feature = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/FeatureCollection-1.xml"));
		NodeList points = feature.getDocumentElement().getElementsByTagNameNS(
				GmlUtils.GML_NS, "Point");
		Element p1 = (Element) points.item(0);
		assertTrue("Expected missing @srsName.", p1.getAttribute("srsName")
				.isEmpty());
		String crsRef = GmlUtils.findCRSReference(p1);
		assertEquals(
				"Point has unexpected CRS reference: "
						+ p1.getAttributeNS(GmlUtils.GML_NS, "id"),
				"urn:ogc:def:crs:EPSG::4326", crsRef);
		String srsName = p1.getAttribute("srsName");
		assertThat(srsName, StringContains.containsString("4326"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void computeConvexHullForCurveWithLineStringSegment()
			throws JAXBException {
		URL url = this.getClass().getResource("/gml/Curve-LineString.xml");
		JAXBElement<CurveType> result = (JAXBElement<CurveType>) gmlUnmarshaller
				.unmarshal(url);
		CurveType curve = result.getValue();
		Geometry hull = GmlUtils.computeConvexHull(curve);
		assertEquals("Convex hull has unexpected geometry type.", "Polygon",
				hull.getGeometryType());
		Polygon polygon = (Polygon) hull;
		assertEquals("Unexpected number of interior rings.", 0,
				polygon.getNumInteriorRing());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void computeConvexHullForCurveWithArcSegment() throws JAXBException {
		URL url = this.getClass()
				.getResource("/gml/Curve-ArcByCenterPoint.xml");
		JAXBElement<CurveType> result = (JAXBElement<CurveType>) gmlUnmarshaller
				.unmarshal(url);
		CurveType curve = result.getValue();
		Geometry hull = GmlUtils.computeConvexHull(curve);
		Polygon polygon = Polygon.class.cast(hull);
		Geometry centerPoint = geomFactory.createPoint(new Coordinate(
				49.194722, -123.183889));
		assertTrue(
				"Expect hull to contain center point " + centerPoint.toText(),
				polygon.contains(centerPoint));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void inferPointsOnArcByCenterPoint() throws JAXBException {
		URL url = this.getClass()
				.getResource("/gml/Curve-ArcByCenterPoint.xml");
		JAXBElement<CurveType> result = (JAXBElement<CurveType>) gmlUnmarshaller
				.unmarshal(url);
		CurveType curve = result.getValue();
		AbstractCurveSegment segment = curve.getSegments()
				.getAbstractCurveSegment().get(0);
		List<Coordinate> coordSet = new ArrayList<Coordinate>();
		GmlUtils.inferPointsOnArc(segment,
				curve.getCoordinateReferenceSystem(), coordSet);
		assertEquals("Unexpected number of points on arc.",
				GmlUtils.TOTAL_ARC_POINTS, coordSet.size());
		// end of arc is 10 NM north of center point
		Coordinate arcEnd = coordSet.get(GmlUtils.TOTAL_ARC_POINTS - 1);
		assertEquals("End of arc has unexpected latitude", 49.19472 + 0.16653,
				arcEnd.x, 0.00015);
		assertEquals("End of arc has unexpected longitude", -123.18389,
				arcEnd.y, 0.00015);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void inferPointsOnCircleByCenterPoint() throws JAXBException {
		URL url = this.getClass().getResource(
				"/gml/Curve-CircleByCenterPoint.xml");
		JAXBElement<CurveType> result = (JAXBElement<CurveType>) gmlUnmarshaller
				.unmarshal(url);
		CurveType curve = result.getValue();
		AbstractCurveSegment segment = curve.getSegments()
				.getAbstractCurveSegment().get(0);
		List<Coordinate> coordSet = new ArrayList<Coordinate>();
		GmlUtils.inferPointsOnArc(segment,
				curve.getCoordinateReferenceSystem(), coordSet);
		// first and last points are identical (north of center)
		Coordinate arcEnd = coordSet.get(0);
		assertEquals("Point north of center has unexpected latitude",
				49.19472 + 0.04496, arcEnd.x, 0.00015);
		assertEquals("Point north of center has unexpected longitude",
				-123.18389, arcEnd.y, 0.00015);
	}

	@Test
	public void convert100NauticalMiles() {
		LengthType length = new LengthType();
		length.setUom("M");
		length.setValue(100.0);
		assertEquals("Unexpected length (meters).", 185200,
				GmlUtils.lengthInMeters(length), 1);
	}

	@Test
	public void convert12km() {
		LengthType length = new LengthType();
		length.setUom("km");
		length.setValue(12.0);
		assertEquals("Unexpected length (meters).", 12000,
				GmlUtils.lengthInMeters(length), 0.5);
	}

	@Test
	public void createPeriodFromGmlTimePeriod() throws SAXException,
			IOException {
		Document gmlPeriod = docBuilder.parse(this.getClass()
				.getResourceAsStream("/gml/temporal/TimePeriod-UTC.xml"));
		TemporalGeometricPrimitive tmPrimitive = GmlUtils
				.gmlToTemporalGeometricPrimitive(gmlPeriod.getDocumentElement());
		assertTrue("Expected object of type " + Period.class.getName(),
				Period.class.isInstance(tmPrimitive));
		Period period = Period.class.cast(tmPrimitive);
		ZonedDateTime endTime = ZonedDateTime.parse("2016-07-10T22:05:39Z",
				DateTimeFormatter.ISO_DATE_TIME);
		assertTrue(Date.from(endTime.toInstant()).equals(
				period.getEnding().getPosition().getDate()));
	}

	@Test
	public void unmarshalPointFromURI() throws JAXBException,
			URISyntaxException {
		URL url = getClass().getResource("/gml/Point.xml");
		AbstractGeometry geom = GmlUtils.unmarshalGMLGeometry(url.toURI());
		assertTrue(Point.class.isInstance(geom));
		Point point = Point.class.cast(geom);
		assertEquals(52.27, point.getPos().getOrdinate(0), 0.005);
	}

	@Test
	public void unmarshalPolygonFromSource() throws SAXException, IOException,
			JAXBException {
		Document doc = docBuilder.parse(this.getClass().getResourceAsStream(
				"/gml/Polygon.xml"));
		AbstractGeometry geom = GmlUtils
				.unmarshalGMLGeometry(new DOMSource(doc));
		assertTrue(org.geotoolkit.gml.xml.Polygon.class.isInstance(geom));
		org.geotoolkit.gml.xml.Polygon polygon = org.geotoolkit.gml.xml.Polygon.class
				.cast(geom);
		assertFalse(polygon.getInterior().isEmpty());
	}
}
