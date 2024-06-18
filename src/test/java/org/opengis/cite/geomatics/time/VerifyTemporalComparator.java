package org.opengis.cite.geomatics.time;

import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.geotoolkit.temporal.factory.DefaultTemporalFactory;
import org.junit.Test;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalFactory;

public class VerifyTemporalComparator {

	private static final TemporalFactory TM_FACTORY = new DefaultTemporalFactory();

	@Test
	public void instantBeforeInstant() {
		ZonedDateTime dateTime = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0,
				ZoneId.of("Z"));
		Instant t1 = TM_FACTORY.createInstant(Date
				.from(dateTime.toInstant()));
		Instant t2 = TM_FACTORY.createInstant(Date
				.from(dateTime.plusMonths(1).toInstant()));
		TemporalComparator iut = new TemporalComparator();
		int comparison = iut.compare(t1, t2);
		assertTrue(comparison < 0);
	}

	@Test
	public void instantAfterPeriod() {
		ZonedDateTime dateTime = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0,
				ZoneId.of("Z"));
		Instant t1 = TM_FACTORY.createInstant(Date
				.from(dateTime.toInstant()));
		Instant startOfPeriod = TM_FACTORY.createInstant(
				Date.from(dateTime.minusMonths(5).toInstant()));
		Instant endOfPeriod = TM_FACTORY.createInstant(Date
				.from(dateTime.minusMonths(1).toInstant()));
		Period t2 = TM_FACTORY.createPeriod(startOfPeriod, endOfPeriod);
		TemporalComparator iut = new TemporalComparator();
		int comparison = iut.compare(t1, t2);
		assertTrue(comparison > 0);
	}

	@Test
	public void periodBeforePeriod() {
		ZonedDateTime dateTime = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0,
				ZoneId.of("Z"));
		Instant startP1 = TM_FACTORY.createInstant(Date
				.from(dateTime.minusMonths(5).toInstant()));
		Instant endP1 = TM_FACTORY.createInstant(Date
				.from(dateTime.minusMonths(1).toInstant()));
		Period t1 = TM_FACTORY.createPeriod(startP1, endP1);
		Instant startP2 = TM_FACTORY.createInstant(Date
				.from(dateTime.plusDays(5).toInstant()));
		Instant endP2 = TM_FACTORY.createInstant(Date
				.from(dateTime.plusDays(15).toInstant()));
		Period t2 = TM_FACTORY.createPeriod(startP2, endP2);
		TemporalComparator iut = new TemporalComparator();
		int comparison = iut.compare(t1, t2);
		assertTrue(comparison < 0);
	}
}
