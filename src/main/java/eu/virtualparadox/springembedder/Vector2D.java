package eu.virtualparadox.springembedder;

import java.util.Objects;

/**
 * A class representing a 2-dimensional vector.
 */
public class Vector2D {
    private final double x;
    private final double y;

    /**
     * Constructs a Vector2D with the specified x and y coordinates.
     *
     * @param x the x-coordinate of the vector.
     * @param y the y-coordinate of the vector.
     */
    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the x-coordinate of the vector.
     *
     * @return the x-coordinate.
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the y-coordinate of the vector.
     *
     * @return the y-coordinate.
     */
    public double getY() {
        return y;
    }

    /**
     * Adds the given vector to this vector.
     *
     * @param v the vector to add.
     * @return a new vector that is the sum of this vector and the given vector.
     */
    public Vector2D add(Vector2D v) {
        return new Vector2D(this.x + v.x, this.y + v.y);
    }

    /**
     * Subtracts the given vector from this vector.
     *
     * @param v the vector to subtract.
     * @return a new vector that is the difference between this vector and the given vector.
     */
    public Vector2D subtract(Vector2D v) {
        return new Vector2D(this.x - v.x, this.y - v.y);
    }

    /**
     * Scales this vector by the given scalar.
     *
     * @param scalar the scalar to scale the vector by.
     * @return a new vector that is the product of this vector and the scalar.
     */
    public Vector2D scale(double scalar) {
        return new Vector2D(this.x * scalar, this.y * scalar);
    }

    /**
     * Computes the dot product of this vector and the given vector.
     *
     * @param v the vector to compute the dot product with.
     * @return the dot product of this vector and the given vector.
     */
    public double dot(Vector2D v) {
        return this.x * v.x + this.y * v.y;
    }

    /**
     * Returns the magnitude (length) of the vector.
     *
     * @return the magnitude of the vector.
     */
    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    /**
     * Normalizes this vector (makes it unit length).
     *
     * @return a new vector that is the normalized (unit length) version of this vector.
     * @throws ArithmeticException if the vector is a zero vector.
     */
    public Vector2D normalize() {
        double magnitude = length();
        if (magnitude == 0) {
            throw new ArithmeticException("Cannot normalize a zero vector.");
        }
        return new Vector2D(this.x / magnitude, this.y / magnitude);
    }

    /**
     * Divides this vector by the given scalar.
     *
     * @param scalar the scalar to divide the vector by.
     * @return a new vector that is the quotient of this vector and the scalar.
     * @throws ArithmeticException if the scalar is zero.
     */
    public Vector2D div(double scalar) {
        if (scalar == 0) {
            throw new ArithmeticException("Cannot divide by zero.");
        }
        return new Vector2D(this.x / scalar, this.y / scalar);
    }

    /**
     * Compares this vector to the specified object. The result is true if and only if
     * the argument is not null and is a Vector2D object that has the same x and
     * y coordinates as this vector.
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector2D vector2D = (Vector2D) o;
        return Double.compare(x, vector2D.x) == 0 && Double.compare(y, vector2D.y) == 0;
    }

    /**
     * Returns a hash code value for the vector.
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    /**
     * Returns a string representation of the vector.
     *
     * @return a string representation of the vector.
     */
    @Override
    public String toString() {
        return "Vector2D(" + "x=" + x + ", y=" + y + ')';
    }
}
