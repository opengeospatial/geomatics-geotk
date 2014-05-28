package org.opengis.cite.geomatics;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Logger;

import org.geotoolkit.geometry.Envelopes;
import org.geotoolkit.geometry.ImmutableEnvelope;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gml.xml.AbstractRing;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.referencing.GeodeticCalculator;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.opengis.cite.geomatics.gml.CurveCoordinateListFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.coordinate.Position;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Provides utility methods for using coordinate reference systems and
 * performing coordinate transformations.
 */
public class GeodesyUtils {

    private static final Logger LOGR = Logger.getLogger(GeodesyUtils.class
            .getPackage().getName());

    /**
     * OGC identifier for WGS 84 (geographic 2D)
     */
    public static final String EPSG_4326 = "urn:ogc:def:crs:EPSG::4326";
    /**
     * OGC identifier for WGS 84 (longitude-latitude)
     */
    public static final String OGC_CRS84 = "urn:ogc:def:crs:OGC:1.3:CRS84";

    /**
     * Returns an immutable envelope representing the valid geographic extent of
     * the CRS identified by the given URI reference.
     * 
     * @param crsRef
     *            An absolute URI that identifies a CRS definition.
     * @return An ImmutableEnvelope object.
     * @throws FactoryException
     *             if the CRS reference cannot be resolved to a known
     *             definition.
     */
    public static ImmutableEnvelope getDomainOfValidity(String crsRef)
            throws FactoryException {
        CoordinateReferenceSystem crs = null;
        if (crsRef.equals(OGC_CRS84)) {
            crs = DefaultGeographicCRS.WGS84;
        } else {
            crs = CRS.decode(getAbbreviatedCRSIdentifier(crsRef));
        }
        Envelope areaOfUse = Envelopes.getDomainOfValidity(crs);
        return new ImmutableEnvelope(areaOfUse);
    }

    /**
     * Returns a standard OGC identifier for the given coordinate reference
     * system (e.g. "urn:ogc:def:crs:EPSG::4326").
     * 
     * @see "OGC 09-048r3: Name type specification - definitions - part 1 - basic name"
     * 
     * @param crs
     *            A {@link CoordinateReferenceSystem} object.
     * @return A String representing a URN value in the 'ogc' namespace; if no
     *         identifier is found an empty String is returned.
     */
    public static String getCRSIdentifier(CoordinateReferenceSystem crs) {
        StringBuilder crsId = new StringBuilder("urn:ogc:def:crs:");
        Set<ReferenceIdentifier> identifiers = crs.getIdentifiers();
        if (identifiers.isEmpty()) {
            return "";
        }
        ReferenceIdentifier id = identifiers.iterator().next();
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
     * Determines the destination position given the azimuth and distance from
     * some starting position.
     * 
     * @param startingPos
     *            The starting position.
     * @param azimuth
     *            The horizontal angle measured clockwise from a meridian.
     * @param distance
     *            The great-circle (orthodromic) distance in the same units as
     *            the ellipsoid axis (e.g. meters for EPSG 4326).
     * @return A DirectPosition representing the destination position (in the
     *         same CRS as the starting position).
     */
    public static DirectPosition calculateDestination(Position startingPos,
            double azimuth, double distance) {
        CoordinateReferenceSystem crs = startingPos.getDirectPosition()
                .getCoordinateReferenceSystem();
        GeodeticCalculator calculator = new GeodeticCalculator(crs);
        DirectPosition destPos = null;
        // calculator only accepts azimuth values in range +- 180
        if (azimuth > 180) {
            azimuth = azimuth - 360;
        } else if (azimuth < -180) {
            azimuth = azimuth + 360;
        }
        try {
            calculator.setStartingPosition(startingPos);
            calculator.setDirection(azimuth, distance);
            destPos = calculator.getDestinationPosition();
        } catch (TransformException te) {
            // Same CRS so this should never arise
            LOGR.fine(te.getMessage());
        }
        return destPos;
    }

    /**
     * Transforms the given GML ring to a right-handed coordinate system (if it
     * does not already use one) and returns the resulting coordinate sequence.
     * Many computational geometry algorithms assume right-handed coordinates.
     * In some cases this can be achieved simply by changing the axis order; for
     * example, from (lat,lon) to (lon,lat).
     * 
     * @param gmlRing
     *            A representation of a GML ring (simple closed curve).
     * @return A Coordinate[] array, or {@code null} if the original CRS could
     *         not be identified.
     */
    public static Coordinate[] transformRingToRightHandedCS(AbstractRing gmlRing) {
        String srsName = gmlRing.getSrsName();
        if (null == srsName || srsName.isEmpty()) {
            return null;
        }
        CurveCoordinateListFactory curveCoordFactory = new CurveCoordinateListFactory();
        List<Coordinate> curveCoords = curveCoordFactory
                .createCoordinateList(gmlRing);
        MathTransform crsTransform;
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode(srsName);
            CoordinateReferenceSystem targetCRS = CRS.decode(
                    getAbbreviatedCRSIdentifier(srsName), true);
            crsTransform = CRS.findMathTransform(sourceCRS, targetCRS);
        } catch (FactoryException fx) {
            throw new RuntimeException(
                    "Failed to create coordinate transformer.", fx);
        }
        for (Coordinate coord : curveCoords) {
            try {
                JTS.transform(coord, coord, crsTransform);
            } catch (TransformException tx) {
                throw new RuntimeException("Failed to transform coordinate: "
                        + coord, tx);
            }
        }
        removeConsecutiveDuplicates(curveCoords, 1);
        return curveCoords.toArray(new Coordinate[curveCoords.size()]);
    }

    /**
     * Returns an abbreviated identifier for the given CRS reference. The result
     * contains the code space (authority) and code value extracted from the URI
     * reference.
     * 
     * @param srsName
     *            An absolute URI ('http' or 'urn' scheme) that identifies a CRS
     *            in accord with OGC 09-048r3.
     * @return A String of the form "{@code authority:code}".
     * 
     * @see <a target="_blank"
     *      href="http://portal.opengeospatial.org/files/?artifact_id=37802">OGC
     *      09-048r3, <em>Name type specification - definitions - part 1 - basic
     *      name</em></a>
     */
    public static String getAbbreviatedCRSIdentifier(String srsName) {
        StringBuilder crsId = new StringBuilder();
        int crsIndex = srsName.indexOf("crs");
        String separator;
        if (srsName.startsWith("http://www.opengis.net")) {
            separator = "/";
        } else if (srsName.startsWith("urn:ogc")) {
            separator = ":";
        } else {
            throw new IllegalArgumentException(
                    "Invalid CRS reference (see OGC 09-048r3): " + srsName);
        }
        String[] parts = srsName.substring(crsIndex + 4).split(separator);
        if (parts.length == 3) {
            crsId.append(parts[0]).append(':').append(parts[2]);
        }
        return crsId.toString();
    }

    /**
     * Checks a coordinate list for consecutive duplicate positions and removes
     * them. That is, P(n+1) is removed if it represents the same location as
     * P(n) within the specified tolerance. The third dimension is ignored.
     * 
     * @param coordList
     *            A list of Coordinate objects.
     * @param tolerancePPM
     *            The tolerance for comparing coordinates, in parts per million
     *            (ppm).
     */
    public static void removeConsecutiveDuplicates(List<Coordinate> coordList,
            int tolerancePPM) {
        if (coordList.size() < 2)
            return;
        double tolerance = tolerancePPM * 1E-06;
        ListIterator<Coordinate> itr = coordList.listIterator();
        Coordinate prevCoord = itr.next();
        while (itr.hasNext()) {
            Coordinate coord = itr.next();
            double xDelta = Math.abs((coord.x / prevCoord.x) - 1.0);
            double yDelta = Math.abs((coord.y / prevCoord.y) - 1.0);
            if ((xDelta <= tolerance) && (yDelta <= tolerance)) {
                itr.remove();
                continue;
            }
            prevCoord = coord;
        }
    }
}
