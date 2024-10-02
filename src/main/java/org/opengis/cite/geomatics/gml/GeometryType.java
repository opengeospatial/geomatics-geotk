package org.opengis.cite.geomatics.gml;

/**
 * Geometric primitive types defined in ISO 19107 and realized in ISO 19136 (GML 3.2.1).
 * All geometric primitives are oriented in the direction implied by the sequence of their
 * constituent coordinate tuples.
 */
public enum GeometryType {

	/** GM_POINT (gml:Point) */
	POINT {
		@Override
		public CoordinateListFactory getCoordinateSetFactory() {
			return new PointCoordinateListFactory();
		}
	},
	/** GM_CURVE (gml:Curve) */
	CURVE {
		@Override
		public CoordinateListFactory getCoordinateSetFactory() {
			return new CurveCoordinateListFactory();
		}
	},
	/** GM_SURFACE (gml:Suface) */
	SURFACE {
		@Override
		public CoordinateListFactory getCoordinateSetFactory() {
			return new SurfaceCoordinateListFactory();
		}
	};

	public abstract CoordinateListFactory getCoordinateSetFactory();

}
