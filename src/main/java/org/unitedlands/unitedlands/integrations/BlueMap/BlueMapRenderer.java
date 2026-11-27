package org.unitedlands.unitedlands.integrations.BlueMap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.integrations.map.MapGeometry;
import org.unitedlands.unitedlands.integrations.map.MapRenderer;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.unitedlands.utils.PolygonUtils;
import org.unitedlands.utils.United;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

public class BlueMapRenderer implements MapRenderer {

    private static BlueMapRenderer instance;

    public static BlueMapRenderer instance() {
        return instance;
    }

    private final int BATCH_SIZE = 150;
    private final long FREQUENCY = 20l;
    private final float SHAPE_Y = 64f;
    private final String ICON_PATH = "unitedlands/icons/";
    private final String SET_PREFIX = "unitedlands-";

    private record Layer(String id, String label, int sorting) {
    }

    private final Layer SETTLEMENTS = new Layer("settlements", "Settlements", 0);
    private final Layer COUNTRIES = new Layer("countries", "Countries", 1);
    private final Layer COUNTRY_ICONS = new Layer("countryicons", "Country Icons", 2);
    private final Layer CLAIMED_REGIONS = new Layer("claimedregions", "Claimed Regions", 3);
    private final Layer UNCLAIMED_REGIONS = new Layer("unclaimedregions", "Unclaimed Regions", 4);
    private final Layer REGION_INFOS = new Layer("regioninfos", "Region Infos", 5);

    private final List<Layer> LAYERS = List.of(SETTLEMENTS, COUNTRIES, COUNTRY_ICONS, CLAIMED_REGIONS,
            UNCLAIMED_REGIONS, REGION_INFOS);

    private final Set<Settlement> settlementQueue = new LinkedHashSet<>();
    private final Set<Region> regionQueue = new LinkedHashSet<>();
    private final Set<Country> countryQueue = new LinkedHashSet<>();

    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "unitedlands-bluemap-render");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, String> iconFiles = new ConcurrentHashMap<>();
    private final Map<String, String> iconAddresses = new ConcurrentHashMap<>();
    private final Map<String, Vector2i> iconAnchors = new ConcurrentHashMap<>();
    private final Set<String> customLayerIds = ConcurrentHashMap.newKeySet();
    private volatile String clearIconAddress = "";

    private final Consumer<BlueMapAPI> enableListener = this::onApiEnabled;
    private final Consumer<BlueMapAPI> disableListener = this::onApiDisabled;

    private BukkitTask queueCheckTask;

    private String settlementMapInfoBox;
    private String countryMapInfoBox;

    private boolean debugMode = false;

    public BlueMapRenderer() {
        instance = this;
    }

    private record SettlementSnapshot(UUID uuid, String worldName, String name, int fillColor, int strokeColor,
            int strokeWidth, String infoBox, Set<MapGeometry.MapChunk> chunks) {
    }

    private record RegionSnapshot(UUID uuid, String worldName, boolean claimed, boolean claiming,
            Coordinates homeChunk, String label, int fillColor, int strokeColor, int strokeWidth,
            double[] polygon) {
    }

    private record CountrySnapshot(UUID uuid, String worldName, String name, int strokeColor, int strokeWidth,
            int offset, Vector3d iconPosition, String infoBox, List<double[]> polygons) {
    }

    @Override
    public void initialize() {
        registerIcon("region-center", "region-center.png");
        registerIcon("claiming", "claiming.png");
        registerIcon("fight", "fight.png");
        registerIcon("siege", "siege.png");
        registerIcon("fortress", "fortress.png");
        registerIcon("home", "home.png");
        registerIcon("camp", "camp.png");
        loadMessages();
        BlueMapAPI.onEnable(enableListener);
        BlueMapAPI.onDisable(disableListener);
    }

    @Override
    public void loadMessages() {
        settlementMapInfoBox = UnitedLandsConfig.get().mapInfoBoxes().settlement();
        countryMapInfoBox = UnitedLandsConfig.get().mapInfoBoxes().country();
    }

    @Override
    public void shutdown() {
        BlueMapAPI.unregisterListener(enableListener);
        BlueMapAPI.unregisterListener(disableListener);
        stopQueueMonitor();
        settlementQueue.clear();
        regionQueue.clear();
        countryQueue.clear();
        renderExecutor.shutdownNow();
        BlueMapAPI.getInstance().ifPresent(this::removeAllMarkerSets);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    private void onApiEnabled(BlueMapAPI api) {
        runOnMainThread(() -> {
            United.logger().info("BlueMap API enabled, rendering map markers...", "UnitedLands");
            createClearIcon(api);
            for (var entry : iconFiles.entrySet())
                writeIcon(api, entry.getKey(), entry.getValue());

            var dataManager = UnitedLandsDataManager.instance();
            if (dataManager == null)
                return;
            addSettlementsToRenderQueue(dataManager.getSettlements());
            addRegionsToRenderQueue(dataManager.getRegions());
            addCountriesToRenderQueue(dataManager.getCountries());
        });
    }

    private void onApiDisabled(BlueMapAPI api) {
        runOnMainThread(() -> {
            stopQueueMonitor();
            settlementQueue.clear();
            regionQueue.clear();
            countryQueue.clear();
            iconAddresses.clear();
            iconAnchors.clear();
            clearIconAddress = "";
        });
    }

    // #region Icons

    @Override
    public void registerIcon(String key, String filename) {
        iconFiles.put(key, filename);
        BlueMapAPI.getInstance().ifPresent(api -> writeIcon(api, key, filename));
    }

    private void writeIcon(BlueMapAPI api, String key, String filename) {
        File importFolder = new File(UnitedLands.instance().getDataFolder(), "icons");
        File imageFile = new File(importFolder, filename);

        BufferedImage img;
        try {
            img = ImageIO.read(imageFile);
        } catch (Exception ex) {
            United.logger().error("Failed to read image " + filename, "UnitedLands");
            return;
        }
        if (img == null) {
            United.logger().error("Failed to read image " + filename, "UnitedLands");
            return;
        }

        try {
            var address = api.getWebApp().createImage(img, ICON_PATH + key);
            iconAddresses.put(key, address);
            iconAnchors.put(key, new Vector2i(img.getWidth() / 2, img.getHeight() / 2));
        } catch (Exception ex) {
            United.logger().error("Failed to register BlueMap icon " + filename + ": " + ex.getMessage(),
                    "UnitedLands");
        }
    }

    private void createClearIcon(BlueMapAPI api) {
        try {
            var img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            clearIconAddress = api.getWebApp().createImage(img, ICON_PATH + "clear");
        } catch (Exception ex) {
            United.logger().error("Failed to create BlueMap clear icon: " + ex.getMessage(), "UnitedLands");
        }
    }

    // #endregion

    // #region Queue handling

    public void checkQueue() {

        if (BlueMapAPI.getInstance().isEmpty()) {
            stopQueueMonitor();
            return;
        }

        var settlements = snapshotBatch(settlementQueue, this::snapshot, "settlement");
        var regions = snapshotBatch(regionQueue, region -> snapshot(region, debugMode), "region");
        var countries = snapshotBatch(countryQueue, this::snapshot, "country");

        if (!settlements.isEmpty() || !regions.isEmpty() || !countries.isEmpty())
            render(settlements, regions, countries);

        if (settlementQueue.isEmpty() && regionQueue.isEmpty() && countryQueue.isEmpty()) {
            stopQueueMonitor();
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
            if (BlueMapAPI.getInstance().isEmpty())
                return;
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
        if (Bukkit.isPrimaryThread())
            runnable.run();
        else
            Bukkit.getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void addToRenderQueue(Settlement settlement) {
        settlementQueue.add(settlement);
        startQueueMonitor();
    }

    @Override
    public void addToRenderQueue(Region region) {
        regionQueue.add(region);
        startQueueMonitor();
    }

    @Override
    public void addToRenderQueue(Country country) {
        countryQueue.add(country);
        startQueueMonitor();
    }

    @Override
    public void addSettlementsToRenderQueue(Collection<Settlement> settlements) {
        settlementQueue.addAll(settlements);
        startQueueMonitor();
    }

    @Override
    public void addRegionsToRenderQueue(Collection<Region> regions) {
        regionQueue.addAll(regions);
        startQueueMonitor();
    }

    @Override
    public void addCountriesToRenderQueue(Collection<Country> countries) {
        countryQueue.addAll(countries);
        startQueueMonitor();
    }

    private void startQueueMonitor() {
        if (queueCheckTask == null && BlueMapAPI.getInstance().isPresent()) {
            United.logger().info("Started map rendering task.", "UnitedLands");
            queueCheckTask = Bukkit.getScheduler().runTaskTimer(UnitedLands.instance(), () -> {
                checkQueue();
            }, FREQUENCY, FREQUENCY);
        }
    }

    private void stopQueueMonitor() {
        if (queueCheckTask != null) {
            queueCheckTask.cancel();
            queueCheckTask = null;
        }
    }

    // #endregion

    // #region Marker rendering

    @Override
    public void renderMarker(String worldName, Location location, String markerName, String markerKey,
            String layerId, String layerName) {
        var layer = new Layer(layerId, layerName, 10);
        customLayerIds.add(layerId);

        var builder = POIMarker.builder().label(markerName)
                .position(location.getX(), location.getY(), location.getZ());
        var address = iconAddresses.get(markerName);
        if (address != null)
            builder.icon(address, iconAnchors.getOrDefault(markerName, Vector2i.ZERO));
        else
            builder.defaultIcon();

        var marker = builder.build();
        for (var set : getMarkerSets(worldName, layer, true)) {
            set.remove(markerKey);
            set.put(markerKey, marker);
        }
    }

    @Override
    public void removeMarker(String worldName, String markerKey, String layerId, String layerName) {
        for (var set : getMarkerSets(worldName, new Layer(layerId, layerName, 10), false))
            set.remove(markerKey);
    }

    // #endregion

    // #region Country rendering

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

        Vector3d iconPosition = null;
        if (country.getIconPosition() != null) {
            var pos = country.getIconPosition();
            iconPosition = new Vector3d(pos.getX(), SHAPE_Y, pos.getZ());
        }

        return new CountrySnapshot(country.getUuid(), country.getWorldName(), country.getCleanName(),
                country.getStrokeColor(), strokeWidth, offset, iconPosition, getCountryInfoBox(country), polygons);
    }

    private Runnable buildCountry(CountrySnapshot country) {

        var uuid = country.uuid().toString();
        var key = "country-" + uuid;
        var iconKey = "country-icon-" + uuid;

        POIMarker iconMarker = null;
        var iconAddress = iconAddresses.get(uuid);
        if (country.iconPosition() != null && iconAddress != null) {
            iconMarker = POIMarker.builder().label(country.name()).detail(country.infoBox())
                    .position(country.iconPosition())
                    .icon(iconAddress, iconAnchors.getOrDefault(uuid, new Vector2i(16, 16))).build();
        }

        Map<String, Marker> markers = new LinkedHashMap<>();
        int i = 0;
        for (var mergedPolygon : MapGeometry.mergePolygons(country.polygons())) {
            double[] offsetPolygon = PolygonUtils.offsetPolygon(mergedPolygon, country.offset());
            var shape = toShape(offsetPolygon);
            if (shape == null)
                continue;
            markers.put(key + "-" + i, ShapeMarker.builder().label(country.name()).detail(country.infoBox())
                    .shape(shape, SHAPE_Y).centerPosition()
                    .fillColor(toColor(0)).lineColor(toColor(country.strokeColor()))
                    .lineWidth(country.strokeWidth()).depthTestEnabled(false).build());
            i++;
        }

        var finalIconMarker = iconMarker;
        return () -> {
            if (UnitedLandsDataManager.instance().getCountry(country.uuid()) == null)
                return;

            for (var set : getMarkerSets(country.worldName(), COUNTRY_ICONS, true)) {
                set.remove(iconKey);
                if (finalIconMarker != null)
                    set.put(iconKey, finalIconMarker);
            }

            replaceMarkers(country.worldName(), COUNTRIES, key, markers);
        };
    }

    @Override
    public void removeCountry(Country country) {
        for (var set : getMarkerSets(country.getWorldName(), COUNTRIES, false))
            removeMarkers(set, "country-" + country.getUuid().toString());
        removeCountryIcon(country);
    }

    @Override
    public void removeCountryIcon(Country country) {
        for (var set : getMarkerSets(country.getWorldName(), COUNTRY_ICONS, false))
            set.remove("country-icon-" + country.getUuid().toString());
    }

    private String getCountryInfoBox(Country country) {
        var infoBox = countryMapInfoBox;

        var iconAddress = iconAddresses.get(country.getUuid().toString());
        infoBox = infoBox.replace("%icon%", iconAddress != null ? iconAddress : clearIconAddress);
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

    private RegionSnapshot snapshot(Region region, boolean debug) {

        int strokeWidth = UnitedLandsConfig.get().mapRendering().get("region-default").width();

        if (region.hasCountry())
            strokeWidth = UnitedLandsConfig.get().mapRendering().get("region-in-country").width();

        int fillColor;
        int strokeColor;
        if (debug) {
            fillColor = region.getDebugFillColor();
            strokeColor = region.getDebugStrokeColor();
            strokeWidth = 2;
        } else {
            fillColor = region.getFillColor();
            strokeColor = region.getStrokeColor();
        }

        var label = region.getCleanName()
                + (region.hasCountry() ? " (" + region.getCountry().getCleanName() + ")" : "")
                + (region.isCoastal() ? " " + Character.toString(0x1F30A) : "")
                + (region.isIsland() ? " " + Character.toString(0x2693) : "");

        return new RegionSnapshot(region.getUuid(), region.getWorldName(), region.hasCountry(),
                region.getClaimEndTime() != null, region.getHomeChunkCoordinates().clone(), label, fillColor,
                strokeColor, strokeWidth, region.getPolygon());
    }

    private Runnable buildRegion(RegionSnapshot region) {

        var key = "region-" + region.uuid().toString();
        var claimMarkerKey = "claim-" + region.uuid();

        var centerWorldLocation = CoordinateUtils.chunkToWorldCoordinates(region.homeChunk());
        var centerMarkerPosition = new Vector3d(centerWorldLocation.getX() + 8, SHAPE_Y,
                centerWorldLocation.getZ() + 8);

        POIMarker claimMarker = null;
        var claimingAddress = iconAddresses.get("claiming");
        if (region.claiming() && claimingAddress != null) {
            claimMarker = POIMarker.builder().label(region.label()).position(centerMarkerPosition)
                    .icon(claimingAddress, iconAnchors.getOrDefault("claiming", Vector2i.ZERO)).build();
        }

        ShapeMarker polygon = null;
        var shape = toShape(region.polygon());
        if (shape != null) {
            polygon = ShapeMarker.builder().label(region.label()).shape(shape, SHAPE_Y).centerPosition()
                    .fillColor(toColor(region.fillColor())).lineColor(toColor(region.strokeColor()))
                    .lineWidth(region.strokeWidth()).depthTestEnabled(false).build();
        }

        var finalClaimMarker = claimMarker;
        var finalPolygon = polygon;
        return () -> {
            if (UnitedLandsDataManager.instance().getRegion(region.uuid()) == null)
                return;

            for (var set : getMarkerSets(region.worldName(), UNCLAIMED_REGIONS, true))
                set.remove(key);
            for (var set : getMarkerSets(region.worldName(), CLAIMED_REGIONS, true))
                set.remove(key);
            if (finalPolygon != null) {
                for (var set : getMarkerSets(region.worldName(),
                        region.claimed() ? CLAIMED_REGIONS : UNCLAIMED_REGIONS, true))
                    set.put(key, finalPolygon);
            }

            for (var set : getMarkerSets(region.worldName(), REGION_INFOS, true)) {
                set.remove(claimMarkerKey);
                if (finalClaimMarker != null)
                    set.put(claimMarkerKey, finalClaimMarker);
            }
        };
    }

    @Override
    public void removeRegion(Region region) {
        String key = "region-" + region.getUuid().toString();
        for (var set : getMarkerSets(region.getWorldName(), UNCLAIMED_REGIONS, false))
            set.remove(key);
        for (var set : getMarkerSets(region.getWorldName(), CLAIMED_REGIONS, false))
            set.remove(key);
        for (var set : getMarkerSets(region.getWorldName(), REGION_INFOS, false))
            set.remove("claim-" + region.getUuid());
    }

    // #endregion

    // #region Settlement rendering

    private SettlementSnapshot snapshot(Settlement settlement) {

        var style = settlement.hasCountry() ? "settlement-in-country" : "settlement-default";
        int strokeWidth = UnitedLandsConfig.get().mapRendering().get(style).width();

        Set<MapGeometry.MapChunk> chunks = settlement.getChunks().stream()
                .map(c -> new MapGeometry.MapChunk(c.getCoordinates().clone()))
                .collect(Collectors.toSet());

        return new SettlementSnapshot(settlement.getUuid(), settlement.getWorldName(), settlement.getCleanName(),
                settlement.getFillColor(), settlement.getStrokeColor(), strokeWidth,
                getSettlementInfoBox(settlement), chunks);
    }

    private Runnable buildSettlement(SettlementSnapshot settlement) {

        String key = "settlement-" + settlement.uuid().toString();

        Map<String, Marker> markers = new LinkedHashMap<>();
        int i = 0;
        for (var cluster : MapGeometry.findClusters(settlement.chunks())) {

            var outline = toShape(MapGeometry.generatePolygon(cluster.getChunks()));

            List<Shape> holes = new ArrayList<>();
            for (var holeCluster : MapGeometry.findHoles(cluster, settlement.worldName()))
                holes.add(toShape(MapGeometry.generatePolygon(holeCluster.getChunks())));

            markers.put(key + "-" + i, ShapeMarker.builder().label(settlement.name()).detail(settlement.infoBox())
                    .shape(outline, SHAPE_Y).holes(holes.toArray(Shape[]::new)).centerPosition()
                    .fillColor(toColor(settlement.fillColor())).lineColor(toColor(settlement.strokeColor()))
                    .lineWidth(settlement.strokeWidth()).depthTestEnabled(false).build());
            i++;
        }

        return () -> {
            if (UnitedLandsDataManager.instance().getSettlement(settlement.uuid()) == null)
                return;
            replaceMarkers(settlement.worldName(), SETTLEMENTS, key, markers);
        };
    }

    @Override
    public void removeSettlement(Settlement settlement) {
        for (var set : getMarkerSets(settlement.getWorldName(), SETTLEMENTS, false))
            removeMarkers(set, "settlement-" + settlement.getUuid().toString());
    }

    private String getSettlementInfoBox(Settlement settlement) {

        var infoBox = settlementMapInfoBox;

        infoBox = infoBox.replace("%settlement%", settlement.getCleanName());
        infoBox = infoBox.replace("%mayor%", settlement.getMayor() != null ? settlement.getMayor().getName() : "NPC");
        infoBox = infoBox.replace("%status%",
                settlement.hasCountry() ? (settlement.isCapital() ? "Capital of" : "Member of") : "");
        infoBox = infoBox.replace("%country%",
                settlement.hasCountry() ? settlement.getCountry().getCleanName() : "Independent");
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

    // #region Helpers

    private Optional<BlueMapWorld> findWorld(BlueMapAPI api, String worldName) {
        World world = Bukkit.getWorld(worldName);
        return world != null ? api.getWorld(world) : api.getWorld(worldName);
    }

    private List<MarkerSet> getMarkerSets(String worldName, Layer layer, boolean create) {
        if (worldName == null)
            return List.of();

        var api = BlueMapAPI.getInstance();
        if (api.isEmpty())
            return List.of();

        var world = findWorld(api.get(), worldName);
        if (world.isEmpty())
            return List.of();

        var setId = SET_PREFIX + layer.id();
        List<MarkerSet> sets = new ArrayList<>();
        for (BlueMapMap map : world.get().getMaps()) {
            var markerSets = map.getMarkerSets();
            MarkerSet set = create
                    ? markerSets.computeIfAbsent(setId, k -> MarkerSet.builder().label(layer.label())
                            .toggleable(true).defaultHidden(false).sorting(layer.sorting()).build())
                    : markerSets.get(setId);
            if (set != null)
                sets.add(set);
        }
        return sets;
    }

    private void replaceMarkers(String worldName, Layer layer, String key, Map<String, Marker> markers) {
        for (var set : getMarkerSets(worldName, layer, true)) {
            removeMarkers(set, key);
            set.getMarkers().putAll(markers);
        }
    }

    private void removeMarkers(MarkerSet set, String key) {
        set.getMarkers().keySet().removeIf(k -> k.equals(key) || k.startsWith(key + "-"));
    }

    private void removeAllMarkerSets(BlueMapAPI api) {
        List<String> setIds = new ArrayList<>();
        for (var layer : LAYERS)
            setIds.add(SET_PREFIX + layer.id());
        for (var layerId : customLayerIds)
            setIds.add(SET_PREFIX + layerId);

        for (var map : api.getMaps())
            for (var setId : setIds)
                map.getMarkerSets().remove(setId);
    }

    private Shape toShape(double[] polygon) {
        if (polygon == null || polygon.length < 8)
            return null;
        List<Vector2d> points = new ArrayList<>();
        for (int j = 0; j < polygon.length - 2; j = j + 2)
            points.add(new Vector2d(polygon[j], polygon[j + 1]));
        return new Shape(points);
    }

    private Shape toShape(List<Coordinates> coordinates) {
        List<Vector2d> points = new ArrayList<>();
        for (var c : coordinates)
            points.add(new Vector2d(c.getX(), c.getZ()));
        return new Shape(points);
    }

    private Color toColor(int argb) {
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, ((argb >>> 24) & 0xFF) / 255f);
    }

    // #endregion

}
