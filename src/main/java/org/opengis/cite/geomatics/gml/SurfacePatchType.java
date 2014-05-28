package org.opengis.cite.geomatics.gml;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.geotoolkit.gml.xml.v321.AbstractRingPropertyType;
import org.geotoolkit.gml.xml.v321.AbstractRingType;
import org.geotoolkit.gml.xml.v321.AbstractSurfacePatchType;
import org.geotoolkit.gml.xml.v321.PolygonPatchType;
import org.geotoolkit.gml.xml.v321.RectangleType;
import org.geotoolkit.gml.xml.v321.TriangleType;

/**
 * Surface patch type. A surface is composed of one or more homogeneous,
 * connected patches. Adjacent patches are connected along shared edges.
 * 
 */
public enum SurfacePatchType {

    /** gml:PolygonPatch */
    POLYGON {
        @Override
        public AbstractRingType getExteriorBoundary(
                AbstractSurfacePatchType patch) {
            PolygonPatchType polygon = PolygonPatchType.class.cast(patch);
            return polygon.getExterior().getAbstractRing();
        }

        @Override
        public Set<AbstractRingType> getInteriorBoundaries(
                AbstractSurfacePatchType patch) {
            PolygonPatchType polygon = PolygonPatchType.class.cast(patch);
            Set<AbstractRingType> set = new HashSet<AbstractRingType>();
            for (AbstractRingPropertyType ringProp : polygon.getInterior()) {
                set.add(ringProp.getAbstractRing());
            }
            return set;
        }
    },
    /** gml:Rectangle */
    RECTANGLE {
        @Override
        public AbstractRingType getExteriorBoundary(
                AbstractSurfacePatchType patch) {
            RectangleType rectangle = RectangleType.class.cast(patch);
            return rectangle.getExterior().getAbstractRing();
        }

        @Override
        public Set<AbstractRingType> getInteriorBoundaries(
                AbstractSurfacePatchType patch) {
            return Collections.emptySet();
        }
    },
    /** gml:Triangle */
    TRIANGLE {
        @Override
        public AbstractRingType getExteriorBoundary(
                AbstractSurfacePatchType patch) {
            TriangleType triangle = TriangleType.class.cast(patch);
            return triangle.getExterior().getAbstractRing();
        }

        @Override
        public Set<AbstractRingType> getInteriorBoundaries(
                AbstractSurfacePatchType patch) {
            return Collections.emptySet();
        }
    };

    /**
     * Returns the exterior boundary of a surface patch (an element that
     * substitutes for gml:AbstractSurfacePatch).
     * 
     * @param patch
     *            A GML surface patch.
     * @return A closed curve (LinearRing or Ring) that delimits the exterior of
     *         the given surface patch.
     */
    public abstract AbstractRingType getExteriorBoundary(
            AbstractSurfacePatchType patch);

    /**
     * Returns the set of interior boundaries for a surface patch (an element
     * that substitutes for gml:AbstractSurfacePatch).
     * 
     * @param patch
     *            A GML surface patch.
     * @return A set containing closed curves that delimit the interior of the
     *         given surface patch; the set may be empty.
     */
    public abstract Set<AbstractRingType> getInteriorBoundaries(
            AbstractSurfacePatchType patch);
}
