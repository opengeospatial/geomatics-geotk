package org.opengis.cite.geomatics.time;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import org.geotoolkit.temporal.factory.DefaultTemporalFactory;
import org.geotoolkit.temporal.object.DefaultPosition;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.RelativePosition;
import org.opengis.temporal.TemporalFactory;
import org.opengis.temporal.TemporalGeometricPrimitive;

/**
 * Provides various utility methods that apply to representations of temporal
 * objects.
 * 
 * @see "ISO 19108: Geographic information -- Temporal schema"
 * @see <a
 *      href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.329.2647">Maintaining
 *      knowledge about temporal intervals</a>
 */
public class TemporalUtils {

	private static final TemporalFactory TM_FACTORY = new DefaultTemporalFactory();

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
	public static String temporalGeometricPrimitiveToString(
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
			strBuilder.append(period.getEnding().getPosition().getDateTime());
		}
		return strBuilder.toString();
	}

	/**
	 * Determines the total temporal extent of a set of temporal primitives. The
	 * actual interval is extended by one hour at each end to ensure it contains
	 * all temporal objects in the set.
	 * 
	 * @param tmSet
	 *            An ordered set of TemporalGeometricPrimitive objects (instant
	 *            or period); it cannot be empty.
	 * @return A period that contains the set members.
	 */
	public static Period temporalExtent(
			TreeSet<TemporalGeometricPrimitive> tmSet) {
		if (tmSet.isEmpty()) {
			throw new IllegalArgumentException(
					"Empty Set<TemporalGeometricPrimitive>");
		}
		TemporalGeometricPrimitive first = tmSet.first();
		TemporalGeometricPrimitive last = tmSet.last();
		Instant startOfPeriod;
		if (first instanceof Instant) {
			startOfPeriod = Instant.class.cast(first);
		} else {
			startOfPeriod = Period.class.cast(first).getBeginning();
		}
		startOfPeriod = add(startOfPeriod, -1, ChronoUnit.HOURS);
		Instant endOfPeriod;
		if (last instanceof Instant) {
			endOfPeriod = Instant.class.cast(last);
			// check if last occurs DURING first
			if ((first instanceof Period)
					&& endOfPeriod.relativePosition(
							Period.class.cast(first).getEnding()).equals(
							RelativePosition.BEFORE)) {
				endOfPeriod = Period.class.cast(first).getEnding();
			}
		} else {
			endOfPeriod = Period.class.cast(last).getEnding();
		}
		endOfPeriod = add(endOfPeriod, 1, ChronoUnit.HOURS);
		return TM_FACTORY.createPeriod(startOfPeriod, endOfPeriod);
	}

	/**
	 * Returns a copy of the given instant with the specified amount added or
	 * subtracted.
	 * 
	 * @param instant
	 *            An instantaneous point in time.
	 * @param amount
	 *            The amount to add (positive) or subtract (negative).
	 * @param unit
	 *            The date-time unit of the amount.
	 * @return A new Instant representing the resulting date-time value.
	 */
	public static Instant add(Instant instant, int amount, TemporalUnit unit) {
		DateTimeFormatter xsdDateTimeFormatter = DateTimeFormatter
				.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
		ZonedDateTime dateTime = ZonedDateTime.parse(instant.getPosition()
				.getDateTime().toString(), xsdDateTimeFormatter);
		ZonedDateTime newDateTime = dateTime.plus(amount, unit);
		Instant newInstant = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(newDateTime.toInstant())));
		return newInstant;
	}

	/**
	 * Splits a time period into the specified number of intervals. Each
	 * sub-interval will have approximately the same length.
	 * 
	 * @param period
	 *            A temporal interval.
	 * @param size
	 *            The number of sub-intervals.
	 * @return A sequence of contiguous sub-intervals (i.e. interval n MEETS
	 *         interval n+1).
	 */
	public static List<Period> splitInterval(Period period, int size) {
		DateTimeFormatter xsdDateTimeFormatter = DateTimeFormatter
				.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
		ZonedDateTime startDateTime = ZonedDateTime.parse(period.getBeginning()
				.getPosition().getDateTime().toString(), xsdDateTimeFormatter);
		ZonedDateTime endDateTime = ZonedDateTime.parse(period.getEnding()
				.getPosition().getDateTime().toString(), xsdDateTimeFormatter);
		Duration duration = Duration.between(startDateTime, endDateTime)
				.dividedBy(size);
		List<Period> subIntervals = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			Instant startInstant = TM_FACTORY
					.createInstant(new DefaultPosition(Date.from(startDateTime
							.toInstant())));
			endDateTime = startDateTime.plus(duration);
			Instant endInstant = TM_FACTORY.createInstant(new DefaultPosition(
					Date.from(endDateTime.toInstant())));
			subIntervals.add(TM_FACTORY.createPeriod(startInstant, endInstant));
			startDateTime = endDateTime;
		}
		return subIntervals;
	}
}
