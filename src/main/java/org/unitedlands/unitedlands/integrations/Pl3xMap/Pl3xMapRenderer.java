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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.classes.interfaces.CoordinateHolder;
import org.unitedlands.unitedlands.classes.map.LayerOptions;
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
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.markers.marker.MultiPolygon;
import net.pl3x.map.core.markers.marker.Polygon;
import net.pl3x.map.core.markers.marker.Polyline;
import net.pl3x.map.core.markers.option.Fill;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.markers.option.Popup;

public class Pl3xMapRenderer {

    private static Pl3xMapRenderer instance;

    public static Pl3xMapRenderer instance() {
        return instance;
    }

    private final int BATCH_SIZE = 50;
    private final long FREQUENCY = 20l;

    private Deque<Settlement> settlementQueue = new LinkedList<Settlement>();
    private Deque<Region> regionQueue = new LinkedList<Region>();
    private Deque<Country> countryQueue = new LinkedList<Country>();

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
        settlementMapInfoBox = United.messenger().get("map-info-boxes.settlement");
        countryMapInfoBox = United.messenger().get("map-info-boxes.country");
    }

    public void shutdown() {
        if (queueCheckTask != null) {
            queueCheckTask.cancel();
        }
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

        if (settlementQueue.size() > 0) {
            List<Settlement> settlementRenderList = new ArrayList<>();
            while (!settlementQueue.isEmpty() && settlementRenderList.size() < BATCH_SIZE) {
                settlementRenderList.add(settlementQueue.poll()); // Removes from the front
            }
            renderSettlements(settlementRenderList);
        }

        if (regionQueue.size() > 0) {
            List<Region> regionRenderList = new ArrayList<>();
            while (!regionQueue.isEmpty() && regionRenderList.size() < BATCH_SIZE) {
                regionRenderList.add(regionQueue.poll()); // Removes from the front
            }
            renderPolyRegions(regionRenderList, debugMode);
        }

        if (countryQueue.size() > 0) {
            List<Country> countryRenderList = new ArrayList<>();
            while (!countryQueue.isEmpty() && countryRenderList.size() < BATCH_SIZE) {
                countryRenderList.add(countryQueue.poll()); // Removes from the front
            }
            renderCountries(countryRenderList);
        }

        if (settlementQueue.size() == 0 && regionQueue.size() == 0 && countryQueue.size() == 0) {
            queueCheckTask.cancel();
            queueCheckTask = null;
            United.logger().info("Stopped map rendering task.", "UnitedLands");
        }

    }

    public void addToRenderQueue(Settlement settlement) {
        if (!settlementQueue.contains(settlement))
            settlementQueue.addLast(settlement);
        startQueueMonitor();
    }

    public void addToRenderQueue(Region region) {
        if (!regionQueue.contains(region))
            regionQueue.addLast(region);
        startQueueMonitor();
    }

    public void addToRenderQueue(Country country) {
        if (!countryQueue.contains(country))
            countryQueue.addLast(country);
        startQueueMonitor();
    }

    public void addSettlementsToRenderQueue(Collection<Settlement> settlements) {
        for (var settlement : settlements)
            if (!settlementQueue.contains(settlement))
                settlementQueue.addLast(settlement);
        startQueueMonitor();
    }

    public void addRegionsToRenderQueue(Collection<Region> regions) {
        for (var region : regions)
            if (!regionQueue.contains(region))
                regionQueue.addLast(region);
        startQueueMonitor();
    }

    public void addCountriesToRenderQueue(Collection<Country> countries) {
        for (var country : countries)
            if (!countryQueue.contains(country))
                countryQueue.addLast(country);
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
        if (layer.hasMarker(markerKey))
            layer.removeMarker(markerKey);
        var position = new Point((int) location.getX(), (int) location.getZ());
        var marker = Marker.icon(markerKey, position, markerName);
        marker.setSize(new Vector(16d, 16d));
        marker.setAnchor(new Vector(8d, 8d));
        layer.addMarker(marker);
    }

    public void removeMarker(String worldName, String markerKey, String layerId, String layerName) {
        SimpleLayer layer = getOrCreateSimpleLayer(worldName, layerId, layerName, 1, 490);
        if (layer.hasMarker(markerKey))
            layer.removeMarker(markerKey);
    }

    // #endregion

    // #region Country rendering

    // *********************************************************
    // Country rendering
    // *********************************************************

    private void renderCountries(Collection<Country> countries) {

        United.logger().info("Starting country map rendering...", "UnitedLands");

        if (countries == null || countries.isEmpty()) {
            United.logger().info("No countries to render.", "UnitedLands");
            return;
        }

        var startTime = System.currentTimeMillis();
        List<CompletableFuture<Void>> futures = countries.stream()
                .map(country -> CompletableFuture.runAsync(() -> renderCountryAsync(country))).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            var executionTime = System.currentTimeMillis() - startTime;
            United.logger()
                    .info("Created " + countries.size() + " countries in map overlay in " + executionTime + "ms");
        }).exceptionally(ex -> {
            United.logger().error("Error rendering countries: " + ex.getMessage(), "UnitedLands");
            ex.printStackTrace();
            return null;
        });

    }

    private void renderCountryAsync(Country country) {

        SimpleLayer layer = getOrCreateSimpleLayer(country.getWorldName(), "countries", "Countries", 1, 460);
        SimpleLayer iconLayer = getOrCreateSimpleLayer(country.getWorldName(), "countryicons", "Country Icons", 1, 465);

        var key = "country-" + country.getUuid().toString();
        if (layer.hasMarker(key))
            layer.removeMarker(key);
        var iconKey = "country-icon-" + country.getUuid().toString();
        if (iconLayer.hasMarker(key))
            iconLayer.removeMarker(key);

        // Icon rendering

        if (country.getIconPosition() != null) {

            var pos = country.getIconPosition();
            var iconPosition = new Point((int) pos.getX(), (int) pos.getZ());

            var iconMarker = Marker.icon(iconKey, iconPosition, country.getUuid().toString()).setAnchor(Vector.of(16.0, 16.0));
            iconLayer.addMarker(iconMarker);
        }   

        // Border rendering

        var popup = new Popup(getCountryInfoBox(country));
        int strokeWidth = UnitedLandsConfig.get().mapRendering().get("country-default").width();

        var countryPolygons = country.getRegions().stream().map(Region::getPolygon).collect(Collectors.toList());
        var vassals = UnitedLandsDataManager.instance().getCountryVassals(country);
        if (vassals.size() > 0) {
            strokeWidth *= 1.5;
            for (var vassal : vassals) {
                var vassalPolygone = vassal.getRegions().stream().map(Region::getPolygon).collect(Collectors.toList());
                countryPolygons.addAll(vassalPolygone);
            }
        }

        var mergedPolygons = mergePolygons(countryPolygons);

        List<Polygon> finalPolygons = new ArrayList<>();

        var markerOptions = Options.builder().fill(true).fillColor(0).stroke(true).strokeColor(country.getStrokeColor())
                .strokeWeight(strokeWidth)
                .tooltipContent(country.getCleanName()).build().setPopup(popup);

        int i = 0;
        for (var mergedPolygon : mergedPolygons) {
            LinkedList<Point> polygonPoints = new LinkedList<>();
            for (int j = 0; j < mergedPolygon.length - 2; j = j + 2) {
                double[] offsetPolygon = null;
                if (country.getOverlord() == null) {
                    offsetPolygon = PolygonUtils.offsetPolygon(mergedPolygon, -1 * strokeWidth);
                } else {
                    offsetPolygon = PolygonUtils.offsetPolygon(mergedPolygon, -1 * strokeWidth * 3);
                }
                // polygonPoints.add(new Point((int) mergedPolygon[j], (int) mergedPolygon[j +
                // 1]));
                polygonPoints.add(new Point((int) offsetPolygon[j], (int) offsetPolygon[j + 1]));
            }
            finalPolygons.add(
                    new Polygon(key, new Polyline("border-" + country.getUuid().toString() + "-" + i, polygonPoints)));
        }

        MultiPolygon mapPolygon = new MultiPolygon(key, finalPolygons);
        mapPolygon.setOptions(markerOptions);
        layer.addMarker(mapPolygon);
    }

    public void removeCountry(Country country) {

        net.pl3x.map.core.world.World mapworld = Pl3xMap.api().getWorldRegistry().get(country.getWorldName());
        var countryLayer = mapworld.getLayerRegistry().get("countries");

        if (countryLayer != null) {
            var countryMarkers = countryLayer.getMarkers();
            var poly = countryMarkers.stream().filter(m -> m.getKey().equals("country-" + country.getUuid().toString()))
                    .findFirst().orElse(null);
            if (poly != null) {
                countryMarkers.remove(poly);
                return;
            }
        }

        var iconLayer = mapworld.getLayerRegistry().get("countryicons");
        if (iconLayer != null) {
            var iconMarkers = iconLayer.getMarkers();
            var poly = iconMarkers.stream()
                    .filter(m -> m.getKey().equals("country-icon-" + country.getUuid().toString()))
                    .findFirst().orElse(null);
            if (poly != null) {
                iconMarkers.remove(poly);
                return;
            }
        }
    }

    public void removeCountryIcon(Country country) {

        net.pl3x.map.core.world.World mapworld = Pl3xMap.api().getWorldRegistry().get(country.getWorldName());
        var iconLayer = mapworld.getLayerRegistry().get("countryicons");
        if (iconLayer != null) {
            var iconMarkers = iconLayer.getMarkers();
            var poly = iconMarkers.stream()
                    .filter(m -> m.getKey().equals("country-icon-" + country.getUuid().toString()))
                    .findFirst().orElse(null);
            if (poly != null) {
                iconMarkers.remove(poly);
                return;
            }
        }
    }

    private String getCountryInfoBox(Country country) {
        var infoBox = countryMapInfoBox;
        infoBox = infoBox.replace("%country%", country.getCleanName());
        infoBox = infoBox.replace("%leader%", country.getLeader() != null ? country.getLeader().getName() : "NPC");
        infoBox = infoBox.replace("%capital%", country.hasCapital() ? country.getCapital().getCleanName() : "");
        infoBox = infoBox.replace("%capital-coords%",
                country.hasCapital()
                        ? "x=" + country.getCapital().getSpawn().blockX() + "&z="
                                + country.getCapital().getSpawn().blockZ()
                        : "");
        infoBox = infoBox.replace("%founded%",
                new SimpleDateFormat("dd-MM-yyyy HH:mm").format(country.getFoundingTimestamp()));
        infoBox = infoBox.replace("%balance%", UnitedLandsEconomyManager.instance()
                .format(UnitedLandsEconomyManager.instance().getBalance(country.getUuid())));
        infoBox = infoBox.replace("%upkeep%", UnitedLandsEconomyManager.instance().format(country.getUpkeep()));
        return infoBox;
    }

    // #endregion

    // #region Region rendering

    // *********************************************************
    // Region rendering
    // *********************************************************

    private void renderPolyRegions(Collection<Region> regions, boolean debug) {

        United.logger().info("Starting region map rendering...", "UnitedLands");

        if (regions == null || regions.isEmpty()) {
            United.logger().info("No regions to render.", "UnitedLands");
            return;
        }

        var startTime = System.currentTimeMillis();
        List<CompletableFuture<Void>> futures = regions.stream()
                .map(region -> CompletableFuture.runAsync(() -> renderPolyRegionAsync(region, debug))).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            var executionTime = System.currentTimeMillis() - startTime;
            United.logger().info("Created " + regions.size() + " regions in map overlay in " + executionTime + "ms");
        }).exceptionally(ex -> {
            United.logger().error("Error rendering regions: " + ex.getMessage(), "UnitedLands");
            return null;
        });

    }

    private void renderPolyRegionAsync(Region region, boolean debug) {

        var unclaimedLayer = getOrCreateSimpleLayer(region.getWorldName(), "unclaimedregions", "Unclaimed Regions", 8,
                450, false);
        var claimedLayer = getOrCreateSimpleLayer(region.getWorldName(), "claimedregions", "Claimed Regions", 6, 450,
                false);

        SimpleLayer regionLayer;
        if (!region.hasCountry()) {
            regionLayer = unclaimedLayer;
        } else {
            regionLayer = claimedLayer;
        }
        SimpleLayer regionInfoMarkerLayer = getOrCreateSimpleLayer(region.getWorldName(), "regioninfos", "Region Infos",
                10, 100);

        var key = "region-" + region.getUuid().toString();
        if (unclaimedLayer.hasMarker(key))
            unclaimedLayer.removeMarker(key);
        if (claimedLayer.hasMarker(key))
            claimedLayer.removeMarker(key);

        var centerMarkerKey = "center-" + region.getUuid();
        if (regionInfoMarkerLayer.hasMarker(centerMarkerKey))
            regionInfoMarkerLayer.removeMarker(centerMarkerKey);
        var claimMarkerKey = "claim-" + region.getUuid();
        if (regionInfoMarkerLayer.hasMarker(claimMarkerKey))
            regionInfoMarkerLayer.removeMarker(claimMarkerKey);

        var centerWordLocation = CoordinateUtils.chunkToWorldCoordinates(region.getHomeChunkCoordinates());
        var centerMarkerPosition = new Point(centerWordLocation.getX() + 8, centerWordLocation.getZ() + 8);

        var centerMarker = Marker.icon(centerMarkerKey, centerMarkerPosition, "region-center");
        regionInfoMarkerLayer.addMarker(centerMarker);

        if (region.getClaimEndTime() != null) {
            var claimMarker = Marker.icon(claimMarkerKey, centerMarkerPosition, "claiming");
            regionInfoMarkerLayer.addMarker(claimMarker);
        }

        // var popup = new Popup("<div><p><strong>" + region.getCleanName() +
        // "</strong></p><p><strong>Owner: </strong>"
        // + (region.hasFounder() ? region.getFounderName() : "-") + "</p></div>");

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
                + (region.hasCountry() ? " (" + region.getCountry().getCleanName() + ")" : "");
        var markerOptions = Options.builder().fill(true).fillType(Fill.Type.NONZERO).fillColor(fillColor).stroke(true)
                .strokeColor(strokeColor)
                .strokeDashPattern(dash).strokeWeight(strokeWidth).tooltipContent(tooltip).build();

        var polyPoints = region.getPolygon();
        LinkedList<Point> points = new LinkedList<>();
        for (int i = 0; i < polyPoints.length - 2; i = i + 2) {
            points.add(new Point((int) polyPoints[i], (int) polyPoints[i + 1]));
        }

        Polygon polygon = new Polygon(key, new Polyline("border-" + region.getUuid().toString(), points));
        polygon.setOptions(markerOptions);

        regionLayer.addMarker(polygon);
    }

    public void removeRegion(Region region) {
        String key = "region-" + region.getUuid().toString();
        var unclaimedLayer = getOrCreateSimpleLayer(region.getWorldName(), "unclaimedregions", "Unclaimed Regions", 30,
                460, false);
        if (unclaimedLayer.hasMarker(key))
            unclaimedLayer.removeMarker(key);

        var claimedLayer = getOrCreateSimpleLayer(region.getWorldName(), "claimedregions", "Claimed Regions", 20, 460,
                false);
        if (claimedLayer.hasMarker(key))
            claimedLayer.removeMarker(key);
    }

    // #region Settlement rendering

    // *********************************************************
    // Settlement rendering
    // *********************************************************

    private void renderSettlements(Collection<Settlement> settlements) {

        United.logger().info("Starting settlement map rendering...", "UnitedLands");

        if (settlements == null || settlements.isEmpty()) {
            United.logger().info("No settlements to render.", "UnitedLands");
            return;
        }

        var startTime = System.currentTimeMillis();
        List<CompletableFuture<Void>> futures = settlements.stream()
                .map(settlement -> CompletableFuture.runAsync(() -> renderSettlementAsync(settlement)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            var executionTime = System.currentTimeMillis() - startTime;
            United.logger()
                    .info("Created " + settlements.size() + " settlements in map overlay in " + executionTime + "ms");
        }).exceptionally(ex -> {
            United.logger().error("Error rendering settlements: " + ex.getMessage(), "UnitedLands");
            return null;
        });
    }

    private void renderSettlementAsync(Settlement settlement) {

        SimpleLayer layer = getOrCreateSimpleLayer(settlement.getWorldName(), "settlements", "Settlements", 1, 480);

        String uuid = settlement.getUuid().toString();
        United.logger().info("Rendering settlement " + uuid + "...");
        String key = "settlement-" + uuid;
        // if (layer.hasMarker(key))
        // layer.removeMarker(key);

        var popup = new Popup(getSettlementInfoBox(settlement));

        int strokeWidth = UnitedLandsConfig.get().mapRendering().get("settlement-default").width();
        String dash = UnitedLandsConfig.get().mapRendering().get("settlement-default").dash();

        if (settlement.hasCountry()) {
            strokeWidth = UnitedLandsConfig.get().mapRendering().get("settlement-in-country").width();
            dash = UnitedLandsConfig.get().mapRendering().get("settlement-in-country").dash();
        }

        var markerOptions = Options.builder().fill(true).fillType(Fill.Type.EVENODD)
                .fillColor(settlement.getFillColor()).stroke(true)
                .strokeColor(settlement.getStrokeColor()).strokeWeight(strokeWidth).strokeDashPattern(dash)
                .tooltipContent(settlement.getCleanName()).build()
                .setPopup(popup);

        var chunkClusters = findClusters(settlement.getChunks());
        List<Polygon> polygons = new ArrayList<>();

        int i = 0;
        for (var cluster : chunkClusters) {

            List<Polyline> clusterLines = new ArrayList<>();

            clusterLines.add(new Polyline("border-" + uuid + "-" + i, generatePolygon(cluster.chunks)));

            var holeClusters = findHoles(cluster, settlement.getWorldName());
            int j = 0;
            for (var holeCluster : holeClusters) {
                clusterLines.add(new Polyline("hole-" + uuid + "-" + j, generatePolygon(holeCluster.chunks)));
                j++;
            }

            polygons.add(new Polygon("cluster-" + uuid + "-" + i, clusterLines));
        }
        MultiPolygon mapPolygon = new MultiPolygon(key, polygons);
        mapPolygon.setOptions(markerOptions);

        layer.addMarker(mapPolygon);
    }

    public void removeSettlement(Settlement settlement) {

        net.pl3x.map.core.world.World mapworld = Pl3xMap.api().getWorldRegistry().get(settlement.getWorldName());
        var settlementsLayer = mapworld.getLayerRegistry().get("settlements");

        if (settlementsLayer != null) {
            var settlemenMarkers = settlementsLayer.getMarkers();
            var poly = settlemenMarkers.stream()
                    .filter(m -> m.getKey().equals("settlement-" + settlement.getUuid().toString())).findFirst()
                    .orElse(null);
            if (poly != null) {
                settlemenMarkers.remove(poly);
                return;
            }
        }
    }

    private String getSettlementInfoBox(Settlement settlement) {

        var infoBox = settlementMapInfoBox;

        infoBox = infoBox.replace("%settlement%", settlement.getCleanName());
        infoBox = infoBox.replace("%mayor%", settlement.getMayor() != null ? settlement.getMayor().getName() : "NPC");
        infoBox = infoBox.replace("%country%",
                settlement.hasCountry() ? settlement.getCountry().getCleanName() : "Independent");
        infoBox = infoBox.replace("%citizencount%", String.valueOf(settlement.getCitizenCount()));
        infoBox = infoBox.replace("%founded%",
                new SimpleDateFormat("dd-MM-yyyy HH:mm").format(settlement.getFoundingTimestamp()));
        infoBox = infoBox.replace("%balance%", UnitedLandsEconomyManager.instance()
                .format(UnitedLandsEconomyManager.instance().getBalance(settlement.getUuid())));
        infoBox = infoBox.replace("%upkeep%", UnitedLandsEconomyManager.instance().format(settlement.getUpkeep()));
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
        Set<SettlementChunk> holeChunks = new HashSet<>();
        for (int x = minX + 1; x <= maxX - 1; x++) {
            for (int z = minZ + 1; z <= maxZ - 1; z++) {
                String key = toKey(x, z, worldName);
                if (!occupied.contains(key) && !reachable.contains(key)) {
                    // Create a synthetic SettlementChunk to represent this empty position
                    SettlementChunk hole = new SettlementChunk(new Coordinates(x, z, worldName));
                    holeChunks.add(hole);
                }
            }
        }

        // Group the hole chunks into connected components — each is a distinct hole
        return findClusters(holeChunks);
    }

    // *******************************
    // Polygon creation
    // *******************************

    private List<Point> generatePolygon(Set<CoordinateHolder> chunks) {

        Set<Coordinates> polygon = new LinkedHashSet<>();

        var rightmostChunk = findRightmostChunk(chunks);

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

        CompletableFuture<SimpleLayer> simpleLayerFuture = new CompletableFuture<>();

        var paneName = key.replace("-", "");

        Bukkit.getScheduler().runTask(UnitedLands.instance(), () -> {

            net.pl3x.map.core.world.World mapworld = Pl3xMap.api().getWorldRegistry().get(world);
            SimpleLayer layer = (SimpleLayer) mapworld.getLayerRegistry().get(key);

            if (layer == null) {
                var options = new LayerOptions(name, true, false, priority, zindex);
                layer = new SimpleLayer(key, options::getName);
                layer.setDefaultHidden(options.isDefaultHidden());
                layer.setPriority(options.getLayerPriority());
                if (setPane)
                    layer.setPane(paneName);
                layer.setZIndex(options.getZIndex());
                layer.setShowControls(options.showControls());
                mapworld.getLayerRegistry().register(layer);
            }

            simpleLayerFuture.complete(layer);
        });

        return simpleLayerFuture.join();
    }

    private boolean isChunkAtCoordinates(Set<CoordinateHolder> chunks, Coordinates coords) {
        return chunks.stream().anyMatch(c -> c.getCoordinates().equals(coords));
    }

    private CoordinateHolder getChunkAtCoordinates(Set<CoordinateHolder> chunks, Coordinates coords) {
        return chunks.stream().filter(c -> c.getCoordinates().equals(coords)).findFirst().orElse(null);
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
