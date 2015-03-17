package org.opengis.cite.geomatics;

import org.geotoolkit.geometry.Envelopes;
import org.geotoolkit.geometry.GeneralEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Provides specialized assertion methods that apply to representations of
 * spatial objects. Many of these are concerned with evaluating topological
 * relationships between geometry objects.
 * 
 * @see "ISO 19125-1: Geographic information -- Simple feature access -- Part 1: Common architecture"
 */
public class SpatialAssert {

	/**
	 * Asserts that the given envelopes intersect. The coordinate reference
	 * systems used by the envelopes do not have to be the same; a coordinate
	 * transformation will be attempted if necessary.
	 * 
	 * More specifically, this method asserts that the envelopes are not
	 * disjoint:
	 * 
	 * <pre>
	 * a.Intersects(b) <==> ! a.Disjoint(b)
	 * </pre>
	 * 
	 * @param env1
	 *            A GeneralEnvelope object representing a spatial extent.
	 * @param env2
	 *            A GeneralEnvelope object representing some other spatial
	 *            extent.
	 */
	public static void assertIntersects(GeneralEnvelope env1,
			GeneralEnvelope env2) {
		CoordinateReferenceSystem crs1 = env1.getCoordinateReferenceSystem();
		Envelope env = env2;
		if (!env2.getCoordinateReferenceSystem().equals(crs1)) {
			try {
				env = Envelopes.transform(env2, crs1);
			} catch (TransformException te) {
				StringBuilder msg = new StringBuilder(
						"Coordinate transformation failed.");
				msg.append("\n crs1 is ").append(
						GeodesyUtils.getCRSIdentifier(crs1));
				msg.append("\n crs2 is ").append(
						GeodesyUtils.getCRSIdentifier(env2
								.getCoordinateReferenceSystem()));
				throw new AssertionError(msg.toString(), te);
			}
		}
		if (!env1.intersects(env, false)) {
			StringBuilder msg = new StringBuilder(
					"The envelopes do not intersect.\n");
			msg.append(env1.toString()).append(" with CRS ")
					.append(GeodesyUtils.getCRSIdentifier(crs1));
			msg.append('\n')
					.append(env2.toString())
					.append(" with CRS ")
					.append(GeodesyUtils.getCRSIdentifier(env2
							.getCoordinateReferenceSystem()));
			throw new AssertionError(msg.toString());
		}
	}
}
