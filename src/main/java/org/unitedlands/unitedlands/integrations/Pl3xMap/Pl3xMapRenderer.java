package org.unitedlands.unitedlands.integrations.Pl3xMap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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
import org.unitedlands.unitedlands.classes.map.LayerOptions;
import org.unitedlands.unitedlands.integrations.map.MapGeometry;
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

    private record SettlementSnapshot(UUID uuid, String worldName, String name, int fillColor, int strokeColor,
            int strokeWidth, String dash, String infoBox, Set<MapGeometry.MapChunk> chunks) {
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

        var mergedPolygons = MapGeometry.mergePolygons(country.polygons());

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

        Set<MapGeometry.MapChunk> chunks = settlement.getChunks().stream()
                .map(c -> new MapGeometry.MapChunk(c.getCoordinates().clone()))
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

        var chunkClusters = MapGeometry.findClusters(settlement.chunks());
        List<Polygon> polygons = new ArrayList<>();

        int i = 0;
        for (var cluster : chunkClusters) {

            List<Polyline> clusterLines = new ArrayList<>();

            clusterLines.add(new Polyline("border-" + uuid + "-" + i, toPoints(MapGeometry.generatePolygon(cluster.getChunks()))));

            var holeClusters = MapGeometry.findHoles(cluster, settlement.worldName());
            int j = 0;
            for (var holeCluster : holeClusters) {
                clusterLines.add(
                        new Polyline("hole-" + uuid + "-" + i + "-" + j, toPoints(MapGeometry.generatePolygon(holeCluster.getChunks()))));
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

    // *******************************
    // Helpers
    // *******************************

    private List<Point> toPoints(List<Coordinates> coordinates) {
        List<Point> points = new ArrayList<>();
        for (var c : coordinates)
            points.add(new Point(c.getX(), c.getZ()));
        return points;
    }

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

}
