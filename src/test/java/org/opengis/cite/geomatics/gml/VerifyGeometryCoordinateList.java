package org.opengis.cite.geomatics.gml;

import static org.junit.Assert.*;

import java.net.URL;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.geotoolkit.gml.xml.v321.CurveType;
import org.geotoolkit.xml.MarshallerPool;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.cite.geomatics.gml.GeometryCoordinateList;

import com.vividsolutions.jts.geom.Coordinate;

public class VerifyGeometryCoordinateList {

    private static Unmarshaller gmlUnmarshaller;

    @BeforeClass
    public static void initFixture() throws Exception {
        MarshallerPool pool = new MarshallerPool("org.geotoolkit.gml.xml.v321");
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
