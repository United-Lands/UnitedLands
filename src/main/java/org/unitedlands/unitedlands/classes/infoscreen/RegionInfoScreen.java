package org.unitedlands.unitedlands.classes.infoscreen;

import java.text.SimpleDateFormat;
import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.classes.LocationMembership;
import org.unitedlands.unitedlands.classes.Region;

import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.utils.CostUtils;
import org.unitedlands.utils.United;

public class RegionInfoScreen extends InfoScreen {

    public RegionInfoScreen(Region region, Player player) {

        var header = buildHeader(region.getCleanName());
        addComponent("header", header);


        if (region.hasCountry()) {

            addComponent(player, "owner", "info-screens.region.owner", 
                region.getCountry() != null ? region.getCountry().getCleanName() : "-", 
                region.getClaimedTime() != null ? new SimpleDateFormat("dd-MM-yyyy HH:mm").format(region.getClaimedTime()) : "-");

            addComponent(player, "upkeep", "info-screens.region.costs",
                    UnitedLandsEconomyManager.instance().format(CostUtils.getRegionUpkeep(region, region.getCountry())));
        } else {

            if (region.getClaimEndTime() != null && region.getClaimantCountry() != null) {
                addComponent(player, "upkeep", "info-screens.region.claiming",
                    region.getClaimantCountry().getCleanName(),
                    United.formatter().formatDuration(region.getClaimEndTime() - System.currentTimeMillis()));
            } else {
                addComponent(player, "owner", "info-screens.region.owner", 
                    "(unclaimed)", "-");
                var citizen = UnitedLandsDataManager.instance().getCitizen(player);
                if (citizen != null && citizen.hasCountry()) {
                    addComponent(player, "upkeep", "info-screens.region.costs-projected",
                            UnitedLandsEconomyManager.instance().format(CostUtils.getRegionClaimCosts(citizen.getCountry(), region)),
                            UnitedLandsEconomyManager.instance().format(CostUtils.getRegionUpkeep(region, citizen.getCountry()))
                        );
                }
            }


        }

        var regionAdminStr = "";
        if (region.getAdministrator() != null) {
            if (region.getAdministratorTitle() != null) {
                regionAdminStr += region.getAdministratorTitle() + " ";
            }
            regionAdminStr += region.getAdministrator().getName();
        } else {
            regionAdminStr = "-";
        }
        addComponent(player, "administrator", "info-screens.region.administrator", regionAdminStr);

        addComponent(player, "area", "info-screens.region.area",
                String.format("%,.0f", region.getArea()));

        var pvp = region.allowPvp() ? "<green>PVP</green>" : "<red>PVP</red>";
        var mobs = region.allowMonsters() ? "<green>Monsters</green>" : "<red>Monsters</red>";
        var animals = region.allowAnimals() ? "<green>Animals</green>" : "<red>Animals</red>";
        var fire = region.allowFire() ? "<green>Fire</green>" : "<red>Fire</red>";
        var explosions = region.allowExplosions() ? "<green>Explosions</green>" : "<red>Explosions</red>";

        addComponent(player, "toggles", "info-screens.region.toggles",
                pvp, mobs, animals, fire, explosions);

        addComponent(player, "perm1", "info-screens.region.perm-1",
                LocationMembership.toInfoScreenString(region.getBreakPermissions()),
                LocationMembership.toInfoScreenString(region.getPlacePermissions()),
                LocationMembership.toInfoScreenString(region.getContainerPermissions()));

        addComponent(player, "perm2", "info-screens.region.perm-2",
                LocationMembership.toInfoScreenString(region.getSwitchPermissions()), 
                LocationMembership.toInfoScreenString(region.getBlockUsePermissions()), 
                LocationMembership.toInfoScreenString(region.getInteractPermissions()));

    }

}
