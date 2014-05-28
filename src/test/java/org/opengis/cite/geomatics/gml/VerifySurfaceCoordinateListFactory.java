package org.opengis.cite.geomatics.gml;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.geotoolkit.gml.xml.v321.PolygonType;
import org.geotoolkit.gml.xml.v321.SurfaceType;
import org.geotoolkit.xml.MarshallerPool;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;

public class VerifySurfaceCoordinateListFactory {

    private static DocumentBuilder docBuilder;
    private static Unmarshaller gmlUnmarshaller;

    @BeforeClass
    public static void initFixture() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();
        MarshallerPool pool = new MarshallerPool("org.geotoolkit.gml.xml.v321");
        gmlUnmarshaller = pool.acquireUnmarshaller();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void exteriorBoundaryOfPolygon() throws JAXBException {
        URL url = this.getClass().getResource("/gml/Polygon.xml");
        @SuppressWarnings("unchecked")
        JAXBElement<PolygonType> result = (JAXBElement<PolygonType>) gmlUnmarshaller
                .unmarshal(url);
        PolygonType polygon = result.getValue();
        SurfaceCoordinateListFactory iut = new SurfaceCoordinateListFactory();
        List<Coordinate> coordSet = iut.createCoordinateList(polygon);
        assertEquals("Unexpected number of points on exterior boundary.", 42,
                coordSet.size());
    }

    @Test
    public void interiorBoundaryOfPolygon() throws JAXBException {
        URL url = this.getClass().getResource("/gml/Polygon.xml");
        @SuppressWarnings("unchecked")
        JAXBElement<PolygonType> result = (JAXBElement<PolygonType>) gmlUnmarshaller
                .unmarshal(url);
        PolygonType polygon = result.getValue();
        SurfaceCoordinateListFactory iut = new SurfaceCoordinateListFactory();
        Set<List<Coordinate>> coordSet = iut
                .interiorBoundariesOfPolygon(polygon);
        assertFalse("Set is empty.", coordSet.isEmpty());
        List<Coordinate> interiorCoords = coordSet.iterator().next();
        assertEquals("Unexpected number of points on interior boundary.", 9,
                interiorCoords.size());
    }

    @Test
    public void exteriorBoundaryOfSurfaceWithPolygonPatch()
            throws JAXBException {
        URL url = this.getClass()
                .getResource("/gml/Surface-PolygonPatch-1.xml");
        @SuppressWarnings("unchecked")
        JAXBElement<SurfaceType> result = (JAXBElement<SurfaceType>) gmlUnmarshaller
                .unmarshal(url);
        SurfaceType surface = result.getValue();
        SurfaceCoordinateListFactory iut = new SurfaceCoordinateListFactory();
        List<Coordinate> coordSet = iut.createCoordinateList(surface);
        assertEquals("Unexpected number of points on exterior boundary.", 42,
                coordSet.size());
    }

    @Test
    public void interiorBoundaryOfSurface() throws JAXBException {
        URL url = this.getClass()
                .getResource("/gml/Surface-PolygonPatch-1.xml");
        @SuppressWarnings("unchecked")
        JAXBElement<SurfaceType> result = (JAXBElement<SurfaceType>) gmlUnmarshaller
                .unmarshal(url);
        SurfaceType surface = result.getValue();
        SurfaceCoordinateListFactory iut = new SurfaceCoordinateListFactory();
        Set<List<Coordinate>> coordSet = iut.interiorCoordinatesSet(surface);
        assertFalse("Set is empty.", coordSet.isEmpty());
        List<Coordinate> interiorCoords = coordSet.iterator().next();
        assertEquals("Unexpected number of points on interior boundary.", 9,
                interiorCoords.size());
    }

    @Test
    public void exteriorBoundaryOfSurfaceWithTwoPatches() throws JAXBException {
        URL url = this.getClass()
                .getResource("/gml/Surface-PolygonPatch-2.xml");
        @SuppressWarnings("unchecked")
        JAXBElement<SurfaceType> result = (JAXBElement<SurfaceType>) gmlUnmarshaller
                .unmarshal(url);
        SurfaceType surface = result.getValue();
        SurfaceCoordinateListFactory iut = new SurfaceCoordinateListFactory();
        List<Coordinate> coordSet = iut.createCoordinateList(surface);
        assertEquals("Unexpected number of points on exterior boundary.", 6,
                coordSet.size());
    }

    @Test
    public void exteriorBoundaryOfSurfaceIsTripartiteCurve()
            throws SAXException, IOException {
        Document doc = docBuilder.parse(this.getClass().getResourceAsStream(
                "/gml/Surface-PolygonPatch-3.xml"));
        SurfaceCoordinateListFactory iut = new SurfaceCoordinateListFactory();
        List<Coordinate> coordSet = iut.createCoordinateList(doc
                .getDocumentElement());
        assertEquals("Unexpected number of points on exterior boundary.", 9,
                coordSet.size());
    }

    @Test
    public void exteriorBoundaryOfAIXMSurface() throws JAXBException,
            SAXException, IOException {
        Document doc = docBuilder.parse(this.getClass().getResourceAsStream(
                "/gml/AIXMSurface.xml"));
        SurfaceCoordinateListFactory iut = new SurfaceCoordinateListFactory();
        List<Coordinate> coordSet = iut.createCoordinateList(doc
                .getDocumentElement());
        assertEquals("Unexpected number of points on exterior boundary.", 10,
                coordSet.size());
    }

    @Test
    public void interiorBoundaryOfAIXMSurface() throws JAXBException,
            SAXException, IOException {
        Document doc = docBuilder.parse(this.getClass().getResourceAsStream(
                "/gml/AIXMSurface-2.xml"));
        SurfaceCoordinateListFactory iut = new SurfaceCoordinateListFactory();
        Set<List<Coordinate>> coordSet = iut.interiorCoordinatesSet(doc
                .getDocumentElement());
        assertFalse("Set is empty.", coordSet.isEmpty());
        List<Coordinate> interiorCoords = coordSet.iterator().next();
        assertEquals("Unexpected number of points on interior boundary.", 4,
                interiorCoords.size());
    }

}
