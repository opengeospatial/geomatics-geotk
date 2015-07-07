package org.opengis.cite.geomatics;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.geotoolkit.geometry.GeneralDirectPosition;
import org.geotoolkit.geometry.ImmutableEnvelope;
import org.geotoolkit.gml.xml.AbstractRing;
import org.geotoolkit.gml.xml.v321.PolygonPatchType;
import org.geotoolkit.gml.xml.v321.PolygonType;
import org.geotoolkit.gml.xml.v321.SurfaceType;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.xml.MarshallerPool;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

import com.vividsolutions.jts.geom.Coordinate;

public class VerifyGeodesyUtils {

	private static Unmarshaller gmlUnmarshaller;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@BeforeClass
	public static void initBasicFixture() throws ParserConfigurationException,
			JAXBException {
		MarshallerPool pool = new MarshallerPool("org.geotoolkit.gml.xml.v321");
		gmlUnmarshaller = pool.acquireUnmarshaller();
	}

	@Test
	public void getExtentOfCRS_epsg4326() throws FactoryException {
		String urn = "urn:ogc:def:crs:EPSG::4326";
		ImmutableEnvelope envelope = GeodesyUtils.getDomainOfValidity(urn);
		assertEquals("Envelope has unexpected dimension", 2,
				envelope.getDimension());
		DirectPosition lowerCorner = envelope.getLowerCorner();
		assertEquals("Unexpected value for latitude of lower corner.", -90,
				lowerCorner.getOrdinate(0), 0.01);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getExtentOfCRS_invalidURI() throws FactoryException {
		String crsRef = "epsg-4326";
		ImmutableEnvelope envelope = GeodesyUtils.getDomainOfValidity(crsRef);
		assertNull(envelope);
	}

	@Test(expected = NoSuchAuthorityCodeException.class)
	public void getExtentOfCRS_unknownEPSGCode() throws FactoryException {
		String crsRef = "http://www.opengis.net/def/crs/EPSG/0/999999";
		ImmutableEnvelope envelope = GeodesyUtils.getDomainOfValidity(crsRef);
		assertNull(envelope);
	}

	@Test
	public void getCRSIdentifier_epsg4326() throws FactoryException {
		CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
		assertNotNull(crs);
		String crsId = GeodesyUtils.getCRSIdentifier(crs);
		assertFalse("No identifier found.", crsId.isEmpty());
		assertEquals("Unexpected CRS identifier.",
				"urn:ogc:def:crs:EPSG::4326", crsId);
	}

	@Test
	public void calculateDestinationNorthFromYVR()
			throws NoSuchAuthorityCodeException, FactoryException {
		GeneralDirectPosition yvrPos = new GeneralDirectPosition(
				CRS.decode("EPSG:4326"));
		yvrPos.setLocation(new double[] { 49.194722, -123.183889 });
		// north 1852 m (1 NM) ~ 1' (0.016667 deg)
		DirectPosition destPos = GeodesyUtils.calculateDestination(yvrPos, 0.0,
				1852.0);
		// tolerance ~ 10m
		assertEquals("Unexpected latitude.", 49.194722 + 0.016667,
				destPos.getOrdinate(0), 0.0001);
		assertEquals("Unexpected longitude.", -123.183889,
				destPos.getOrdinate(1), 0.00015);
	}

	@Test
	public void calculateDestinationEastFromYVR()
			throws NoSuchAuthorityCodeException, FactoryException {
		GeneralDirectPosition yvrPos = new GeneralDirectPosition(
				CRS.decode("EPSG:4326"));
		yvrPos.setLocation(new double[] { 49.194722, -123.183889 });
		// east 1852 m (1 NM) ~ 0.025310 deg lon at 49 deg lat
		DirectPosition destPos = GeodesyUtils.calculateDestination(yvrPos,
				90.0, 1852.0);
		// tolerance ~ 10m
		assertEquals("Unexpected latitude.", 49.194722, destPos.getOrdinate(0),
				0.0001);
		assertEquals("Unexpected longitude.", -123.183889 + 0.025310,
				destPos.getOrdinate(1), 0.00015);
	}

	@Test
	public void calculateDestinationWestFromYVR()
			throws NoSuchAuthorityCodeException, FactoryException {
		GeneralDirectPosition yvrPos = new GeneralDirectPosition(
				CRS.decode("EPSG:4326"));
		yvrPos.setLocation(new double[] { 49.194722, -123.183889 });
		// east 1852 m (1 NM) ~ 0.025310 deg lon at 49 deg lat
		DirectPosition destPos = GeodesyUtils.calculateDestination(yvrPos,
				270.0, 1852.0);
		// tolerance ~ 10m
		assertEquals("Unexpected latitude.", 49.194722, destPos.getOrdinate(0),
				0.0001);
		assertEquals("Unexpected longitude.", -123.183889 - 0.025310,
				destPos.getOrdinate(1), 0.00015);
	}

	@Test
	public void transformRingToRightHandedCS_LinearRing() throws JAXBException {
		URL url = this.getClass().getResource(
				"/gml/Polygon-InteriorLinearRing.xml");
		@SuppressWarnings("unchecked")
		JAXBElement<PolygonType> polygon = (JAXBElement<PolygonType>) gmlUnmarshaller
				.unmarshal(url);
		AbstractRing exterior = polygon.getValue().getExterior()
				.getAbstractRing();
		exterior.setSrsName(polygon.getValue().getSrsName());
		Coordinate[] coords = GeodesyUtils
				.transformRingToRightHandedCS(exterior);
		assertNotNull("Coordinate sequence is null", coords);
		assertEquals("Coordinate sequence has unexpected length.", 6,
				coords.length);
		assertEquals("First coordinate has unexpected x value.", -123.1839,
				coords[0].x, 0.0001);
	}

	@Test
	public void transformRingToRightHandedCS_TripartiteCurve()
			throws JAXBException {
		URL url = this.getClass()
				.getResource("/gml/Surface-PolygonPatch-3.xml");
		@SuppressWarnings("unchecked")
		JAXBElement<SurfaceType> surface = (JAXBElement<SurfaceType>) gmlUnmarshaller
				.unmarshal(url);
		PolygonPatchType patch = (PolygonPatchType) surface.getValue()
				.getPatches().getValue().getAbstractSurfacePatch().get(0)
				.getValue();
		AbstractRing exterior = patch.getExterior().getAbstractRing();
		exterior.setSrsName(surface.getValue().getSrsName());
		Coordinate[] coords = GeodesyUtils
				.transformRingToRightHandedCS(exterior);
		assertNotNull("Coordinate sequence is null", coords);
		// first/end point + 5 points on arc
		assertEquals("Coordinate sequence has unexpected length.", 7,
				coords.length);
		assertEquals("First coordinate has unexpected x value.", -36.1667,
				coords[0].x, 0.0001);
	}

	@Test
	public void removeConsecutiveDuplicates_1ppm() {
		List<Coordinate> coords = new ArrayList<Coordinate>();
		coords.add(new Coordinate(55.233333, -36.166667));
		coords.add(new Coordinate(55.231164, -36.894373));
		coords.add(new Coordinate(55.23116339, -36.89437371));
		GeodesyUtils.removeConsecutiveDuplicates(coords, 1);
		assertEquals("Coordinate list has unexpected length.", 2, coords.size());
	}

	@Test
	public void removeConsecutiveDuplicates_noDuplicatesInRing() {
		List<Coordinate> coords = new ArrayList<Coordinate>();
		coords.add(new Coordinate(55.233333, -36.166667));
		coords.add(new Coordinate(55.231164, -36.894373));
		coords.add(new Coordinate(54.928164, -35.674116));
		coords.add(new Coordinate(55.233333, -36.166667));
		GeodesyUtils.removeConsecutiveDuplicates(coords, 1);
		assertEquals("Coordinate list has unexpected length.", 4, coords.size());
	}

	@Test
	public void convertEPSGSrsNameToURN() {
		String urn = GeodesyUtils
				.convertSRSNameToURN("http://www.opengis.net/def/crs/EPSG/0/4326");
		assertEquals("Unexpected CRS identifier", "urn:ogc:def:crs:EPSG::4326",
				urn);
	}
}
