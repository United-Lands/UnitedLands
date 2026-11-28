package org.unitedlands.unitedlands.commands.handlers.admin.towny;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.unitedlands.annotations.UnitedSubCommand;
import org.unitedlands.economy.managers.BankAccountManager;
import org.unitedlands.registrars.command.UnitedCommandExecutor;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.LocationMembership;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.SettlementChunk;
import org.unitedlands.unitedlands.classes.interfaces.MetadataHolder;
import org.unitedlands.unitedlands.classes.metadata.BooleanMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.DoubleMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.IntegerMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.LocationMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.LongMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.StringMetaDataField;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.managers.IntegrationManager;
import org.unitedlands.unitedlands.managers.PermissionManager;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.metadata.BooleanDataField;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.DecimalDataField;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;
import com.palmergames.bukkit.towny.object.metadata.LocationDataField;
import com.palmergames.bukkit.towny.object.metadata.LongDataField;

@UnitedSubCommand(parent = CmdAdminTowny.class, name = "import", description = "Admin towny import", usage = "/ula towny import [town|nation]", requirePlugins = "Towny")
public class CmdAdminTownyImport implements UnitedCommandExecutor {

    @Override
    public List<String> handleTab(CommandSender sender, String[] args) {
        if (args.length == 1)
            return IntegrationManager.instance().getTownyProvider().getTownNames();
        return null;
    }

    @Override
    public void handleCommand(CommandSender sender, String[] args) {

        if (args.length > 1) {
            sendUsage(sender);
            return;
        }

        if (args.length == 1) {
            var town = IntegrationManager.instance().getTownyProvider().getTown(args[0]);
            if (town == null)
                return;

            List<PendingOwner> deferredOwners = Collections.synchronizedList(new ArrayList<>());
            importTown(sender, town, deferredOwners)
                    .thenRun(() -> resolveDeferredOwners(deferredOwners))
                    .thenRun(() -> United.messenger().send(sender, "admin.towny.import.finished"));

        } else {

            // Import everything

            var towns = IntegrationManager.instance().getTownyProvider().getTowns();
            List<PendingOwner> deferredOwners = Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture<Void>> settlementFutures = new ArrayList<>();
            for (Town town : towns) {
                var spawnLocation = town.getSpawnOrNull();
                if (spawnLocation == null)
                    continue;
                settlementFutures.add(importTown(sender, town, deferredOwners));
            }

            CompletableFuture.allOf(settlementFutures.toArray(new CompletableFuture[0])).join();

            resolveDeferredOwners(deferredOwners);

            United.logger().info(
                    "---------------------------------------------------------------------- Finished town import, proceeding to nations...");

            var nations = IntegrationManager.instance().getTownyProvider().getNations();
            List<CompletableFuture<Void>> nationFutures = new ArrayList<>();
            for (var nation : nations) {
                nationFutures.add(importCountry(sender, nation));
            }

            CompletableFuture.allOf(nationFutures.toArray(new CompletableFuture[0])).join();

            United.logger().info(
                    "---------------------------------------------------------------------- Finished nation import, assiging regions...");

            var regions = UnitedLandsDataManager.instance().getRegions();

            // Assign regions to countries based on the oldest Towny nation town in each
            // region
            for (var region : regions) {

                if (region.getSettlements().size() == 0) {
                    United.logger().info("Region " + region.getName() + " has no settlements, skipping.");
                    continue;
                }

                Town oldestTownyNationTown = null;
                for (var settlement : region.getSettlements()) {
                    var townyTown = TownyAPI.getInstance().getTown(settlement.getUuid());
                    if (!townyTown.hasNation())
                        continue;

                    if (oldestTownyNationTown == null) {
                        oldestTownyNationTown = townyTown;
                        continue;
                    }

                    if (townyTown.getRegistered() < oldestTownyNationTown.getRegistered()) {
                        oldestTownyNationTown = townyTown;
                    }
                }

                if (oldestTownyNationTown == null) {
                    United.logger().info("Region " + region.getName() + " has no nation towns, skipping.");
                    continue;
                }

                var country = UnitedLandsDataManager.instance()
                        .getCountry(oldestTownyNationTown.getNationOrNull().getUUID());
                if (country == null) {
                    United.logger().error("Found unimported nation UUID for " + oldestTownyNationTown.getName()
                            + " (country UUID " + oldestTownyNationTown.getNationOrNull().getUUID() + "), skipping.");
                    continue;
                }

                region.setCountry(country);
                region.save();
                country.addRegion(region);
                country.save();

                United.logger().info("Assigned region " + region.getName() + " to country " + country.getName()
                        + " based on age of " + oldestTownyNationTown.getName() + ".");

            }

            United.logger().info(
                    "---------------------------------------------------------------------- Finished region asssigments, setting settlement countries...");

            for (var settlement : UnitedLandsDataManager.instance().getSettlements()) {

                if (!settlement.hasRegion())
                    continue;

                var region = settlement.getRegion();

                if (!region.hasCountry())
                    continue;

                var country = region.getCountry();

                var townyTown = TownyAPI.getInstance().getTown(settlement.getUuid());
                if (townyTown == null) {
                    United.logger().error("Invalid towny town: " + settlement.getName());
                    continue;
                }

                if (!townyTown.hasNation())
                    continue;

                if (townyTown.getNationOrNull().getUUID().equals(country.getUuid())) {

                    settlement.setCountry(country);
                    settlement.save();

                    country.addSettlement(settlement);
                    if (townyTown.isCapital()) {
                        country.setCapital(settlement);
                    }
                    country.save();

                    United.logger().info("Assigned settlement " + settlement.getName() + " to country "
                            + country.getName() + (townyTown.isCapital() ? " as capital " : "") + ".");

                }

            }

            United.logger().info(
                    "---------------------------------------------------------------------- Finished setting settlement countries, starting rendering...");

            var renderer = IntegrationManager.instance().getMapRenderer();
            renderer.setDebugMode(false);
            renderer.addSettlementsToRenderQueue(UnitedLandsDataManager.instance().getSettlements());
            renderer.addRegionsToRenderQueue(UnitedLandsDataManager.instance().getRegions());
            renderer.addCountriesToRenderQueue(UnitedLandsDataManager.instance().getCountries());

            United.messenger().send(sender, "admin.towny.import.finished");
        }

    }

    private CompletableFuture<Void> importTown(CommandSender sender, Town town, List<PendingOwner> deferredOwners) {

        var plots = town.getTownBlocks();

        if (UnitedLandsDataManager.instance().getSettlement(town.getUUID()) != null) {
            United.logger().info("Settlement " + town.getName() + " is already imported, skipping.");
            return CompletableFuture.completedFuture(null);
        }

        var settlement = new Settlement();
        settlement.setUuid(town.getUUID());
        settlement.setName(town.getName());
        settlement.setFoundingTimestamp(town.getRegistered());

        try {
            var spawnLocation = town.getSpawn();

            var homeChunkCoord = CoordinateUtils.locationToChunkCoordinates(spawnLocation);
            settlement.setHomeChunkCoordinates(homeChunkCoord);
            settlement.setSpawn(spawnLocation);
            settlement.setVisitorSpawn(spawnLocation);
            settlement.setWorld(spawnLocation.getWorld());

            var region = UnitedLandsDataManager.instance()
                    .getRegion(CoordinateUtils.locationToChunkCenterCoordinates(spawnLocation));
            if (region != null) {
                region.addSettlement(settlement);
                settlement.setRegion(region);
            }

            List<PendingOwner> townOwners = new ArrayList<>();
            for (var plot : plots) {
                var chunk = convertTownBlockToSettlementChunk(town, plot);

                chunk.setSettlement(settlement);
                chunk.setChunkType(convertTownBlockType(settlement, plot));
                settlement.addChunk(chunk);

                if (plot.hasResident())
                    townOwners.add(new PendingOwner(chunk, plot.getResidentOrNull(), plot));
            }

            var townPerms = town.getPermissions();
            var placePerms = getMembershipFlags(townPerms, ActionType.BUILD);
            var breakPerms = getMembershipFlags(townPerms, ActionType.DESTROY);
            var switchPerms = getMembershipFlags(townPerms, ActionType.SWITCH);
            var blockUsePerms = getMembershipFlags(townPerms, ActionType.ITEM_USE);

            settlement.setPlacePermissions(placePerms);
            settlement.setBreakPermissions(breakPerms);
            settlement.setSwitchPermissions(switchPerms);
            settlement.setInteractPermissions(switchPerms);
            settlement.setContainerPermissions(switchPerms);
            settlement.setBlockUsePermissions(blockUsePerms);

            // Metadata
            convertTownyMetaData(town.getMetadata(), settlement);
            
            // Workaround for CMI UUID chaos
            // Check if there is an imported account for the settlement
            var accountHolder = BankAccountManager.instance().getBankAccountHolder(town.getUUID());
            if (accountHolder == null) {
                United.logger()
                        .warning("No imported bank account found for " + settlement.getName() + ", attempting fix...");

                accountHolder = BankAccountManager.instance().getBankAccountHolder(settlement.getName());
                if (accountHolder == null) {
                    United.logger()
                            .warning("Could not find named account for " + settlement.getName() + ", fix impossible.");
                } else {
                    var balance = accountHolder.getAccount().getBalance();
                    BankAccountManager.instance().remove(accountHolder.getUuid());
                    United.logger().warning(
                            "Removed faulty bank account of " + settlement.getName() + ", adding new account...");
                    BankAccountManager.instance().deposit(settlement.getUuid(), settlement.getName(), balance, null,
                            null);
                    United.logger().warning("New account created for " + settlement.getName() + ".");
                }

            }

            return UnitedLandsDataManager.instance().createSettlementDbDataAsync(settlement).thenRun(() -> {

                United.logger().debug("Saved " + settlement.getName() + ", importing citizens");

                if (!UnitedLandsEconomyManager.instance().hasAccount(settlement.getUuid())) {
                    United.logger().warning("Could not find imported CMI account!");
                }

                var residents = town.getResidents();
                for (var resident : residents) {
                    var citizen = UnitedLandsDataManager.instance().getCitizen(resident.getUUID());
                    if (citizen == null) {
                        citizen = new Citizen();
                        citizen.setUuid(resident.getUUID());
                        citizen.setName(resident.getName());
                        citizen.setJoined(resident.getRegistered());
                        citizen.setLastLogon(resident.getLastOnline());
                        citizen.setSettlement(settlement);
                        settlement.addCitizen(citizen);

                        convertTownyMetaData(resident.getMetadata(), citizen);

                        UnitedLandsDataManager.instance().createCitizenDbData(citizen);
                    }

                    if (resident.isMayor()) {
                        citizen.addSettlementRank("mayor");
                        UnitedLandsDataManager.instance().getDatabaseManager().getCitizenService().updateAsync(citizen);
                    }
                }

                for (var pendingOwner : townOwners) {
                    if (!assignOwner(pendingOwner, false))
                        deferredOwners.add(pendingOwner);
                }

                UnitedLandsDataManager.instance().registerSettlement(settlement);

                United.messenger().send(sender, "admin.towny.import.success-settlement", settlement.getName(),
                        plots.size() + "");
            });

        } catch (Exception ex) {
            United.messenger().send(sender, "admin.towny.import.error", ex.getMessage());
            return CompletableFuture.failedFuture(ex);
        }
    }

    private CompletableFuture<Void> importCountry(CommandSender sender, Nation nation) {

        var country = UnitedLandsDataManager.instance().getCountry(nation.getUUID());
        if (country != null) {
            United.logger().info("Country " + nation.getName() + " is already imported, skipping.");
            return CompletableFuture.completedFuture(null);
        }

        World nationWorld = null;
        if (nation.getSpawnOrNull() == null) {
            // Fallback for nations without spawn
            if (nation.getTowns().size() == 0) {
                United.logger().info("Country " + nation.getName() + " does not have a spawn or any towns, skipping.");
                return CompletableFuture.completedFuture(null);
            }
            var anyTownWithHomeBlock = nation.getTowns().stream().filter(t -> t.getHomeBlockOrNull() != null).findAny()
                    .orElse(null);
            if (anyTownWithHomeBlock == null) {
                United.logger()
                        .info("Country " + nation.getName() + " does not have a town with a home block, skipping.");
                return CompletableFuture.completedFuture(null);
            }
            nationWorld = anyTownWithHomeBlock.getHomeBlockOrNull().getWorld().getBukkitWorld();
        } else {
            nationWorld = nation.getSpawnOrNull().getWorld();
        }

        country = new Country();
        country.setUuid(nation.getUUID());
        country.setName(nation.getName());
        country.setFoundingTimestamp(nation.getRegistered());
        country.setWorld(nationWorld);

        country.setStrokeColor(nation.getMapColorHexCode());
        country.setFillColor(nation.getMapColorHexCode() + "20");

        convertTownyMetaData(nation.getMetadata(), country);

        var leaderResident = UnitedLandsDataManager.instance().getCitizen(nation.getKing().getUUID());
        if (leaderResident != null) {
            leaderResident.addCountryRank("leader");
            leaderResident.save();
        }
        
        // Workaround for CMI UUID chaos
        // Check if there is an imported account for the country
        var accountHolder = BankAccountManager.instance().getBankAccountHolder(nation.getUUID());
        if (accountHolder == null) {
            United.logger().warning("No imported bank account found for " + country.getName() + ", attempting fix...");

            accountHolder = BankAccountManager.instance().getBankAccountHolder(country.getName());
            if (accountHolder == null) {
                United.logger().warning("Could not find named account for " + country.getName() + ", fix impossible.");
            } else {
                var balance = accountHolder.getAccount().getBalance();
                BankAccountManager.instance().remove(accountHolder.getUuid());
                United.logger()
                        .warning("Removed faulty bank account of " + country.getName() + ", adding new account...");
                BankAccountManager.instance().deposit(country.getUuid(), country.getName(), balance, null, null);
                United.logger().warning("New account created for " + country.getName() + ".");
            }

        }

        var finalCountry = country;
        return UnitedLandsDataManager.instance().createCountryDbDataAsync(country).thenAccept(result -> {
            UnitedLandsDataManager.instance().registerCountry(finalCountry);
            United.messenger().send(sender, "admin.towny.import.success-country", nation.getName());
        });

    }

    private SettlementChunk convertTownBlockToSettlementChunk(Town town, TownBlock block) {

        SettlementChunk chunk = new SettlementChunk();
        chunk.setUuid(UUID.randomUUID());

        chunk.setCoordinates(new Coordinates(block.getX(), block.getZ(), block.getWorld().getName()));

        var townPerms = town.getPermissions();
        var blockPerms = block.getPermissions();

        if (townPerms.mobs != blockPerms.mobs) {
            chunk.setAllowAnimals(blockPerms.mobs);
            chunk.setAllowMonsters(blockPerms.mobs);
        }
        if (townPerms.fire != blockPerms.fire)
            chunk.setAllowFire(blockPerms.fire);
        if (townPerms.explosion != blockPerms.explosion)
            chunk.setAllowExplosions(blockPerms.explosion);
        if (townPerms.pvp != blockPerms.pvp)
            chunk.setAllowPvp(blockPerms.pvp);

        if (!block.hasResident() && block.isForSale())
            chunk.setSalePrice((int) Math.round(block.getPlotPrice()));

        var placePerms = getPlotMembershipFlags(townPerms, blockPerms, ActionType.BUILD);
        var breakPerms = getPlotMembershipFlags(townPerms, blockPerms, ActionType.DESTROY);
        var switchPerms = getPlotMembershipFlags(townPerms, blockPerms, ActionType.SWITCH);
        var blockUsePerms = getPlotMembershipFlags(townPerms, blockPerms, ActionType.ITEM_USE);

        chunk.setPlacePermissions(placePerms);
        chunk.setBreakPermissions(breakPerms);
        chunk.setSwitchPermissions(switchPerms);
        chunk.setInteractPermissions(switchPerms);
        chunk.setContainerPermissions(switchPerms);
        chunk.setBlockUsePermissions(blockUsePerms);

        return chunk;
    }

    private record PendingOwner(SettlementChunk chunk, Resident resident, TownBlock block) {
    }

    private boolean assignOwner(PendingOwner pendingOwner, boolean createTownlessCitizen) {
        var resident = pendingOwner.resident();
        if (resident == null)
            return true;

        var citizen = UnitedLandsDataManager.instance().getCitizen(resident.getUUID());
        if (citizen == null && createTownlessCitizen && !resident.hasTown()) {
            citizen = new Citizen();
            citizen.setUuid(resident.getUUID());
            citizen.setName(resident.getName());
            citizen.setJoined(resident.getRegistered());
            citizen.setLastLogon(resident.getLastOnline());
            UnitedLandsDataManager.instance().createCitizenDbData(citizen);
        }
        if (citizen == null)
            return false;

        pendingOwner.chunk().setOwner(citizen);
        UnitedLandsDataManager.instance().getDatabaseManager().getSettlementChunkService()
                .updateAsync(pendingOwner.chunk());
        return true;
    }

    private void resolveDeferredOwners(List<PendingOwner> deferredOwners) {
        synchronized (deferredOwners) {
            for (var pendingOwner : deferredOwners) {
                if (assignOwner(pendingOwner, true))
                    continue;
                var block = pendingOwner.block();
                var ownerTown = pendingOwner.resident().getTownOrNull();
                United.logger().warning("Could not import owner " + pendingOwner.resident().getName() + " of plot "
                        + block.getX() + "/" + block.getZ() + " in " + pendingOwner.chunk().getSettlement().getName()
                        + ": their town " + (ownerTown != null ? ownerTown.getName() : "?")
                        + " has not been imported. The plot was left without owner.");
            }
            deferredOwners.clear();
        }
    }

    private static final String DEFAULT_PLOT_TYPE = "none";

    private static final Map<String, String> TOWNY_PLOT_TYPE_MAPPING = Map.of(
            "default", DEFAULT_PLOT_TYPE,
            "residential", DEFAULT_PLOT_TYPE,
            "shop", DEFAULT_PLOT_TYPE,
            "commercial", DEFAULT_PLOT_TYPE,
            "jail", DEFAULT_PLOT_TYPE,
            "embassy", "freehold");

    private String convertTownBlockType(Settlement settlement, TownBlock block) {
        var townyType = block.getTypeName() != null ? block.getTypeName().toLowerCase() : "default";
        var type = TOWNY_PLOT_TYPE_MAPPING.getOrDefault(townyType, townyType);
        if (type.equals(DEFAULT_PLOT_TYPE))
            return DEFAULT_PLOT_TYPE;

        var plotType = PermissionManager.instance().getPlotType(type);
        if (plotType == null) {
            United.logger().warning("Towny plot type '" + townyType + "' at " + block.getX() + "/" + block.getZ()
                    + " in " + settlement.getName() + " has no matching plot type, using '" + DEFAULT_PLOT_TYPE
                    + "'.");
            return DEFAULT_PLOT_TYPE;
        }

        if (plotType.maxPerSettlement() != -1
                && settlement.getChunksOfType(type).size() >= plotType.maxPerSettlement()) {
            United.logger().warning("Towny plot type '" + townyType + "' at " + block.getX() + "/" + block.getZ()
                    + " in " + settlement.getName() + " exceeds the limit of " + plotType.maxPerSettlement()
                    + ", using '" + DEFAULT_PLOT_TYPE + "'.");
            return DEFAULT_PLOT_TYPE;
        }

        return type;
    }

    private int getPlotMembershipFlags(TownyPermission townPerms, TownyPermission plotPerms, ActionType action) {
        int membership = LocationMembership.UNSET;

        var anyOverrides = townPerms.getResidentPerm(action) != plotPerms.getResidentPerm(action) ||
                townPerms.getNationPerm(action) != plotPerms.getNationPerm(action) ||
                townPerms.getAllyPerm(action) != plotPerms.getAllyPerm(action) ||
                townPerms.getOutsiderPerm(action) != plotPerms.getOutsiderPerm(action);

        if (!anyOverrides)
            return membership;

        if (plotPerms.getResidentPerm(action)) {
            membership |= LocationMembership.SETTLEMENT_RESIDENT;
        }

        if (plotPerms.getNationPerm(action)) {
            membership |= LocationMembership.COUNTRY_RESIDENT;
        }

        if (plotPerms.getAllyPerm(action)) {
            membership |= LocationMembership.ALLY;
        }

        if (plotPerms.getOutsiderPerm(action)) {
            membership |= LocationMembership.FOREIGNER;
        }

        return membership;
    }

    private int getMembershipFlags(TownyPermission perms, ActionType action) {
        int membership = LocationMembership.OWNER;
        if (perms.getResidentPerm(action)) {
            membership |= LocationMembership.SETTLEMENT_RESIDENT;
        }
        if (perms.getNationPerm(action)) {
            membership |= LocationMembership.COUNTRY_RESIDENT;
        }
        if (perms.getAllyPerm(action)) {
            membership |= LocationMembership.ALLY;
        }
        if (perms.getOutsiderPerm(action)) {
            membership |= LocationMembership.FOREIGNER;
        }
        return membership;
    }

    private void convertTownyMetaData(Collection<CustomDataField<?>> townyMeta, MetadataHolder holder) {
        for (var tmf : townyMeta) {
            var type = tmf.getTypeID();
            switch (type) {
                case "towny_booldf":
                    var boolMeta = new BooleanMetaDataField(tmf.getKey());
                    boolMeta.setLabel(tmf.getLabel());
                    boolMeta.setShowInScreens(tmf.shouldDisplayInStatus());
                    boolMeta.setValue(((BooleanDataField) tmf).getValue());
                    holder.addMetadata(boolMeta);
                    break;
                case "towny_decdf":
                    var doubleMeta = new DoubleMetaDataField(tmf.getKey());
                    doubleMeta.setLabel(tmf.getLabel());
                    doubleMeta.setShowInScreens(tmf.shouldDisplayInStatus());
                    doubleMeta.setValue(((DecimalDataField) tmf).getValue());
                    holder.addMetadata(doubleMeta);
                    break;
                case "towny_intdf":
                    var intMeta = new IntegerMetaDataField(tmf.getKey());
                    intMeta.setLabel(tmf.getLabel());
                    intMeta.setShowInScreens(tmf.shouldDisplayInStatus());
                    intMeta.setValue(((IntegerDataField) tmf).getValue());
                    holder.addMetadata(intMeta);
                    break;
                case "towny_locationdf":
                    var locMeta = new LocationMetaDataField(tmf.getKey());
                    locMeta.setLabel(tmf.getLabel());
                    locMeta.setShowInScreens(tmf.shouldDisplayInStatus());
                    locMeta.setValue(((LocationDataField) tmf).getValue());
                    holder.addMetadata(locMeta);
                    break;
                case "towny_longdf":
                    var longMeta = new LongMetaDataField(tmf.getKey());
                    longMeta.setLabel(tmf.getLabel());
                    longMeta.setShowInScreens(tmf.shouldDisplayInStatus());
                    longMeta.setValue(((LongDataField) tmf).getValue());
                    holder.addMetadata(longMeta);
                    break;
                default:
                    var stringMeta = new StringMetaDataField(tmf.getKey());
                    stringMeta.setLabel(tmf.getLabel());
                    stringMeta.setShowInScreens(tmf.shouldDisplayInStatus());
                    stringMeta.setValue(String.valueOf(tmf.getValue()));
                    holder.addMetadata(stringMeta);
                    break;
            }
        }
    }

}
