package org.opengis.cite.geomatics;

import org.geotoolkit.geometry.GeneralEnvelope;
import org.geotoolkit.referencing.CRS;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

public class VerifySpatialAssert {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void envelopesIntersect_sameCRS() throws FactoryException {
		CoordinateReferenceSystem epsg4326 = CRS.decode("EPSG:4326");
		GeneralEnvelope e1 = new GeneralEnvelope(epsg4326);
		e1.setEnvelope(new double[] { 32.0, -117.6, 33.5, -116.2 });
		GeneralEnvelope e2 = new GeneralEnvelope(epsg4326);
		e2.setEnvelope(new double[] { 31.0, -118.0, 34.0, -116.0 });
		SpatialAssert.assertIntersects(e1, e2);
	}

	@Test
	public void envelopesIntersect_differentCRS() throws FactoryException {
		GeneralEnvelope e1 = new GeneralEnvelope(CRS.decode("EPSG:4326"));
		e1.setEnvelope(new double[] { 49.25, -123.1, 50.0, -122.5 });
		GeneralEnvelope e2 = new GeneralEnvelope(CRS.decode("EPSG:32610"));
		e2.setEnvelope(new double[] { 490571, 5428426, 515131, 5459036 });
		SpatialAssert.assertIntersects(e1, e2);
	}

	@Test
	public void envelopesDoNotIntersect_sameCRS() throws FactoryException {
		thrown.expect(AssertionError.class);
		thrown.expectMessage("The envelopes do not intersect");
		CoordinateReferenceSystem epsg4326 = CRS.decode("EPSG:4326");
		GeneralEnvelope e1 = new GeneralEnvelope(epsg4326);
		e1.setEnvelope(new double[] { 32.0, -117.6, 33.5, -116.2 });
		GeneralEnvelope e2 = new GeneralEnvelope(epsg4326);
		e2.setEnvelope(new double[] { 49.25, -123.1, 50.0, -122.5 });
		SpatialAssert.assertIntersects(e1, e2);
	}

}
