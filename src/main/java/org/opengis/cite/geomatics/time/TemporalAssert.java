package org.opengis.cite.geomatics.time;

import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.RelativePosition;
import org.opengis.temporal.TemporalGeometricPrimitive;

/**
 * Provides specialized assertion methods that apply to representations of
 * temporal objects.
 * 
 * @see "ISO 19108: Geographic information -- Temporal schema"
 * @see <a
 *      href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.329.2647">Maintaining
 *      knowledge about temporal intervals</a>
 */
public class TemporalAssert {

	/**
	 * Asserts that the first temporal primitive is related to the second one so
	 * as to satisfy the specified temporal relationship. There are 13 temporal
	 * relationships defined in ISO 19108:
	 * <ul>
	 * <li>Before</li>
	 * <li>After</li>
	 * <li>Begins</li>
	 * <li>Ends</li>
	 * <li>During</li>
	 * <li>Equals</li>
	 * <li>Contains</li>
	 * <li>Overlaps</li>
	 * <li>Meets</li>
	 * <li>OverlappedBy</li>
	 * <li>MetBy</li>
	 * <li>BegunBy</li>
	 * <li>EndedBy</li>
	 * </ul>
	 * 
	 * @param temporalRelation
	 *            A RelativePosition instance that designates a temporal
	 *            relationship.
	 * @param t1
	 *            A temporal geometric primitive (instant or period).
	 * @param t2
	 *            Another temporal geometric primitive.
	 */
	public static void assertTemporalRelation(
			RelativePosition temporalRelation, TemporalGeometricPrimitive t1,
			TemporalGeometricPrimitive t2) {
		RelativePosition relPos = t1.relativePosition(t2);
		if (!relPos.equals(temporalRelation)) {
			throw new AssertionError(
					String.format(
							"t1 (%s) is not related to t2 (%s): %s. The actual temporal relation is %s.",
							temporalGeometricPrimitiveToString(t1),
							temporalGeometricPrimitiveToString(t2),
							temporalRelation.name(),
							(null != relPos) ? relPos.name() : "UNDEFINED"));
		}
	}

	/**
	 * Produces a condensed string representation of the given temporal object.
	 * 
	 * @param timeObj
	 *            A temporal geometric primitive (instant or period).
	 * @return A String that displays temporal position in accord with ISO 8601.
	 */
	static String temporalGeometricPrimitiveToString(
			TemporalGeometricPrimitive timeObj) {
		StringBuilder strBuilder = new StringBuilder();
		if (Instant.class.isInstance(timeObj)) {
			strBuilder.append("Instant: ");
			Instant instant = Instant.class.cast(timeObj);
			strBuilder.append(instant.getPosition().getDateTime());
		} else {
			strBuilder.append("Period: ");
			Period period = Period.class.cast(timeObj);
			strBuilder
					.append(period.getBeginning().getPosition().getDateTime())
					.append('/');
			strBuilder
					.append(period.getBeginning().getPosition().getDateTime());
		}
		return strBuilder.toString();
	}
}
