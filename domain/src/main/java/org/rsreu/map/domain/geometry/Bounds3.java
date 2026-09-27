package org.rsreu.map.domain.geometry;

import java.util.Objects;

/**
 * Represents an axis-aligned bounding box in 3D space.
 */
public record Bounds3(Point3 min, Point3 max) {

    public Bounds3 {
        Objects.requireNonNull(min, "min point cannot be null");
        Objects.requireNonNull(max, "max point cannot be null");
    }

    /**
     * Returns the center point of the bounding box.
     */
    public Point3 center() {
        return new Point3(
            (min.x() + max.x()) / 2f,
            (min.y() + max.y()) / 2f,
            (min.z() + max.z()) / 2f
        );
    }

    /**
     * Checks if this bounding box contains a point.
     */
    public boolean contains(Point3 p) {
        return p.x() >= min.x() && p.x() <= max.x()
            && p.y() >= min.y() && p.y() <= max.y()
            && p.z() >= min.z() && p.z() <= max.z();
    }

    /**
     * Checks if this bounding box intersects another bounding box.
     */
    public boolean intersects(Bounds3 other) {
        return min.x() <= other.max.x() && max.x() >= other.min.x()
            && min.y() <= other.max.y() && max.y() >= other.min.y()
            && min.z() <= other.max.z() && max.z() >= other.min.z();
    }

    /**
     * Returns the width (X dimension) of the bounding box.
     */
    public float width() {
        return max.x() - min.x();
    }

    /**
     * Returns the height (Y dimension) of the bounding box.
     */
    public float height() {
        return max.y() - min.y();
    }

    /**
     * Returns the depth (Z dimension) of the bounding box.
     */
    public float depth() {
        return max.z() - min.z();
    }
}
