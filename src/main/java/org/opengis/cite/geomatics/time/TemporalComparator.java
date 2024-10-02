package org.opengis.cite.geomatics.time;

import java.util.Comparator;

import org.opengis.temporal.TemporalGeometricPrimitive;

/**
 * Compares temporal objects according to their relative position in time.
 */
public class TemporalComparator implements Comparator<TemporalGeometricPrimitive> {

	@Override
	public int compare(TemporalGeometricPrimitive t1, TemporalGeometricPrimitive t2) {
		int comparison = 0;
		String relPos = t1.relativePosition(t2).name();
		switch (relPos) {
			case "DURING":
				// discard (will not affect total temporal extent)
			case "EQUALS":
				comparison = 0;
				break;
			case "OVERLAPS":
				// t1.begin.position < t2.begin.position
			case "MEETS":
				// t1.end.position = t2.begin.position
			case "CONTAINS":
				// t1.begin.position < t2.begin.position
			case "BEFORE":
				comparison = -1;
				break;
			case "MET_BY":
				// t1.begin.position = t2.end.position
			case "AFTER":
				comparison = 1;
				break;
			default:
				// discard
		}
		return comparison;
	}

}
