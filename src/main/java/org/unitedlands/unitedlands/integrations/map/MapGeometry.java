package org.unitedlands.unitedlands.integrations.map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.interfaces.CoordinateHolder;
import org.unitedlands.utils.United;

public final class MapGeometry {

    private MapGeometry() {
    }

    private enum DIRECTION {
        RIGHT, DOWN, UP, LEFT
    };

    public static class ChunkCluster {
        private final Set<CoordinateHolder> chunks = new HashSet<>();

        public Set<CoordinateHolder> getChunks() {
            return chunks;
        }
    }

    public record MapChunk(Coordinates coordinates) implements CoordinateHolder {

        private static final int SIZE = 16;

        @Override
        public Coordinates getCoordinates() {
            return coordinates;
        }

        @Override
        public Coordinates getWorldCoords() {
            return new Coordinates(coordinates.getX() * SIZE, coordinates.getZ() * SIZE, coordinates.getWorldName());
        }

        @Override
        public Coordinates getCenter() {
            var worldCoords = getWorldCoords();
            return new Coordinates(worldCoords.getX() + (SIZE / 2), worldCoords.getZ() - (SIZE / 2),
                    coordinates.getWorldName());
        }

        @Override
        public Coordinates getLowerLeft() {
            var worldCoords = getWorldCoords();
            return new Coordinates(worldCoords.getX(), worldCoords.getZ() + SIZE, coordinates.getWorldName());
        }

        @Override
        public Coordinates getLowerRight() {
            var worldCoords = getWorldCoords();
            return new Coordinates(worldCoords.getX() + SIZE, worldCoords.getZ() + SIZE, coordinates.getWorldName());
        }

        @Override
        public Coordinates getUpperLeft() {
            var worldCoords = getWorldCoords();
            return new Coordinates(worldCoords.getX(), worldCoords.getZ(), coordinates.getWorldName());
        }

        @Override
        public Coordinates getUpperRight() {
            var worldCoords = getWorldCoords();
            return new Coordinates(worldCoords.getX() + SIZE, worldCoords.getZ(), coordinates.getWorldName());
        }
    }

    // *******************************
    // Chunk cluster detections
    // *******************************

    public static Set<ChunkCluster> findClusters(Set<? extends CoordinateHolder> chunks) {
        if (chunks == null || chunks.isEmpty())
            return Collections.emptySet();

        Map<String, CoordinateHolder> lookup = new HashMap<>((4 * chunks.size()) / 3);
        for (CoordinateHolder chunk : chunks) {
            lookup.put(toKey(chunk.getCoordinates()), chunk);
        }

        Set<ChunkCluster> clusters = new HashSet<>();

        while (!lookup.isEmpty()) {
            ChunkCluster cluster = new ChunkCluster();
            Deque<String> stack = new ArrayDeque<>();

            stack.push(lookup.keySet().iterator().next());

            while (!stack.isEmpty()) {
                String key = stack.pop();
                CoordinateHolder chunk = lookup.remove(key);

                if (chunk == null)
                    continue;

                cluster.chunks.add(chunk);

                Coordinates c = chunk.getCoordinates();
                int[] offsets = { -1, 1 };
                for (int i = 0; i < 2; i++) {
                    for (int dir : offsets) {
                        int nx = c.getX() + (i == 0 ? dir : 0);
                        int nz = c.getZ() + (i == 1 ? dir : 0);
                        String neighbourKey = toKey(nx, nz, c.getWorldName());
                        if (lookup.containsKey(neighbourKey))
                            stack.push(neighbourKey);
                    }
                }
            }

            if (!cluster.chunks.isEmpty())
                clusters.add(cluster);
        }

        return clusters;
    }

    private static String toKey(Coordinates c) {
        return toKey(c.getX(), c.getZ(), c.getWorldName());
    }

    private static String toKey(int x, int z, String world) {
        return x + "," + z + "," + world;
    }

    // *********************************************
    // Negative space detection
    // *********************************************

    public static Set<ChunkCluster> findHoles(ChunkCluster cluster, String worldName) {
        if (cluster.chunks.isEmpty())
            return Collections.emptySet();

        // Build a lookup set of all occupied coordinates in this cluster
        Set<String> occupied = new HashSet<>();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (CoordinateHolder chunk : cluster.chunks) {
            Coordinates c = chunk.getCoordinates();
            occupied.add(toKey(c.getX(), c.getZ(), worldName));
            minX = Math.min(minX, c.getX());
            maxX = Math.max(maxX, c.getX());
            minZ = Math.min(minZ, c.getZ());
            maxZ = Math.max(maxZ, c.getZ());
        }

        // Expand bounding box by 1 in all directions so the flood-fill
        // seed is guaranteed to be outside the cluster
        minX--;
        maxX++;
        minZ--;
        maxZ++;

        // Flood-fill from the top-left corner outward, through all empty chunks
        // within the expanded bounding box. Any empty chunk reachable from here
        // is NOT a hole.
        Set<String> reachable = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        String seedKey = toKey(minX, minZ, worldName);
        stack.push(seedKey);
        reachable.add(seedKey);

        int[] offsets = { -1, 1 };
        while (!stack.isEmpty()) {
            String key = stack.pop();
            // Decode the key back to coordinates for neighbour calculation
            String[] parts = key.split(",");
            try {
                int x = Integer.parseInt(parts[0]);
                int z = Integer.parseInt(parts[1]);

                for (int i = 0; i < 2; i++) {
                    for (int dir : offsets) {
                        int nx = x + (i == 0 ? dir : 0);
                        int nz = z + (i == 1 ? dir : 0);

                        // Stay within the expanded bounding box
                        if (nx < minX || nx > maxX || nz < minZ || nz > maxZ)
                            continue;

                        String neighbourKey = toKey(nx, nz, worldName);
                        if (!occupied.contains(neighbourKey) && reachable.add(neighbourKey))
                            stack.push(neighbourKey);
                    }
                }
            } catch (Exception ex) {
                United.logger().error("Could not parse x, y for key " + key + ": " + ex.getMessage(), "UnitedLands");
            }
        }

        // Collect all empty chunks inside the (unexpanded) bounding box
        // that were never reached — these are holes
        Set<MapChunk> holeChunks = new HashSet<>();
        for (int x = minX + 1; x <= maxX - 1; x++) {
            for (int z = minZ + 1; z <= maxZ - 1; z++) {
                String key = toKey(x, z, worldName);
                if (!occupied.contains(key) && !reachable.contains(key)) {
                    holeChunks.add(new MapChunk(new Coordinates(x, z, worldName)));
                }
            }
        }

        // Group the hole chunks into connected components — each is a distinct hole
        return findClusters(holeChunks);
    }

    // *******************************
    // Polygon creation
    // *******************************

    public static List<Coordinates> generatePolygon(Set<CoordinateHolder> chunkSet) {

        Map<Coordinates, CoordinateHolder> chunks = new HashMap<>();
        for (var chunk : chunkSet)
            chunks.put(chunk.getCoordinates(), chunk);

        Set<Coordinates> polygon = new LinkedHashSet<>();

        var rightmostChunk = findRightmostChunk(chunkSet);

        var originPoint = rightmostChunk.getUpperLeft();

        Queue<CoordinateHolder> chunksToVisit = new ArrayDeque<>(1);
        chunksToVisit.add(rightmostChunk);

        DIRECTION currDir = DIRECTION.RIGHT;

        boolean isOriginPoint = true;
        while (!chunksToVisit.isEmpty()) {

            var chunk = chunksToVisit.poll();

            switch (currDir) {
                case RIGHT: {
                    var upperLeft = chunk.getUpperLeft();
                    if (!isOriginPoint && upperLeft.equals(originPoint))
                        continue;
                    else if (isOriginPoint)
                        isOriginPoint = false;

                    var upCoords = chunk.getCoordinates().clone().add(0, -1);
                    var rightCoords = chunk.getCoordinates().clone().add(1, 0);
                    // Check if there's a chunk above
                    if (chunks.containsKey(upCoords)) {
                        chunksToVisit.add(chunks.get(upCoords));
                        currDir = DIRECTION.UP;
                        polygon.add(chunk.getUpperLeft());
                        // Check if there's a chunk to the right
                    } else if (chunks.containsKey(rightCoords)) {
                        chunksToVisit.add(chunks.get(rightCoords));
                        // We're the rightmost, so switch direction to down and queue the same block
                    } else {
                        chunksToVisit.add(chunk);
                        currDir = DIRECTION.DOWN;
                        polygon.add(chunk.getUpperRight());
                    }
                    break;
                }
                case LEFT: {
                    var downCoords = chunk.getCoordinates().clone().add(0, 1);
                    var leftCoords = chunk.getCoordinates().clone().add(-1, 0);
                    // Check if there's a chunk below
                    if (chunks.containsKey(downCoords)) {
                        chunksToVisit.add(chunks.get(downCoords));
                        currDir = DIRECTION.DOWN;
                        polygon.add(chunk.getLowerRight());
                        // Check if there's a chunk to the right
                    } else if (chunks.containsKey(leftCoords)) {
                        chunksToVisit.add(chunks.get(leftCoords));
                        // We're the the leftmost, so switch direction to up
                    } else {
                        chunksToVisit.add(chunk);
                        currDir = DIRECTION.UP;
                        polygon.add(chunk.getLowerLeft());

                    }
                    break;
                }
                case DOWN: {
                    var rightCoords = chunk.getCoordinates().clone().add(1, 0);
                    var downCoords = chunk.getCoordinates().clone().add(0, 1);
                    // Check if there's a chunk to the right
                    if (chunks.containsKey(rightCoords)) {
                        chunksToVisit.add(chunks.get(rightCoords));
                        currDir = DIRECTION.RIGHT;
                        polygon.add(chunk.getUpperRight());
                        // Check if there's a chunk below
                    } else if (chunks.containsKey(downCoords)) {
                        chunksToVisit.add(chunks.get(downCoords));
                        // We're the the bottom most, so switch direction to left
                    } else {
                        chunksToVisit.add(chunk);
                        currDir = DIRECTION.LEFT;
                        polygon.add(chunk.getLowerRight());
                    }
                    break;
                }
                case UP: {
                    var leftCoords = chunk.getCoordinates().clone().add(-1, 0);
                    var upCoords = chunk.getCoordinates().clone().add(0, -1);
                    // Check if there's a chunk to the left
                    if (chunks.containsKey(leftCoords)) {
                        chunksToVisit.add(chunks.get(leftCoords));
                        currDir = DIRECTION.LEFT;
                        polygon.add(chunk.getLowerLeft());
                        // Check if there's a chunk below
                    } else if (chunks.containsKey(upCoords)) {
                        chunksToVisit.add(chunks.get(upCoords));
                        // We're the the top most, so switch direction to right
                    } else {
                        chunksToVisit.add(chunk);
                        currDir = DIRECTION.RIGHT;
                        polygon.add(chunk.getUpperLeft());

                    }
                    break;
                }
            }
        }

        return new ArrayList<>(polygon);
    }

    private static CoordinateHolder findRightmostChunk(Set<CoordinateHolder> chunks) {
        var rightmost = chunks.stream().findAny().orElse(null);
        for (var chunk : chunks) {
            if (chunk.getCoordinates().getX() > rightmost.getCoordinates().getX()
                    || (chunk.getCoordinates().getX() == rightmost.getCoordinates().getX()
                            && chunk.getCoordinates().getZ() < rightmost.getCoordinates().getZ())) {
                rightmost = chunk;
            }
        }
        return rightmost;
    }

    // *******************************
    // Polygon merging
    // *******************************

    // Yes this is AI code, but icba to spend weeks learning complex polygon merging
    // from scratch -_-

    private static final double SNAP_EPS = 1e-6;
    private static final double T_EPS = 1e-9; // parametric tolerance for "strictly between" endpoints

    private static final class Pt {
        double x, y;

        Pt(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class EdgeKey {
        final int a, b;

        EdgeKey(int a, int b) {
            this.a = Math.min(a, b);
            this.b = Math.max(a, b);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof EdgeKey))
                return false;
            EdgeKey e = (EdgeKey) o;
            return a == e.a && b == e.b;
        }

        @Override
        public int hashCode() {
            return a * 31 + b;
        }
    }

    // ---------- Step 1: snap near-coincident vertices across all polygons
    // ----------

    private static int[] snapVertices(List<double[]> polygons, List<Pt> canonicalPoints) {
        Map<Long, List<Integer>> grid = new HashMap<>();
        int totalVerts = 0;
        for (double[] poly : polygons)
            totalVerts += poly.length / 2;

        int[] canonicalIndex = new int[totalVerts];
        double cellSize = SNAP_EPS * 2;
        int idx = 0;

        for (double[] poly : polygons) {
            for (int i = 0; i < poly.length; i += 2) {
                double x = poly[i], y = poly[i + 1];
                long cx = Math.round(x / cellSize);
                long cy = Math.round(y / cellSize);

                Integer match = null;
                outer: for (long dx = -1; dx <= 1; dx++) {
                    for (long dy = -1; dy <= 1; dy++) {
                        long key = ((cx + dx) * 1000003L) ^ (cy + dy);
                        List<Integer> bucket = grid.get(key);
                        if (bucket == null)
                            continue;
                        for (int cand : bucket) {
                            Pt c = canonicalPoints.get(cand);
                            if (Math.abs(c.x - x) <= SNAP_EPS && Math.abs(c.y - y) <= SNAP_EPS) {
                                match = cand;
                                break outer;
                            }
                        }
                    }
                }

                if (match != null) {
                    canonicalIndex[idx] = match;
                } else {
                    int newIdx = canonicalPoints.size();
                    canonicalPoints.add(new Pt(x, y));
                    long key = (cx * 1000003L) ^ cy;
                    grid.computeIfAbsent(key, k -> new ArrayList<>()).add(newIdx);
                    canonicalIndex[idx] = newIdx;
                }
                idx++;
            }
        }
        return canonicalIndex;
    }

    // ---------- Step 2: split every edge at any other vertex lying on it
    // (T-junctions) ----------

    private static List<Integer> splitRingAtTJunctions(List<Integer> ring, List<Pt> pts) {
        int n = ring.size();
        List<Integer> expanded = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int aIdx = ring.get(i);
            int bIdx = ring.get((i + 1) % n);
            expanded.add(aIdx);
            if (aIdx == bIdx)
                continue;

            Pt a = pts.get(aIdx), b = pts.get(bIdx);
            double dx = b.x - a.x, dy = b.y - a.y;
            double lenSq = dx * dx + dy * dy;
            if (lenSq == 0)
                continue;

            // Find every other point that lies strictly on segment a-b
            List<double[]> onEdge = new ArrayList<>(); // {t, pointIndex}
            for (int p = 0; p < pts.size(); p++) {
                if (p == aIdx || p == bIdx)
                    continue;
                Pt c = pts.get(p);

                double t = ((c.x - a.x) * dx + (c.y - a.y) * dy) / lenSq;
                if (t <= T_EPS || t >= 1 - T_EPS)
                    continue; // not strictly between endpoints

                // perpendicular distance from c to the line through a-b
                double projX = a.x + t * dx, projY = a.y + t * dy;
                double distSq = (c.x - projX) * (c.x - projX) + (c.y - projY) * (c.y - projY);
                if (distSq <= SNAP_EPS * SNAP_EPS) {
                    onEdge.add(new double[] { t, p });
                }
            }

            if (!onEdge.isEmpty()) {
                onEdge.sort((x, y) -> Double.compare(x[0], y[0]));
                for (double[] entry : onEdge) {
                    expanded.add((int) entry[1]);
                }
            }
        }
        return expanded;
    }

    // ---------- Step 3: merge (edge cancellation + ring tracing) ----------

    public static List<double[]> mergePolygons(List<double[]> polygons) {

        List<Pt> canonicalPoints = new ArrayList<>();
        int[] canonicalIndex = snapVertices(polygons, canonicalPoints);

        Map<EdgeKey, Integer> netCount = new HashMap<>();
        Map<EdgeKey, int[]> edgeEndpoints = new HashMap<>();

        int cursor = 0;
        for (double[] poly : polygons) {
            int n = poly.length / 2;
            List<Integer> ring = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
                ring.add(canonicalIndex[cursor++]);

            if (isClockwise(ring, canonicalPoints))
                Collections.reverse(ring);

            // Expand the ring so any T-junction vertices from other polygons are included
            ring = splitRingAtTJunctions(ring, canonicalPoints);

            int m = ring.size();
            for (int i = 0; i < m; i++) {
                int a = ring.get(i);
                int b = ring.get((i + 1) % m);
                if (a == b)
                    continue;

                boolean forward = a <= b;
                int lo = forward ? a : b;
                int hi = forward ? b : a;
                EdgeKey key = new EdgeKey(lo, hi);

                edgeEndpoints.putIfAbsent(key, new int[] { lo, hi });
                netCount.merge(key, forward ? 1 : -1, Integer::sum);
            }
        }

        Map<Integer, Deque<Integer>> adjacency = new HashMap<>();
        for (Map.Entry<EdgeKey, Integer> e : netCount.entrySet()) {
            int net = e.getValue();
            if (net == 0)
                continue;
            int[] ends = edgeEndpoints.get(e.getKey());
            int from = net > 0 ? ends[0] : ends[1];
            int to = net > 0 ? ends[1] : ends[0];
            for (int i = 0; i < Math.abs(net); i++) {
                adjacency.computeIfAbsent(from, k -> new ArrayDeque<>()).add(to);
            }
        }

        List<double[]> result = new ArrayList<>();
        for (Integer start : new ArrayList<>(adjacency.keySet())) {
            Deque<Integer> outs = adjacency.get(start);
            while (outs != null && !outs.isEmpty()) {
                List<Integer> ring = new ArrayList<>();
                int current = start;
                ring.add(current);
                boolean closed = false;
                while (true) {
                    Deque<Integer> currentOuts = adjacency.get(current);
                    if (currentOuts == null || currentOuts.isEmpty()) {
                        Pt stranded = canonicalPoints.get(current);
                        System.err.printf("WARNING: ring did not close — dead end at (%.6f, %.6f).%n", stranded.x,
                                stranded.y);
                        break;
                    }
                    int next = currentOuts.poll();
                    if (next == start) {
                        closed = true;
                        break;
                    }
                    ring.add(next);
                    current = next;
                }
                if (closed && ring.size() >= 3) {
                    result.add(toArray(ring, canonicalPoints));
                }
                outs = adjacency.get(start);
            }
        }

        return result;
    }

    private static boolean isClockwise(List<Integer> ring, List<Pt> pts) {
        double sum = 0;
        int n = ring.size();
        for (int i = 0; i < n; i++) {
            Pt a = pts.get(ring.get(i));
            Pt b = pts.get(ring.get((i + 1) % n));
            sum += (b.x - a.x) * (b.y + a.y);
        }
        return sum > 0;
    }

    private static double[] toArray(List<Integer> ring, List<Pt> pts) {
        int n = ring.size();
        double[] arr = new double[(n + 1) * 2]; // +1 to re-close the ring
        for (int i = 0; i < n; i++) {
            Pt p = pts.get(ring.get(i));
            arr[2 * i] = p.x;
            arr[2 * i + 1] = p.y;
        }
        // repeat the first vertex to explicitly close the ring
        Pt first = pts.get(ring.get(0));
        arr[2 * n] = first.x;
        arr[2 * n + 1] = first.y;
        return arr;
    }

}
