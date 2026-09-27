package org.rsreu.map.domain.geometry;

/**
 * Represents a 3D point with coordinates in meters.
 * Y is the up axis.
 */
public record Point3(float x, float y, float z) {

    public Point3 {
        // Records are already immutable, this is just for potential future validation
    }

    /**
     * Adds another point to this one.
     */
    public Point3 add(Point3 other) {
        return new Point3(x + other.x, y + other.y, z + other.z);
    }

    /**
     * Subtracts another point from this one.
     */
    public Point3 subtract(Point3 other) {
        return new Point3(x - other.x, y - other.y, z - other.z);
    }

    /**
     * Calculates distance to another point.
     */
    public float distanceTo(Point3 other) {
        float dx = other.x - x;
        float dy = other.y - y;
        float dz = other.z - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
