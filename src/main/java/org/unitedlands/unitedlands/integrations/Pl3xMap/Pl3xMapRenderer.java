package org.unitedlands.unitedlands.integrations.Pl3xMap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.classes.interfaces.CoordinateHolder;
import org.unitedlands.unitedlands.classes.map.LayerOptions;
import org.unitedlands.unitedlands.integrations.map.MapRenderer;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.unitedlands.utils.PolygonUtils;
import org.unitedlands.utils.United;

import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.image.IconImage;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.Vector;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Icon;
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.markers.marker.MultiPolygon;
import net.pl3x.map.core.markers.marker.Polygon;
import net.pl3x.map.core.markers.marker.Polyline;
import net.pl3x.map.core.markers.option.Fill;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.markers.option.Popup;

public class Pl3xMapRenderer implements MapRenderer {

    private static Pl3xMapRenderer instance;

    public static Pl3xMapRenderer instance() {
        return instance;
    }

    private final int BATCH_SIZE = 150;
    private final long FREQUENCY = 20l;

    private final Set<Settlement> settlementQueue = new LinkedHashSet<>();
    private final Set<Region> regionQueue = new LinkedHashSet<>();
    private final Set<Country> countryQueue = new LinkedHashSet<>();

    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "unitedlands-map-render");
        t.setDaemon(true);
        return t;
    });

    private BukkitTask queueCheckTask;

    private String settlementMapInfoBox;
    // private String regionMapInfoBox;
    private String countryMapInfoBox;

    private boolean debugMode = false;

    public Pl3xMapRenderer() {
        instance = this;
    }

    private enum DIRECTION {
        RIGHT, DOWN, UP, LEFT
    };

    private static class ChunkCluster {
        private Set<CoordinateHolder> chunks = new HashSet<>();
    }

    private record MapChunk(Coordinates coordinates) implements CoordinateHolder {

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

    private record SettlementSnapshot(UUID uuid, String worldName, String name, int fillColor, int strokeColor,
            int strokeWidth, String dash, String infoBox, Set<MapChunk> chunks) {
    }

    private record RegionSnapshot(UUID uuid, String worldName, boolean claimed, boolean claiming,
            Coordinates homeChunk, String tooltip, int fillColor, int strokeColor, int strokeWidth, String dash,
            double[] polygon) {
    }

    private record CountrySnapshot(UUID uuid, String worldName, String name, int strokeColor, int strokeWidth,
            int offset, Point iconPosition, String infoBox, List<double[]> polygons) {
    }

    public void initialize() {
        registerIcon("region-center", "region-center.png");
        registerIcon("claiming", "claiming.png");
        registerIcon("fight", "fight.png");
        registerIcon("siege", "siege.png");
        registerIcon("fortress", "fortress.png");
        registerIcon("home", "home.png");
        registerIcon("camp", "camp.png");
        loadMessages();
    }

    public void loadMessages() {
        settlementMapInfoBox = UnitedLandsConfig.get().mapInfoBoxes().settlement();
        countryMapInfoBox = UnitedLandsConfig.get().mapInfoBoxes().country();
    }

    public void shutdown() {
        if (queueCheckTask != null) {
            queueCheckTask.cancel();
            queueCheckTask = null;
        }
        settlementQueue.clear();
        regionQueue.clear();
        countryQueue.clear();
        renderExecutor.shutdownNow();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void registerIcon(String key, String filename) {
        File importFolder = new File(UnitedLands.instance().getDataFolder(), "icons");
        File imageFile = new File(importFolder, filename);

        BufferedImage img;
        try {
            img = ImageIO.read(imageFile);
        } catch (Exception ex) {
            United.logger().error("Failed to read image " + filename, "UnitedLands");
            return;
        }

        IconImage iconImage = new IconImage(key, img, "png");
        Pl3xMap.api().getIconRegistry().register(key, iconImage);
    }

    public void checkQueue() {

        var settlements = snapshotBatch(settlementQueue, this::snapshot, "settlement");
        var regions = snapshotBatch(regionQueue, region -> snapshot(region, debugMode), "region");
        var countries = snapshotBatch(countryQueue, this::snapshot, "country");

        if (!settlements.isEmpty() || !regions.isEmpty() || !countries.isEmpty())
            render(settlements, regions, countries);

        if (settlementQueue.isEmpty() && regionQueue.isEmpty() && countryQueue.isEmpty()) {
            queueCheckTask.cancel();
            queueCheckTask = null;
            United.logger().info("Stopped map rendering task.", "UnitedLands");
        }

    }

    private <T, S> List<S> snapshotBatch(Set<T> queue, Function<T, S> snapshotter, String type) {
        List<S> snapshots = new ArrayList<>();
        Iterator<T> iterator = queue.iterator();
        int count = 0;
        while (iterator.hasNext() && count < BATCH_SIZE) {
            T item = iterator.next();
            iterator.remove();
            count++;
            try {
                snapshots.add(snapshotter.apply(item));
            } catch (Exception ex) {
                United.logger().error("Could not prepare " + type + " for map rendering: " + ex.getMessage(),
                        "UnitedLands");
            }
        }
        return snapshots;
    }

    private void render(List<SettlementSnapshot> settlements, List<RegionSnapshot> regions,
            List<CountrySnapshot> countries) {

        var startTime = System.currentTimeMillis();

        CompletableFuture.supplyAsync(() -> {
            List<Runnable> actions = new ArrayList<>();
            for (var settlement : settlements)
                build(actions, () -> buildSettlement(settlement), "settlement " + settlement.uuid());
            for (var region : regions)
                build(actions, () -> buildRegion(region), "region " + region.uuid());
            for (var country : countries)
                build(actions, () -> buildCountry(country), "country " + country.uuid());
            return actions;
        }, renderExecutor).thenAccept(actions -> runOnMainThread(() -> {
            for (var action : actions) {
                try {
                    action.run();
                } catch (Exception ex) {
                    United.logger().error("Error applying map marker: " + ex.getMessage(), "UnitedLands");
                }
            }
            United.logger().info("Rendered " + settlements.size() + " settlements, " + regions.size()
                    + " regions and " + countries.size() + " countries in "
                    + (System.currentTimeMillis() - startTime) + "ms", "UnitedLands");
        })).exceptionally(ex -> {
            United.logger().error("Error rendering map: " + ex.getMessage(), "UnitedLands");
            return null;
        });
    }

    private void build(List<Runnable> actions, Supplier<Runnable> builder, String description) {
        try {
            actions.add(builder.get());
        } catch (Exception ex) {
            United.logger().error("Error rendering " + description + ": " + ex.getMessage(), "UnitedLands");
        }
    }

    private void runOnMainThread(Runnable runnable) {
        var plugin = UnitedLands.instance();
        if (!plugin.isEnabled())
            return;
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void addToRenderQueue(Settlement settlement) {
        settlementQueue.add(settlement);
        startQueueMonitor();
    }

    public void addToRenderQueue(Region region) {
        regionQueue.add(region);
        startQueueMonitor();
    }

    public void addToRenderQueue(Country country) {
        countryQueue.add(country);
        startQueueMonitor();
    }

    public void addSettlementsToRenderQueue(Collection<Settlement> settlements) {
        settlementQueue.addAll(settlements);
        startQueueMonitor();
    }

    public void addRegionsToRenderQueue(Collection<Region> regions) {
        regionQueue.addAll(regions);
        startQueueMonitor();
    }

    public void addCountriesToRenderQueue(Collection<Country> countries) {
        countryQueue.addAll(countries);
        startQueueMonitor();
    }

    private void startQueueMonitor() {
        if (queueCheckTask == null) {
            United.logger().info("Started map rendering task.", "UnitedLands");
            queueCheckTask = Bukkit.getScheduler().runTaskTimer(UnitedLands.instance(), () -> {
                checkQueue();
            }, FREQUENCY, FREQUENCY);
        }
    }

    // #region Country rendering

    // *********************************************************
    // Marker rendering
    // *********************************************************

    public void renderMarker(String worldName, Location location, String markerName, String markerKey, String layerId,
            String layerName) {
        SimpleLayer layer = getOrCreateSimpleLayer(worldName, layerId, layerName, 1, 490);
        if (layer == null)
            return;
        removeIfPresent(layer, markerKey);
        var position = new Point((int) location.getX(), (int) location.getZ());
        var marker = Marker.icon(markerKey, position, markerName);
        marker.setSize(new Vector(16d, 16d));
        marker.setAnchor(new Vector(8d, 8d));
        layer.addMarker(marker);
    }

    public void removeMarker(String worldName, String markerKey, String layerId, String layerName) {
        removeMarkerFromLayer(worldName, layerId, markerKey);
    }

    // #endregion

    // #region Country rendering

    // *********************************************************
    // Country rendering
    // *********************************************************

    private CountrySnapshot snapshot(Country country) {

        int strokeWidth = UnitedLandsConfig.get().mapRendering().get("country-default").width();

        List<double[]> polygons = new ArrayList<>();
        country.getRegions().stream().map(Region::getPolygon).filter(Objects::nonNull).forEach(polygons::add);
        var vassals = UnitedLandsDataManager.instance().getCountryVassals(country);
        if (vassals.size() > 0) {
            strokeWidth *= 1.5;
            for (var vassal : vassals) {
                vassal.getRegions().stream().map(Region::getPolygon).filter(Objects::nonNull)
                        .forEach(polygons::add);
            }
        }

        int offset = country.getOverlord() == null ? -1 * strokeWidth : -1 * strokeWidth * 3;

        Point iconPosition = null;
        if (country.getIconPosition() != null) {
            var pos = country.getIconPosition();
            iconPosition = new Point((int) pos.getX(), (int) pos.getZ());
        }

        return new CountrySnapshot(country.getUuid(), country.getWorldName(), country.getCleanName(),
                country.getStrokeColor(), strokeWidth, offset, iconPosition, getCountryInfoBox(country), polygons);
    }

    private Runnable buildCountry(CountrySnapshot country) {

        var uuid = country.uuid().toString();
        var key = "country-" + uuid;
        var iconKey = "country-icon-" + uuid;

        // Icon rendering

        Icon iconMarker = null;
        if (country.iconPosition() != null) {
            iconMarker = Marker.icon(iconKey, country.iconPosition(), uuid).setAnchor(Vector.of(16.0, 16.0));
        }

        // Border rendering

        var mergedPolygons = mergePolygons(country.polygons());

        List<Polygon> finalPolygons = new ArrayList<>();

        var markerOptions = Options.builder().fill(true).fillColor(0).stroke(true).strokeColor(country.strokeColor())
                .strokeWeight(country.strokeWidth())
                .tooltipContent(country.name()).build().setPopup(new Popup(country.infoBox()));

        int i = 0;
        for (var mergedPolygon : mergedPolygons) {
            double[] offsetPolygon = PolygonUtils.offsetPolygon(mergedPolygon, country.offset());
            LinkedList<Point> polygonPoints = new LinkedList<>();
            for (int j = 0; j < mergedPolygon.length - 2; j = j + 2) {
                // polygonPoints.add(new Point((int) mergedPolygon[j], (int) mergedPolygon[j +
                // 1]));
                polygonPoints.add(new Point((int) offsetPolygon[j], (int) offsetPolygon[j + 1]));
            }
            finalPolygons.add(
                    new Polygon(key + "-" + i, new Polyline("border-" + uuid + "-" + i, polygonPoints)));
            i++;
        }

        MultiPolygon mapPolygon = new MultiPolygon(key, finalPolygons);
        mapPolygon.setOptions(markerOptions);

        var finalIconMarker = iconMarker;
        return () -> {
            if (UnitedLandsDataManager.instance().getCountry(country.uuid()) == null)
                return;

            var layer = getCountryLayer(country.worldName());
            var iconLayer = getCountryIconLayer(country.worldName());
            if (layer == null || iconLayer == null)
                return;

            removeIfPresent(iconLayer, iconKey);
            if (finalIconMarker != null)
                iconLayer.addMarker(finalIconMarker);

            removeIfPresent(layer, key);
            layer.addMarker(mapPolygon);
        };
    }

    public void removeCountry(Country country) {
        removeMarkerFromLayer(country.getWorldName(), "countries", "country-" + country.getUuid().toString());
        removeCountryIcon(country);
    }

    public void removeCountryIcon(Country country) {
        removeMarkerFromLayer(country.getWorldName(), "countryicons", "country-icon-" + country.getUuid().toString());
    }

    private String getCountryInfoBox(Country country) {
        var infoBox = countryMapInfoBox;

        if (Pl3xMap.api().getIconRegistry().has(country.getUuid().toString())) {
            infoBox = infoBox.replace("%icon%", "images/icon/registered/" + country.getUuid().toString() + ".png");
        } else {
            infoBox = infoBox.replace("%icon%", "images/clear.png");
        }
        infoBox = infoBox.replace("%country%", country.getCleanName());
        infoBox = infoBox.replace("%leader%", country.getLeader() != null ? country.getLeader().getName() : "NPC");
        infoBox = infoBox.replace("%capital%", country.hasCapital() ? country.getCapital().getCleanName() : "");
        infoBox = infoBox.replace("%founded%",
                new SimpleDateFormat("dd MMM yyyy").format(country.getFoundingTimestamp()));
        infoBox = infoBox.replace("%balance%", UnitedLandsEconomyManager.instance()
                .format(UnitedLandsEconomyManager.instance().getBalance(country.getUuid())));
        infoBox = infoBox.replace("%settlements%", String.valueOf(country.getSettlementCount()));
        infoBox = infoBox.replace("%regions%", String.valueOf(country.getRegionCount()));
        infoBox = infoBox.replace("%citizens%", String.valueOf(country.getCitizenCount()));
        infoBox = infoBox.replace("%upkeep%", UnitedLandsEconomyManager.instance().format(country.getUpkeep()));
        infoBox = infoBox.replace("%size%", String.format("%,.0f", country.getArea()));
        return infoBox;
    }

    // #endregion

    // #region Region rendering

    // *********************************************************
    // Region rendering
    // *********************************************************

    private RegionSnapshot snapshot(Region region, boolean debug) {

        String dash = UnitedLandsConfig.get().mapRendering().get("region-default").dash();
        int strokeWidth = UnitedLandsConfig.get().mapRendering().get("region-default").width();

        if (region.hasCountry()) {
            dash = UnitedLandsConfig.get().mapRendering().get("region-in-country").dash();
            strokeWidth = UnitedLandsConfig.get().mapRendering().get("region-in-country").width();
        }

        int fillColor;
        int strokeColor;
        if (debug) {
            fillColor = region.getDebugFillColor();
            strokeColor = region.getDebugStrokeColor();
            strokeWidth = 2;
            dash = "99999";
        } else {
            fillColor = region.getFillColor();
            strokeColor = region.getStrokeColor();
        }

        var tooltip = region.getCleanName()
                + (region.hasCountry() ? " (" + region.getCountry().getCleanName() + ")" : "")
                + (region.isCoastal() ? " &#127754;" : "")
                + (region.isIsland() ? " &#9875;" : "");

        return new RegionSnapshot(region.getUuid(), region.getWorldName(), region.hasCountry(),
                region.getClaimEndTime() != null, region.getHomeChunkCoordinates().clone(), tooltip, fillColor,
                strokeColor, strokeWidth, dash, region.getPolygon());
    }

    private Runnable buildRegion(RegionSnapshot region) {

        var key = "region-" + region.uuid().toString();
        // var centerMarkerKey = "center-" + region.getUuid();
        // if (regionInfoMarkerLayer.hasMarker(centerMarkerKey))
        //     regionInfoMarkerLayer.removeMarker(centerMarkerKey);
        var claimMarkerKey = "claim-" + region.uuid();

        var centerWordLocation = CoordinateUtils.chunkToWorldCoordinates(region.homeChunk());
        var centerMarkerPosition = new Point(centerWordLocation.getX() + 8, centerWordLocation.getZ() + 8);

        // var centerMarker = Marker.icon(centerMarkerKey, centerMarkerPosition, "region-center");
        // regionInfoMarkerLayer.addMarker(centerMarker);

        Icon claimMarker = null;
        if (region.claiming()) {
            claimMarker = Marker.icon(claimMarkerKey, centerMarkerPosition, "claiming");
        }

        // var popup = new Popup("<div><p><strong>" + region.getCleanName() +
        // "</strong></p><p><strong>Owner: </strong>"
        // + (region.hasFounder() ? region.getFounderName() : "-") + "</p></div>");

        var markerOptions = Options.builder().fill(true).fillType(Fill.Type.NONZERO).fillColor(region.fillColor())
                .stroke(true)
                .strokeColor(region.strokeColor())
                .strokeDashPattern(region.dash()).strokeWeight(region.strokeWidth())
                .tooltipContent(region.tooltip()).build();

        Polygon polygon = null;
        var polyPoints = region.polygon();
        if (polyPoints != null) {
            LinkedList<Point> points = new LinkedList<>();
            for (int i = 0; i < polyPoints.length - 2; i = i + 2) {
                points.add(new Point((int) polyPoints[i], (int) polyPoints[i + 1]));
            }

            polygon = new Polygon(key, new Polyline("border-" + region.uuid().toString(), points));
            polygon.setOptions(markerOptions);
        }

        var finalClaimMarker = claimMarker;
        var finalPolygon = polygon;
        return () -> {
            if (UnitedLandsDataManager.instance().getRegion(region.uuid()) == null)
                return;

            var unclaimedLayer = getUnclaimedRegionLayer(region.worldName());
            var claimedLayer = getClaimedRegionLayer(region.worldName());
            var regionInfoMarkerLayer = getRegionInfoLayer(region.worldName());
            if (unclaimedLayer == null || claimedLayer == null || regionInfoMarkerLayer == null)
                return;

            removeIfPresent(unclaimedLayer, key);
            removeIfPresent(claimedLayer, key);
            if (finalPolygon != null)
                (region.claimed() ? claimedLayer : unclaimedLayer).addMarker(finalPolygon);

            removeIfPresent(regionInfoMarkerLayer, claimMarkerKey);
            if (finalClaimMarker != null)
                regionInfoMarkerLayer.addMarker(finalClaimMarker);
        };
    }

    public void removeRegion(Region region) {
        String key = "region-" + region.getUuid().toString();
        removeMarkerFromLayer(region.getWorldName(), "unclaimedregions", key);
        removeMarkerFromLayer(region.getWorldName(), "claimedregions", key);
        removeMarkerFromLayer(region.getWorldName(), "regioninfos", "claim-" + region.getUuid());
    }

    // #region Settlement rendering

    // *********************************************************
    // Settlement rendering
    // *********************************************************

    private SettlementSnapshot snapshot(Settlement settlement) {

        var style = settlement.hasCountry() ? "settlement-in-country" : "settlement-default";
        int strokeWidth = UnitedLandsConfig.get().mapRendering().get(style).width();
        String dash = UnitedLandsConfig.get().mapRendering().get(style).dash();

        Set<MapChunk> chunks = settlement.getChunks().stream()
                .map(c -> new MapChunk(c.getCoordinates().clone()))
                .collect(Collectors.toSet());

        return new SettlementSnapshot(settlement.getUuid(), settlement.getWorldName(), settlement.getCleanName(),
                settlement.getFillColor(), settlement.getStrokeColor(), strokeWidth, dash,
                getSettlementInfoBox(settlement), chunks);
    }

    private Runnable buildSettlement(SettlementSnapshot settlement) {

        String uuid = settlement.uuid().toString();
        String key = "settlement-" + uuid;

        var markerOptions = Options.builder().fill(true).fillType(Fill.Type.EVENODD)
                .fillColor(settlement.fillColor()).stroke(true)
                .strokeColor(settlement.strokeColor()).strokeWeight(settlement.strokeWidth())
                .strokeDashPattern(settlement.dash())
                .tooltipContent(settlement.name()).build()
                .setPopup(new Popup(settlement.infoBox()));

        var chunkClusters = findClusters(settlement.chunks());
        List<Polygon> polygons = new ArrayList<>();

        int i = 0;
        for (var cluster : chunkClusters) {

            List<Polyline> clusterLines = new ArrayList<>();

            clusterLines.add(new Polyline("border-" + uuid + "-" + i, generatePolygon(cluster.chunks)));

            var holeClusters = findHoles(cluster, settlement.worldName());
            int j = 0;
            for (var holeCluster : holeClusters) {
                clusterLines.add(
                        new Polyline("hole-" + uuid + "-" + i + "-" + j, generatePolygon(holeCluster.chunks)));
                j++;
            }

            polygons.add(new Polygon("cluster-" + uuid + "-" + i, clusterLines));
            i++;
        }
        MultiPolygon mapPolygon = new MultiPolygon(key, polygons);
        mapPolygon.setOptions(markerOptions);

        return () -> {
            if (UnitedLandsDataManager.instance().getSettlement(settlement.uuid()) == null)
                return;

            var layer = getSettlementLayer(settlement.worldName());
            if (layer == null)
                return;

            removeIfPresent(layer, key);
            layer.addMarker(mapPolygon);
        };
    }

    public void removeSettlement(Settlement settlement) {
        removeMarkerFromLayer(settlement.getWorldName(), "settlements",
                "settlement-" + settlement.getUuid().toString());
    }

    private String getSettlementInfoBox(Settlement settlement) {

        var infoBox = settlementMapInfoBox;

        infoBox = infoBox.replace("%settlement%", settlement.getCleanName());
        infoBox = infoBox.replace("%mayor%", settlement.getMayor() != null ? settlement.getMayor().getName() : "NPC");
        infoBox = infoBox.replace("%status%", settlement.hasCountry() ? (settlement.isCapital() ? "Capital of" : "Member of") : "");
        infoBox = infoBox.replace("%country%", settlement.hasCountry() ? settlement.getCountry().getCleanName() : "Independent");
        infoBox = infoBox.replace("%citizens%", String.valueOf(settlement.getCitizenCount()));
        infoBox = infoBox.replace("%founded%",
                new SimpleDateFormat("dd MMM yyyy").format(settlement.getFoundingTimestamp()));
        infoBox = infoBox.replace("%balance%", UnitedLandsEconomyManager.instance()
                .format(UnitedLandsEconomyManager.instance().getBalance(settlement.getUuid())));
        infoBox = infoBox.replace("%upkeep%", UnitedLandsEconomyManager.instance().format(settlement.getUpkeep()));
        infoBox = infoBox.replace("%size%", String.format("%,d", settlement.getChunks().size()));
        infoBox = infoBox.replace("%board%", settlement.getTownBoard() != null ? settlement.getTownBoard() : "");

        return infoBox;
    }

    // #endregion

    // #region Helpers & Utilities

    // *******************************
    // Chunk cluster detections
    // *******************************

    public Set<ChunkCluster> findClusters(Set<? extends CoordinateHolder> chunks) {
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

    private String toKey(Coordinates c) {
        return toKey(c.getX(), c.getZ(), c.getWorldName());
    }

    private String toKey(int x, int z, String world) {
        return x + "," + z + "," + world;
    }

    // *********************************************
    // Negative space detection
    // *********************************************

    public Set<ChunkCluster> findHoles(ChunkCluster cluster, String worldName) {
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

    private List<Point> generatePolygon(Set<CoordinateHolder> chunkSet) {

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
                    if (isChunkAtCoordinates(chunks, upCoords)) {
                        chunksToVisit.add(getChunkAtCoordinates(chunks, upCoords));
                        currDir = DIRECTION.UP;
                        polygon.add(chunk.getUpperLeft());
                        // Check if there's a chunk to the right
                    } else if (isChunkAtCoordinates(chunks, rightCoords)) {
                        chunksToVisit.add(getChunkAtCoordinates(chunks, rightCoords));
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
                    if (isChunkAtCoordinates(chunks, downCoords)) {
                        chunksToVisit.add(getChunkAtCoordinates(chunks, downCoords));
                        currDir = DIRECTION.DOWN;
                        polygon.add(chunk.getLowerRight());
                        // Check if there's a chunk to the right
                    } else if (isChunkAtCoordinates(chunks, leftCoords)) {
                        chunksToVisit.add(getChunkAtCoordinates(chunks, leftCoords));
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
                    if (isChunkAtCoordinates(chunks, rightCoords)) {
                        chunksToVisit.add(getChunkAtCoordinates(chunks, rightCoords));
                        currDir = DIRECTION.RIGHT;
                        polygon.add(chunk.getUpperRight());
                        // Check if there's a chunk below
                    } else if (isChunkAtCoordinates(chunks, downCoords)) {
                        chunksToVisit.add(getChunkAtCoordinates(chunks, downCoords));
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
                    if (isChunkAtCoordinates(chunks, leftCoords)) {
                        chunksToVisit.add(getChunkAtCoordinates(chunks, leftCoords));
                        currDir = DIRECTION.LEFT;
                        polygon.add(chunk.getLowerLeft());
                        // Check if there's a chunk below
                    } else if (isChunkAtCoordinates(chunks, upCoords)) {
                        chunksToVisit.add(getChunkAtCoordinates(chunks, upCoords));
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

        List<Point> points = new ArrayList<>();
        for (var p : polygon) {
            points.add(new Point(p.getX(), p.getZ()));
        }
        return points;
    }

    // *******************************
    // Helpers
    // *******************************

    private SimpleLayer getOrCreateSimpleLayer(String world, String key, String name, int priority, int zindex) {
        return getOrCreateSimpleLayer(world, key, name, priority, zindex, true);
    }

    private SimpleLayer getOrCreateSimpleLayer(String world, String key, String name, int priority, int zindex,
            boolean setPane) {

        if (world == null)
            return null;

        net.pl3x.map.core.world.World mapworld = Pl3xMap.api().getWorldRegistry().get(world);
        if (mapworld == null)
            return null;

        SimpleLayer layer = (SimpleLayer) mapworld.getLayerRegistry().get(key);

        if (layer == null) {
            var options = new LayerOptions(name, true, false, priority, zindex);
            layer = new SimpleLayer(key, options::getName);
            layer.setDefaultHidden(options.isDefaultHidden());
            layer.setPriority(options.getLayerPriority());
            if (setPane)
                layer.setPane(key.replace("-", ""));
            layer.setZIndex(options.getZIndex());
            layer.setShowControls(options.showControls());
            mapworld.getLayerRegistry().register(layer);
        }

        return layer;
    }

    private SimpleLayer getSettlementLayer(String world) {
        return getOrCreateSimpleLayer(world, "settlements", "Settlements", 1, 480);
    }

    private SimpleLayer getCountryLayer(String world) {
        return getOrCreateSimpleLayer(world, "countries", "Countries", 1, 460);
    }

    private SimpleLayer getCountryIconLayer(String world) {
        return getOrCreateSimpleLayer(world, "countryicons", "Country Icons", 1, 465);
    }

    private SimpleLayer getUnclaimedRegionLayer(String world) {
        return getOrCreateSimpleLayer(world, "unclaimedregions", "Unclaimed Regions", 8, 450, false);
    }

    private SimpleLayer getClaimedRegionLayer(String world) {
        return getOrCreateSimpleLayer(world, "claimedregions", "Claimed Regions", 6, 450, false);
    }

    private SimpleLayer getRegionInfoLayer(String world) {
        return getOrCreateSimpleLayer(world, "regioninfos", "Region Infos", 10, 100);
    }

    private void removeIfPresent(SimpleLayer layer, String markerKey) {
        if (layer.hasMarker(markerKey))
            layer.removeMarker(markerKey);
    }

    private void removeMarkerFromLayer(String world, String layerKey, String markerKey) {
        if (world == null)
            return;
        net.pl3x.map.core.world.World mapworld = Pl3xMap.api().getWorldRegistry().get(world);
        if (mapworld == null)
            return;
        if (mapworld.getLayerRegistry().get(layerKey) instanceof SimpleLayer layer)
            removeIfPresent(layer, markerKey);
    }

    private boolean isChunkAtCoordinates(Map<Coordinates, CoordinateHolder> chunks, Coordinates coords) {
        return chunks.containsKey(coords);
    }

    private CoordinateHolder getChunkAtCoordinates(Map<Coordinates, CoordinateHolder> chunks, Coordinates coords) {
        return chunks.get(coords);
    }

    private CoordinateHolder findRightmostChunk(Set<CoordinateHolder> chunks) {
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

    private final double SNAP_EPS = 1e-6;
    private final double T_EPS = 1e-9; // parametric tolerance for "strictly between" endpoints

    private final class Pt {
        double x, y;

        Pt(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private final class EdgeKey {
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

    private int[] snapVertices(List<double[]> polygons, List<Pt> canonicalPoints) {
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

    private List<Integer> splitRingAtTJunctions(List<Integer> ring, List<Pt> pts) {
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

    public List<double[]> mergePolygons(List<double[]> polygons) {

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

    private boolean isClockwise(List<Integer> ring, List<Pt> pts) {
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
