package org.opengis.cite.geomatics;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.geotoolkit.temporal.factory.DefaultTemporalFactory;
import org.geotoolkit.temporal.object.DefaultPosition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.RelativePosition;
import org.opengis.temporal.TemporalFactory;

public class VerifyTemporalAssert {

	private static final TemporalFactory TM_FACTORY = new DefaultTemporalFactory();
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void instantDuringPeriod() {
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0,
				ZoneId.of("Z"));
		Instant instant = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(t1.toInstant())));
		Instant startPeriod = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(t1.minusMonths(10).toInstant())));
		Instant endPeriod = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(t1.plusMonths(5).toInstant())));
		Period period = TM_FACTORY.createPeriod(startPeriod, endPeriod);
		TemporalAssert.assertTemporalRelation(RelativePosition.DURING, instant,
				period);
	}

	@Test
	public void periodNotDuringInstant() {
		thrown.expect(AssertionError.class);
		thrown.expectMessage("The actual temporal relation is CONTAINS");
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0,
				ZoneId.of("Z"));
		Instant instant = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(t1.toInstant())));
		Instant startPeriod = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(t1.minusMonths(10).toInstant())));
		Instant endPeriod = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(t1.plusMonths(5).toInstant())));
		Period period = TM_FACTORY.createPeriod(startPeriod, endPeriod);
		TemporalAssert.assertTemporalRelation(RelativePosition.DURING, period,
				instant);
	}

	@Test
	public void instantBeforePeriod() {
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0,
				ZoneId.of("Z"));
		Instant instant = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(t1.toInstant())));
		Instant startPeriod = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(t1.plusMonths(1).toInstant())));
		Instant endPeriod = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(t1.plusMonths(5).toInstant())));
		Period period = TM_FACTORY.createPeriod(startPeriod, endPeriod);
		TemporalAssert.assertTemporalRelation(RelativePosition.BEFORE, instant,
				period);
	}

	@Test
	public void instantNotBeforePeriod() {
		thrown.expect(AssertionError.class);
		thrown.expectMessage("The actual temporal relation is AFTER");
		ZonedDateTime t1 = ZonedDateTime.of(2015, 12, 3, 10, 15, 30, 0,
				ZoneId.of("Z"));
		Instant instant = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(t1.toInstant())));
		Instant startPeriod = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(t1.minusMonths(5).toInstant())));
		Instant endPeriod = TM_FACTORY.createInstant(new DefaultPosition(Date
				.from(t1.minusMonths(1).toInstant())));
		Period period = TM_FACTORY.createPeriod(startPeriod, endPeriod);
		TemporalAssert.assertTemporalRelation(RelativePosition.BEFORE, instant,
				period);
	}
}
