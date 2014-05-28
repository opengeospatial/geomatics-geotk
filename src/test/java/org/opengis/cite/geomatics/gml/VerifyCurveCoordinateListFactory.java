package org.opengis.cite.geomatics.gml;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.geotoolkit.gml.xml.AbstractRing;
import org.geotoolkit.gml.xml.LineString;
import org.geotoolkit.gml.xml.v321.CompositeCurveType;
import org.geotoolkit.gml.xml.v321.CurveType;
import org.geotoolkit.gml.xml.v321.OrientableCurveType;
import org.geotoolkit.gml.xml.v321.PolygonType;
import org.geotoolkit.xml.MarshallerPool;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.cite.geomatics.gml.CurveCoordinateListFactory;
import org.opengis.cite.geomatics.gml.GmlUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class VerifyCurveCoordinateListFactory {

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
    public void curveWithArcByCenterPoint() throws JAXBException {
        URL url = this.getClass()
                .getResource("/gml/Curve-ArcByCenterPoint.xml");
        JAXBElement<CurveType> result = (JAXBElement<CurveType>) gmlUnmarshaller
                .unmarshal(url);
        CurveType curve = result.getValue();
        CurveCoordinateListFactory iut = new CurveCoordinateListFactory();
        List<Coordinate> coordSet = iut.getCoordinateList(curve);
        assertEquals("Unexpected number of points on curve.",
                GmlUtils.TOTAL_ARC_POINTS, coordSet.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void lineString() throws JAXBException {
        URL url = this.getClass().getResource("/gml/LineString.xml");
        JAXBElement<LineString> result = (JAXBElement<LineString>) gmlUnmarshaller
                .unmarshal(url);
        LineString line = result.getValue();
        CurveCoordinateListFactory iut = new CurveCoordinateListFactory();
        List<Coordinate> coordList = iut.getCoordinateList(line);
        assertEquals("Unexpected number of vertices.", 3, coordList.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void compositeCurve() throws JAXBException {
        URL url = this.getClass().getResource("/gml/CompositeCurve.xml");
        JAXBElement<CompositeCurveType> result = (JAXBElement<CompositeCurveType>) gmlUnmarshaller
                .unmarshal(url);
        CompositeCurveType curve = result.getValue();
        CurveCoordinateListFactory iut = new CurveCoordinateListFactory();
        List<Coordinate> coordList = iut.getCoordinateList(curve);
        assertEquals("Unexpected number of points on curve.", 8,
                coordList.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void orientableCurve() throws JAXBException {
        URL url = this.getClass().getResource("/gml/OrientableCurve.xml");
        JAXBElement<OrientableCurveType> result = (JAXBElement<OrientableCurveType>) gmlUnmarshaller
                .unmarshal(url);
        OrientableCurveType curve = result.getValue();
        CurveCoordinateListFactory iut = new CurveCoordinateListFactory();
        List<Coordinate> coordList = iut.getCoordinateList(curve);
        assertEquals("Unexpected number of points on curve.", 5,
                coordList.size());
        Coordinate firstCoord = new Coordinate(49.361246, -123.183889);
        assertEquals("Unexpected first coordinate.", firstCoord,
                coordList.get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void tripartiteCurve() throws JAXBException {
        URL url = this.getClass().getResource("/gml/Curve-tripartite.xml");
        JAXBElement<CurveType> result = (JAXBElement<CurveType>) gmlUnmarshaller
                .unmarshal(url);
        CurveType curve = result.getValue();
        CurveCoordinateListFactory iut = new CurveCoordinateListFactory();
        List<Coordinate> coordSet = iut.getCoordinateList(curve);
        assertEquals("Unexpected number of points on curve.", 9,
                coordSet.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void exteriorBoundaryOfPolygon() throws JAXBException {
        URL url = this.getClass().getResource("/gml/Polygon.xml");
        JAXBElement<PolygonType> result = (JAXBElement<PolygonType>) gmlUnmarshaller
                .unmarshal(url);
        PolygonType polygon = result.getValue();
        AbstractRing exterior = polygon.getExterior().getAbstractRing();
        CurveCoordinateListFactory iut = new CurveCoordinateListFactory();
        List<Coordinate> coordSet = iut.createCoordinateList(exterior);
        assertEquals("Unexpected number of points on exterior ring.", 42,
                coordSet.size());
    }
}
