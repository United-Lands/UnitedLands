package org.unitedlands.unitedlands.utils;

import java.util.ArrayList;
import java.util.List;

public class PolygonUtils {

    public static boolean isPointInPolygon(double[] polygon, double px, double py) {

        if (polygon == null)
            return false;

        int n = polygon.length / 2;
        if (n < 3)
            return false;

        boolean inside = false;

        double xj = polygon[2 * (n - 1)];
        double yj = polygon[2 * (n - 1) + 1];

        for (int i = 0; i < n; i++) {
            double xi = polygon[2 * i];
            double yi = polygon[2 * i + 1];

            boolean intersects = ((yi > py) != (yj > py)) && (px < (xj - xi) * (py - yi) / (yj - yi) + xi);

            if (intersects) {
                inside = !inside;
            }

            xj = xi;
            yj = yi;
        }

        return inside;
    }

    /** Main entry point: guaranteed-inside center */
    public static double[] calculatePolygonCenter(double[] points) {
        double[] centroid = areaCentroid(points);
        if (isPointInPolygon(points, centroid[0], centroid[1])) {
            return centroid;
        }
        // Fallback: centroid of the largest triangle from ear-clipping triangulation
        List<double[]> triangles = triangulate(points);
        double[] best = null;
        double bestArea = -1;
        for (double[] tri : triangles) {
            double area = triangleArea(tri);
            if (area > bestArea) {
                bestArea = area;
                best = tri;
            }
        }
        return triangleCentroid(best);
    }

    /**
     * Area-weighted centroid (correct centroid formula, not just vertex average)
     */
    private static double[] areaCentroid(double[] points) {
        int n = points.length / 2;
        double cx = 0, cy = 0, area = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double xi = points[2 * i], yi = points[2 * i + 1];
            double xj = points[2 * j], yj = points[2 * j + 1];
            double cross = xi * yj - xj * yi;
            area += cross;
            cx += (xi + xj) * cross;
            cy += (yi + yj) * cross;
        }
        area *= 0.5;
        if (Math.abs(area) < 1e-9) {
            // Degenerate polygon fallback: plain vertex average
            double sx = 0, sy = 0;
            for (int i = 0; i < n; i++) {
                sx += points[2 * i];
                sy += points[2 * i + 1];
            }
            return new double[] { sx / n, sy / n };
        }
        cx /= (6 * area);
        cy /= (6 * area);
        return new double[] { cx, cy };
    }

    private static double triangleArea(double[] t) {
        return Math.abs((t[2] - t[0]) * (t[5] - t[1]) - (t[4] - t[0]) * (t[3] - t[1])) / 2.0;
    }

    private static double[] triangleCentroid(double[] t) {
        return new double[] { (t[0] + t[2] + t[4]) / 3.0, (t[1] + t[3] + t[5]) / 3.0 };
    }

    /**
     * Ear-clipping triangulation. Returns list of triangles, each as
     * [x1,y1,x2,y2,x3,y3].
     */
    private static List<double[]> triangulate(double[] points) {
        int n = points.length / 2;
        List<double[]> result = new ArrayList<>();
        if (n < 3)
            return result;

        // Ensure CCW winding for the standard ear-clip convexity test
        List<Integer> indices = new ArrayList<>();
        double signedArea = signedArea(points);
        if (signedArea < 0) {
            for (int i = n - 1; i >= 0; i--)
                indices.add(i);
        } else {
            for (int i = 0; i < n; i++)
                indices.add(i);
        }

        int guard = 0;
        int maxIterations = n * n; // safety valve against infinite loops on bad input

        while (indices.size() > 3 && guard++ < maxIterations) {
            boolean earFound = false;

            for (int i = 0; i < indices.size(); i++) {
                int iPrev = indices.get((i - 1 + indices.size()) % indices.size());
                int iCurr = indices.get(i);
                int iNext = indices.get((i + 1) % indices.size());

                double ax = points[2 * iPrev], ay = points[2 * iPrev + 1];
                double bx = points[2 * iCurr], by = points[2 * iCurr + 1];
                double cx = points[2 * iNext], cy = points[2 * iNext + 1];

                if (!isConvex(ax, ay, bx, by, cx, cy))
                    continue;

                boolean anyInside = false;
                for (int idx : indices) {
                    if (idx == iPrev || idx == iCurr || idx == iNext)
                        continue;
                    double px = points[2 * idx], py = points[2 * idx + 1];
                    if (pointInTriangle(px, py, ax, ay, bx, by, cx, cy)) {
                        anyInside = true;
                        break;
                    }
                }

                if (!anyInside) {
                    result.add(new double[] { ax, ay, bx, by, cx, cy });
                    indices.remove(i);
                    earFound = true;
                    break;
                }
            }

            if (!earFound)
                break; // malformed/self-intersecting polygon; stop gracefully
        }

        if (indices.size() == 3) {
            int a = indices.get(0), b = indices.get(1), c = indices.get(2);
            result.add(new double[] { points[2 * a], points[2 * a + 1], points[2 * b], points[2 * b + 1], points[2 * c],
                    points[2 * c + 1] });
        }

        return result;
    }

    private static boolean isConvex(double ax, double ay, double bx, double by, double cx, double cy) {
        return ((bx - ax) * (cy - ay) - (by - ay) * (cx - ax)) > 0;
    }

    private static boolean pointInTriangle(double px, double py, double ax, double ay, double bx, double by, double cx,
            double cy) {
        double d1 = sign(px, py, ax, ay, bx, by);
        double d2 = sign(px, py, bx, by, cx, cy);
        double d3 = sign(px, py, cx, cy, ax, ay);
        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
        return !(hasNeg && hasPos);
    }

    private static double sign(double px, double py, double ax, double ay, double bx, double by) {
        return (px - bx) * (ay - by) - (ax - bx) * (py - by);
    }

    public static double[] offsetPolygon(double[] vertices, double offset) {
        if (vertices.length < 6 || vertices.length % 2 != 0) {
            throw new IllegalArgumentException("Need at least 3 vertices (x,y pairs)");
        }

        // Detect explicit closing vertex (last point == first point) and strip it;
        // we'll add it back at the end so the output has the same shape as the input.
        int totalPoints = vertices.length / 2;
        boolean closed = false;
        double firstX = vertices[0], firstY = vertices[1];
        double lastX = vertices[vertices.length - 2], lastY = vertices[vertices.length - 1];
        if (Math.abs(firstX - lastX) < 1e-9 && Math.abs(firstY - lastY) < 1e-9 && totalPoints > 3) {
            closed = true;
        }

        double[] openVertices = closed ? java.util.Arrays.copyOfRange(vertices, 0, vertices.length - 2) : vertices;

        double[] offsetOpen = offsetPolygonOpen(openVertices, offset);

        if (!closed) {
            return offsetOpen;
        }

        // Re-append the closing vertex (equal to the new first vertex).
        double[] result = new double[offsetOpen.length + 2];
        System.arraycopy(offsetOpen, 0, result, 0, offsetOpen.length);
        result[result.length - 2] = offsetOpen[0];
        result[result.length - 1] = offsetOpen[1];
        return result;
    }

    // Rename your original method to this, unchanged internally:
    private static double[] offsetPolygonOpen(double[] vertices, double offset) {
        int n = vertices.length / 2;

        double signedArea = signedArea(vertices);
        double sign = signedArea >= 0 ? 1.0 : -1.0;

        double[] dirX = new double[n];
        double[] dirY = new double[n];
        double[] refX = new double[n];
        double[] refY = new double[n];

        for (int i = 0; i < n; i++) {
            double x1 = vertices[2 * i];
            double y1 = vertices[2 * i + 1];
            int j = (i + 1) % n;
            double x2 = vertices[2 * j];
            double y2 = vertices[2 * j + 1];

            double dx = x2 - x1;
            double dy = y2 - y1;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 1e-9) {
                throw new IllegalArgumentException("Degenerate edge (duplicate vertex) at index " + i);
            }
            dx /= len;
            dy /= len;

            double nx = sign * dy;
            double ny = -sign * dx;

            dirX[i] = dx;
            dirY[i] = dy;
            refX[i] = x1 + nx * offset;
            refY[i] = y1 + ny * offset;
        }

        double[] result = new double[vertices.length];
        for (int i = 0; i < n; i++) {
            int prev = (i - 1 + n) % n;
            double[] p = intersectLines(refX[prev], refY[prev], dirX[prev], dirY[prev], refX[i], refY[i], dirX[i],
                    dirY[i]);
            result[2 * i] = p[0];
            result[2 * i + 1] = p[1];
        }

        return result;
    }

    private static double[] intersectLines(double p1x, double p1y, double d1x, double d1y, double p2x, double p2y,
            double d2x, double d2y) {
        double cross = d1x * d2y - d1y * d2x;
        if (Math.abs(cross) < 1e-12) {
            // Parallel edges (or a straight-through vertex) — the two offset
            // lines coincide, so the reference point already lies on both.
            return new double[] { p1x, p1y };
        }
        double t = ((p2x - p1x) * d2y - (p2y - p1y) * d2x) / cross;
        return new double[] { p1x + t * d1x, p1y + t * d1y };
    }

    /**
     * Shoelace formula: positive = CCW, negative = CW (in standard math
     * coordinates)
     */
    private static double signedArea(double[] points) {
        int n = points.length / 2;
        double area = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += points[2 * i] * points[2 * j + 1];
            area -= points[2 * j] * points[2 * i + 1];
        }
        return area / 2.0;
    }

    public static double area(double[] vertices) {
        if (vertices.length < 6 || vertices.length % 2 != 0) {
            throw new IllegalArgumentException("Need at least 3 vertices (x,y pairs)");
        }

        int n = vertices.length / 2;
        double signedArea = 0;

        for (int i = 0; i < n; i++) {
            double x1 = vertices[2 * i];
            double y1 = vertices[2 * i + 1];
            int j = (i + 1) % n;
            double x2 = vertices[2 * j];
            double y2 = vertices[2 * j + 1];
            signedArea += x1 * y2 - x2 * y1;
        }

        return Math.abs(signedArea) / 2.0;
    }

    public static boolean sharesVertex(double[] a, double[] b, double epsilon) {
        if (a.length % 2 != 0 || b.length % 2 != 0) {
            throw new IllegalArgumentException("Vertex arrays must contain x,y pairs");
        }

        for (int i = 0; i < a.length; i += 2) {
            double ax = a[i];
            double ay = a[i + 1];
            for (int j = 0; j < b.length; j += 2) {
                if (Math.abs(ax - b[j]) <= epsilon && Math.abs(ay - b[j + 1]) <= epsilon) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Convenience overload with a default tolerance of 1e-9. */
    public static boolean sharesVertex(double[] a, double[] b) {
        return sharesVertex(a, b, 1e-9);
    }

}
