package org.opengis.cite.geomatics.gml;

import static org.junit.Assert.*;

import java.net.URL;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import org.geotoolkit.gml.xml.v321.CurveType;
import org.apache.sis.xml.MarshallerPool;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.cite.geomatics.gml.GeometryCoordinateList;

import org.locationtech.jts.geom.Coordinate;

public class VerifyGeometryCoordinateList {

    private static Unmarshaller gmlUnmarshaller;

    @BeforeClass
    public static void initFixture() throws Exception {
        MarshallerPool pool = org.geotoolkit.gml.xml.GMLMarshallerPool.getInstance();
        gmlUnmarshaller = pool.acquireUnmarshaller();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    @SuppressWarnings("unchecked")
    public void createPointSetForCurveWithLineString() throws JAXBException {
        URL url = this.getClass().getResource("/gml/Curve-LineString.xml");
        JAXBElement<CurveType> result = (JAXBElement<CurveType>) gmlUnmarshaller
                .unmarshal(url);
        CurveType curve = result.getValue();
        GeometryCoordinateList iut = new GeometryCoordinateList();
        Coordinate[] coords = iut.getCoordinateList(curve);
        assertEquals("Unexpected number of coordinates.", 5, coords.length);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void createPointSetForCurveWithArc() throws JAXBException {
        URL url = this.getClass().getResource("/gml/Curve-Arc.xml");
        JAXBElement<CurveType> result = (JAXBElement<CurveType>) gmlUnmarshaller
                .unmarshal(url);
        CurveType curve = result.getValue();
        GeometryCoordinateList iut = new GeometryCoordinateList();
        Coordinate[] coords = iut.getCoordinateList(curve);
        assertEquals("Unexpected number of control points.", 3, coords.length);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void createPointSetForCurveWithCircle() throws JAXBException {
        URL url = this.getClass().getResource("/gml/Curve-Circle.xml");
        JAXBElement<CurveType> result = (JAXBElement<CurveType>) gmlUnmarshaller
                .unmarshal(url);
        CurveType curve = result.getValue();
        GeometryCoordinateList iut = new GeometryCoordinateList();
        Coordinate[] coords = iut.getCoordinateList(curve);
        assertEquals("Unexpected number of control points.", 3, coords.length);
    }
}
