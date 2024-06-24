package eu.virtualparadox.springembedder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Vector2DTest {

    private static final double EPS = 1e-10;

    @Test
    void testGetX() {
        final Vector2D vector = new Vector2D(1.0, 2.0);
        assertEquals(1.0, vector.getX(), EPS, "Expected x-coordinate to be 1.0");
    }

    @Test
    void testGetY() {
        final Vector2D vector = new Vector2D(1.0, 2.0);
        assertEquals(2.0, vector.getY(), EPS, "Expected y-coordinate to be 2.0");
    }

    @Test
    void testAdd() {
        final Vector2D vector1 = new Vector2D(1.0, 2.0);
        final Vector2D vector2 = new Vector2D(3.0, 4.0);
        final Vector2D result = vector1.add(vector2);
        assertEquals(4.0, result.getX(), EPS, "Expected x-coordinate of addition result to be 4.0");
        assertEquals(6.0, result.getY(), EPS, "Expected y-coordinate of addition result to be 6.0");
    }

    @Test
    void testSubtract() {
        final Vector2D vector1 = new Vector2D(5.0, 7.0);
        final Vector2D vector2 = new Vector2D(3.0, 4.0);
        final Vector2D result = vector1.subtract(vector2);
        assertEquals(2.0, result.getX(), EPS, "Expected x-coordinate of subtraction result to be 2.0");
        assertEquals(3.0, result.getY(), EPS, "Expected y-coordinate of subtraction result to be 3.0");
    }

    @Test
    void testScale() {
        final Vector2D vector = new Vector2D(2.0, 3.0);
        final Vector2D result = vector.scale(2.0);
        assertEquals(4.0, result.getX(), EPS, "Expected x-coordinate of scale result to be 4.0");
        assertEquals(6.0, result.getY(), EPS, "Expected y-coordinate of scale result to be 6.0");
    }

    @Test
    void testDot() {
        final Vector2D vector1 = new Vector2D(1.0, 2.0);
        final Vector2D vector2 = new Vector2D(3.0, 4.0);
        final double result = vector1.dot(vector2);
        assertEquals(11.0, result, EPS, "Expected dot product to be 11.0");
    }

    @Test
    void testLength() {
        final Vector2D vector = new Vector2D(3.0, 4.0);
        assertEquals(5.0, vector.length(), EPS, "Expected vector length to be 5.0");
    }

    @Test
    void testNormalize() {
        final Vector2D vector = new Vector2D(3.0, 4.0);
        final Vector2D result = vector.normalize();
        assertEquals(1.0, result.length(), EPS, "Expected normalized vector length to be 1.0");
    }

    @Test
    void testNormalizeZeroVector() {
        final Vector2D vector = new Vector2D(0.0, 0.0);
        assertThrows(ArithmeticException.class, vector::normalize, "Expected ArithmeticException when normalizing a zero vector");
    }

    @Test
    void testDiv() {
        final Vector2D vector = new Vector2D(4.0, 6.0);
        final Vector2D result = vector.div(2.0);
        assertEquals(2.0, result.getX(), EPS, "Expected x-coordinate of division result to be 2.0");
        assertEquals(3.0, result.getY(), EPS, "Expected y-coordinate of division result to be 3.0");
    }

    @Test
    void testDivByZero() {
        final Vector2D vector = new Vector2D(4.0, 6.0);
        assertThrows(ArithmeticException.class, () -> vector.div(0.0), "Expected ArithmeticException when dividing by zero");
    }
    
    @Test
    void testEquality() {
        final Vector2D vector1 = new Vector2D(1.0, 2.0);
        final Vector2D vector2 = new Vector2D(1.0, 2.0);
        assertEquals(vector1, vector2, "Expected vectors to be equal");
    }

    @Test
    void testInequality() {
        final Vector2D vector1 = new Vector2D(1.0, 2.0);
        final Vector2D vector2 = new Vector2D(2.0, 1.0);
        assertNotEquals(vector1, vector2, "Expected vectors to be not equal");
    }
}
