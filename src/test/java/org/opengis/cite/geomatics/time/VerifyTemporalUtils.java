package org.opengis.cite.geomatics.time;

import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import org.geotoolkit.temporal.factory.DefaultTemporalFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.RelativePosition;
import org.opengis.temporal.TemporalFactory;
import org.opengis.temporal.TemporalGeometricPrimitive;

public class VerifyTemporalUtils {

	private static final TemporalFactory TM_FACTORY = new DefaultTemporalFactory();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void instantDuringPeriod() {
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0, ZoneId.of("Z"));
		Instant instant = TM_FACTORY.createInstant(Date.from(t1.toInstant()));
		Instant startPeriod = TM_FACTORY.createInstant(Date.from(t1.minusMonths(10).toInstant()));
		Instant endPeriod = TM_FACTORY.createInstant(Date.from(t1.plusMonths(5).toInstant()));
		Period period = TM_FACTORY.createPeriod(startPeriod, endPeriod);
		TemporalUtils.assertTemporalRelation(RelativePosition.DURING, instant, period);
	}

	@Test
	public void periodNotDuringInstant() {
		thrown.expect(AssertionError.class);
		thrown.expectMessage("The actual temporal relation is CONTAINS");
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0, ZoneId.of("Z"));
		Instant instant = TM_FACTORY.createInstant(Date.from(t1.toInstant()));
		Instant startPeriod = TM_FACTORY.createInstant(Date.from(t1.minusMonths(10).toInstant()));
		Instant endPeriod = TM_FACTORY.createInstant(Date.from(t1.plusMonths(5).toInstant()));
		Period period = TM_FACTORY.createPeriod(startPeriod, endPeriod);
		TemporalUtils.assertTemporalRelation(RelativePosition.DURING, period, instant);
	}

	@Test
	public void instantBeforePeriod() {
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0, ZoneId.of("Z"));
		Instant instant = TM_FACTORY.createInstant(Date.from(t1.toInstant()));
		Instant startPeriod = TM_FACTORY.createInstant(Date.from(t1.plusMonths(1).toInstant()));
		Instant endPeriod = TM_FACTORY.createInstant(Date.from(t1.plusMonths(5).toInstant()));
		Period period = TM_FACTORY.createPeriod(startPeriod, endPeriod);
		TemporalUtils.assertTemporalRelation(RelativePosition.BEFORE, instant, period);
	}

	@Test
	public void instantNotBeforePeriod() {
		thrown.expect(AssertionError.class);
		thrown.expectMessage("The actual temporal relation is AFTER");
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0, ZoneId.of("Z"));
		Instant instant = TM_FACTORY.createInstant(Date.from(t1.toInstant()));
		Instant startPeriod = TM_FACTORY.createInstant(Date.from(t1.minusMonths(5).toInstant()));
		Instant endPeriod = TM_FACTORY.createInstant(Date.from(t1.minusMonths(1).toInstant()));
		Period period = TM_FACTORY.createPeriod(startPeriod, endPeriod);
		TemporalUtils.assertTemporalRelation(RelativePosition.BEFORE, instant, period);
	}

	@Test
	public void temporalExtentOfDisjointTimes() {
		TreeSet<TemporalGeometricPrimitive> tmSet = new TreeSet<>(new TemporalComparator());
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0, ZoneId.of("Z"));
		Instant instant = TM_FACTORY.createInstant(Date.from(t1.toInstant()));
		tmSet.add(instant);
		Instant startPeriod = TM_FACTORY.createInstant(Date.from(t1.minusMonths(5).toInstant()));
		Instant endPeriod = TM_FACTORY.createInstant(Date.from(t1.minusMonths(1).toInstant()));
		Period period = TM_FACTORY.createPeriod(startPeriod, endPeriod);
		tmSet.add(period);
		Period extent = TemporalUtils.temporalExtent(tmSet);
		assertTrue("Expected duration: P5M", extent.length().toString().startsWith("P5M"));
		DateTimeFormatter xsdDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
		ZonedDateTime endDateTime = ZonedDateTime.parse(TemporalUtils.getDateTime(extent.getEnding()),
				xsdDateTimeFormatter);
		assertTrue("Unexpected end of interval.", t1.plus(1, ChronoUnit.HOURS).isEqual(endDateTime));
	}

	@Test
	public void temporalExtentOfIntersectingTimes() {
		TreeSet<TemporalGeometricPrimitive> tmSet = new TreeSet<>(new TemporalComparator());
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0, ZoneId.of("Z"));
		Instant instant = TM_FACTORY.createInstant(Date.from(t1.toInstant()));
		tmSet.add(instant);
		// period CONTAINS instant
		Instant startPeriod = TM_FACTORY.createInstant(Date.from(t1.minusMonths(5).toInstant()));
		Instant endPeriod = TM_FACTORY.createInstant(Date.from(t1.plusMonths(2).toInstant()));
		Period period = TM_FACTORY.createPeriod(startPeriod, endPeriod);
		tmSet.add(period);
		Period extent = TemporalUtils.temporalExtent(tmSet);
		assertTrue("Expected duration: P7M", extent.length().toString().startsWith("P7M"));
	}

	@Test
	public void add1Day() {
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0, ZoneId.of("Z"));
		Instant instant = TM_FACTORY.createInstant(Date.from(t1.toInstant()));
		Instant newInstant = TemporalUtils.add(instant, 1, ChronoUnit.DAYS);
		assertTrue("Expected date 2015-12-04", TemporalUtils.getDateTime(newInstant).startsWith("2015-12-04"));
	}

	@Test
	public void subtract1Month() {
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0, ZoneId.of("Z"));
		Instant instant = TM_FACTORY.createInstant(Date.from(t1.toInstant()));
		Instant newInstant = TemporalUtils.add(instant, -1, ChronoUnit.MONTHS);
		assertTrue("Expected date 2015-11-03", TemporalUtils.getDateTime(newInstant).startsWith("2015-11-03"));
	}

	@Test
	public void splitPeriodInto2Intervals() {
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0, ZoneId.of("Z"));
		Instant startPeriod = TM_FACTORY.createInstant(Date.from(t1.minusMonths(1).toInstant()));
		Instant endPeriod = TM_FACTORY.createInstant(Date.from(t1.plusMonths(1).toInstant()));
		Period period = TM_FACTORY.createPeriod(startPeriod, endPeriod);
		List<Period> subIntervals = TemporalUtils.splitInterval(period, 2);
		assertEquals(2, subIntervals.size());
		assertTrue("", subIntervals.get(0).relativePosition(subIntervals.get(1)).equals(RelativePosition.MEETS));
	}

}
