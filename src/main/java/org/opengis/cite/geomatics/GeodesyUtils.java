package org.opengis.cite.geomatics;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.GeodeticCalculator;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;

import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gml.xml.AbstractRing;

import org.opengis.cite.geomatics.gml.CurveCoordinateListFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.coordinate.Position;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import org.locationtech.jts.geom.Coordinate;

/**
 * Provides utility methods for using coordinate reference systems and performing
 * coordinate transformations.
 */
public class GeodesyUtils {

	private static final Logger LOGR = Logger.getLogger(GeodesyUtils.class.getPackage().getName());

	/**
	 * OGC identifier for WGS 84 (geographic 2D)
	 */
	public static final String EPSG_4326 = "urn:ogc:def:crs:EPSG::4326";

	/**
	 * OGC identifier for WGS 84 (longitude-latitude)
	 */
	public static final String OGC_CRS84 = "urn:ogc:def:crs:OGC:1.3:CRS84";

	/**
	 * Returns an immutable envelope representing the valid geographic extent of the CRS
	 * identified by the given URI reference.
	 * @param crsRef An absolute URI that identifies a CRS definition.
	 * @return An ImmutableEnvelope object.
	 * @throws FactoryException if the CRS reference cannot be resolved to a known
	 * definition.
	 */
	public static ImmutableEnvelope getDomainOfValidity(String crsRef) throws FactoryException {
		CoordinateReferenceSystem crs = null;
		if (crsRef.equals(OGC_CRS84)) {
			crs = CommonCRS.defaultGeographic();
		}
		else {
			crs = CRS.forCode(getAbbreviatedCRSIdentifier(crsRef));
		}
		Envelope areaOfUse = CRS.getDomainOfValidity(crs);
		return new ImmutableEnvelope(areaOfUse);
	}

	/**
	 * Returns a well-known identifier (URI) for the given coordinate reference system
	 * using the 'urn' scheme (e.g. "urn:ogc:def:crs:EPSG::4326").
	 *
	 * @see "OGC 09-048r3: Name type specification - definitions - part 1 - basic name"
	 * @param crs A {@link CoordinateReferenceSystem} object.
	 * @return A String representing a URN value in the 'ogc' namespace; if no identifier
	 * can be constructed an empty String is returned.
	 */
	public static String getCRSIdentifier(CoordinateReferenceSystem crs) {
		Set<Identifier> identifiers = crs.getIdentifiers();
		if (identifiers.isEmpty()) {
			if (crs.getName().getCode().startsWith("WGS84")) {
				// see WMS 1.3 (ISO 19128), B.3
				return "urn:ogc:def:crs:OGC:1.3:CRS84";
			}
			else {
				return "";
			}
		}
		StringBuilder crsId = new StringBuilder("urn:ogc:def:crs:");
		Identifier id = identifiers.iterator().next();
		crsId.append(id.getCodeSpace()).append(":");
		// EPSG definitions are not versioned
		if (!id.getCodeSpace().equalsIgnoreCase("EPSG")) {
			crsId.append(id.getVersion());
		}
		crsId.append(':');
		crsId.append(id.getCode());
		return crsId.toString();
	}

	/**
	 * Determines the destination position given the azimuth and distance from some
	 * starting position.
	 * @param startingPos The starting position.
	 * @param azimuth The horizontal angle measured clockwise from a meridian.
	 * @param distance The great-circle (orthodromic) distance in the same units as the
	 * ellipsoid axis (e.g. meters for EPSG 4326).
	 * @return A DirectPosition representing the destination position (in the same CRS as
	 * the starting position).
	 */
	public static DirectPosition calculateDestination(Position startingPos, double azimuth, double distance) {
		CoordinateReferenceSystem crs = startingPos.getDirectPosition().getCoordinateReferenceSystem();
		GeodeticCalculator calculator = GeodeticCalculator.create(crs);
		DirectPosition destPos = null;
		// calculator only accepts azimuth values in range +- 180
		if (azimuth > 180) {
			azimuth = azimuth - 360;
		}
		else if (azimuth < -180) {
			azimuth = azimuth + 360;
		}
		try {
			calculator.setStartPoint(startingPos);
			calculator.setStartingAzimuth(azimuth);
			calculator.setGeodesicDistance(distance);
			destPos = calculator.getEndPoint();
		}
		catch (IllegalArgumentException te) {
			// Same CRS so this should never arise
			LOGR.fine(te.getMessage());
		}
		return destPos;
	}

	/**
	 * Transforms the given GML ring to a right-handed coordinate system (if it does not
	 * already use one) and returns the resulting coordinate sequence. Many computational
	 * geometry algorithms assume right-handed coordinates. In some cases this can be
	 * achieved simply by changing the axis order; for example, from (lat,lon) to
	 * (lon,lat).
	 * @param gmlRing A representation of a GML ring (simple closed curve).
	 * @return A Coordinate[] array, or {@code null} if the original CRS could not be
	 * identified.
	 */
	public static Coordinate[] transformRingToRightHandedCS(AbstractRing gmlRing) {
		String srsName = gmlRing.getSrsName();
		if (null == srsName || srsName.isEmpty()) {
			return null;
		}
		CurveCoordinateListFactory curveCoordFactory = new CurveCoordinateListFactory();
		List<Coordinate> curveCoords = curveCoordFactory.createCoordinateList(gmlRing);
		MathTransform crsTransform;
		try {
			CoordinateReferenceSystem sourceCRS = CRS.forCode(getAbbreviatedCRSIdentifier(srsName));
			CoordinateReferenceSystem targetCRS = CRS.forCode(getAbbreviatedCRSIdentifier(srsName));
			targetCRS = AbstractCRS.castOrCopy(targetCRS).forConvention(AxesConvention.RIGHT_HANDED);
			crsTransform = CRS.findOperation(sourceCRS, targetCRS, null).getMathTransform();
		}
		catch (FactoryException fx) {
			throw new RuntimeException("Failed to create coordinate transformer.", fx);
		}
		for (Coordinate coord : curveCoords) {
			try {
				JTS.transform(coord, coord, crsTransform);
			}
			catch (TransformException tx) {
				throw new RuntimeException("Failed to transform coordinate: " + coord, tx);
			}
		}
		removeConsecutiveDuplicates(curveCoords, 1);
		return curveCoords.toArray(new Coordinate[curveCoords.size()]);
	}

	/**
	 * Transforms the given GML ring to a right-handed coordinate system (if it does not
	 * already use one) and returns the resulting coordinate sequence. Many computational
	 * geometry algorithms assume right-handed coordinates. In some cases this can be
	 * achieved simply by changing the axis order; for example, from (lat,lon) to
	 * (lon,lat).
	 * @param gmlRing A representation of a GML ring (simple closed curve).
	 * @return A Coordinate[] array, or {@code null} if the original CRS could not be
	 * identified.
	 */
	public static Coordinate[] transformRingToRightHandedCSKeepAllCoords(AbstractRing gmlRing) {
		String srsName = gmlRing.getSrsName();
		if (null == srsName || srsName.isEmpty()) {
			return null;
		}
		CurveCoordinateListFactory curveCoordFactory = new CurveCoordinateListFactory();
		List<Coordinate> curveCoords = curveCoordFactory.createCoordinateList(gmlRing);
		MathTransform crsTransform;
		try {
			CoordinateReferenceSystem sourceCRS = CRS.forCode(getAbbreviatedCRSIdentifier(srsName));
			CoordinateReferenceSystem targetCRS = CRS.forCode(getAbbreviatedCRSIdentifier(srsName));
			targetCRS = AbstractCRS.castOrCopy(targetCRS).forConvention(AxesConvention.RIGHT_HANDED);
			crsTransform = CRS.findOperation(sourceCRS, targetCRS, null).getMathTransform();
		}
		catch (FactoryException fx) {
			throw new RuntimeException("Failed to create coordinate transformer.", fx);
		}
		for (Coordinate coord : curveCoords) {
			try {
				JTS.transform(coord, coord, crsTransform);
			}
			catch (TransformException tx) {
				throw new RuntimeException("Failed to transform coordinate: " + coord, tx);
			}
		}
		return curveCoords.toArray(new Coordinate[curveCoords.size()]);
	}

	/**
	 * Returns an abbreviated identifier for the given CRS reference. The result contains
	 * the code space (authority) and code value extracted from the URI reference.
	 * @param srsName An absolute URI ('http' or 'urn' scheme) that identifies a CRS in
	 * accord with OGC 09-048r3.
	 * @return A String of the form "{@code authority:code}".
	 *
	 * @see <a target="_blank" href=
	 * "http://portal.opengeospatial.org/files/?artifact_id=37802">OGC 09-048r3, <em>Name
	 * type specification - definitions - part 1 - basic name</em></a>
	 */
	public static String getAbbreviatedCRSIdentifier(String srsName) {
		StringBuilder crsId = new StringBuilder();
		int crsIndex = srsName.indexOf("crs");
		String separator;
		if (srsName.startsWith("http://www.opengis.net")) {
			separator = "/";
		}
		else if (srsName.startsWith("urn:ogc")) {
			separator = ":";
		}
		else {
			throw new IllegalArgumentException("Invalid CRS reference (see OGC 09-048r3): " + srsName);
		}
		String[] parts = srsName.substring(crsIndex + 4).split(separator);
		if (parts.length == 3) {
			crsId.append(parts[0]).append(':').append(parts[2]);
		}
		return crsId.toString();
	}

	/**
	 * Converts an srsName identifier to the corresponding URN value if it is an 'http'
	 * URI. The Geotk 3.x library does not recognize CRS identifiers based on the 'http'
	 * schreme.
	 * @param srsName An absolute URI that identifies a CRS in accord with OGC 09-048r3.
	 * @return A URN-based identifier (the given value is unchanged if it is not an 'http'
	 * URI).
	 *
	 * @see <a target="_blank" href=
	 * "http://portal.opengeospatial.org/files/?artifact_id=37802">OGC 09-048r3, <em>Name
	 * type specification - definitions - part 1 - basic name</em></a>
	 */
	public static String convertSRSNameToURN(String srsName) {
		if (!srsName.startsWith("http")) {
			return srsName;
		}
		StringBuilder urn = new StringBuilder("urn:ogc:def:crs:");
		String[] srsNameParts = srsName.split("/");
		int numParts = srsNameParts.length;
		// authority code
		urn.append(srsNameParts[numParts - 3]).append(':');
		// version (may be empty)
		String ver = srsNameParts[numParts - 2];
		if (!(ver.isEmpty() || ver.equals("0"))) {
			urn.append(ver);
		}
		// CRS code
		urn.append(':').append(srsNameParts[numParts - 1]);
		return urn.toString();
	}

	/**
	 * Checks a coordinate list for consecutive duplicate positions and removes them. That
	 * is, P(n+1) is removed if it represents the same location as P(n) within the
	 * specified tolerance, <strong>unless</strong> it is the last point in the list in
	 * which case P(n) is removed instead (the last point may coincide with the first in
	 * order to form a cycle). The third dimension is ignored.
	 * @param coordList A list of Coordinate objects.
	 * @param tolerancePPM The tolerance for comparing coordinates, in parts per million
	 * (ppm).
	 */
	public static void removeConsecutiveDuplicates(List<Coordinate> coordList, double tolerancePPM) {
		if (coordList.size() < 2)
			return;
		double tolerance = tolerancePPM * 1E-06;
		ListIterator<Coordinate> itr = coordList.listIterator();
		Coordinate coord = itr.next();
		while (itr.hasNext()) {
			Coordinate nextCoord = itr.next();
			double xDelta = Math.abs((nextCoord.x / coord.x) - 1.0);
			double yDelta = Math.abs((nextCoord.y / coord.y) - 1.0);
			if ((xDelta <= tolerance) && (yDelta <= tolerance)) {
				if (!itr.hasNext()) {
					// remove next to last item
					coordList.remove(coordList.size() - 2);
					break;
				}
				itr.remove();
				continue;
			}
			coord = nextCoord;
		}
	}

}
