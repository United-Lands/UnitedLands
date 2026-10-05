package org.unitedlands.unitedlands.classes.infoscreen;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.LocationMembership;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.events.infoscreen.SettlementInfoScreenEvent;
import org.unitedlands.unitedlands.classes.metadata.BooleanMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.DoubleMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.FloatMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.IntegerMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.LongMetaDataField;
import org.unitedlands.unitedlands.classes.metadata.StringMetaDataField;
import org.unitedlands.unitedlands.managers.UnitedLandsEconomyManager;
import org.unitedlands.unitedlands.utils.CostUtils;
import org.unitedlands.utils.United;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class SettlementInfoScreen extends InfoScreen {

    public SettlementInfoScreen(Settlement settlement, Player player) {
        var header = buildHeader(settlement.getCleanName());
        addComponent("header", header);

        addComponent(player, "board",
                "info-screens.settlement.board",
                settlement.getTownBoard() != null ? settlement.getTownBoard() : "/settlement setboard [msg]");

        addComponent(player, "region",
                "info-screens.settlement.region",
                settlement.getRegion() != null ? settlement.getRegion().getCleanName() : "-",
                settlement.getCountry() != null ? settlement.getCountry().getCleanName() : "-");

        var foundingDate = new SimpleDateFormat("dd-MM-yyyy HH:mm").format(settlement.getFoundingTimestamp());
        var founder = settlement.getFounderName() != null ? settlement.getFounderName() : "-";

        addComponent(player, "founded",
                "info-screens.settlement.founded",
                foundingDate, founder);

        addComponent(player, "mayor",
                "info-screens.settlement.mayor",
                settlement.getMayor() != null ? settlement.getMayor().getName() : "-");

        var isPublic = settlement.isPublic() ? "<green>Public</green>" : "<red>Private</red>";
        var pvp = settlement.allowPvp() ? "<green>PVP</green>" : "<red>PVP</red>";
        var mobs = settlement.allowMonsters() ? "<green>Monsters</green>" : "<red>Monsters</red>";
        var animals = settlement.allowAnimals() ? "<green>Animals</green>" : "<red>Animals</red>";
        var fire = settlement.allowFire() ? "<green>Fire</green>" : "<red>Fire</red>";
        var explosions = settlement.allowExplosions() ? "<green>Explosions</green>" : "<red>Explosions</red>";

        addComponent(player, "toggles",
                "info-screens.settlement.toggles",
                isPublic,
                pvp,
                mobs,
                animals,
                fire,
                explosions);

        addComponent(player, "perm1",
                "info-screens.settlement.perm-1",
                LocationMembership.toInfoScreenString(settlement.getBreakPermissions()),
                LocationMembership.toInfoScreenString(settlement.getPlacePermissions()),
                LocationMembership.toInfoScreenString(settlement.getContainerPermissions()));
        addComponent(player, "perm2", "info-screens.settlement.perm-2",
                LocationMembership.toInfoScreenString(settlement.getSwitchPermissions()),
                LocationMembership.toInfoScreenString(settlement.getBlockUsePermissions()),
                LocationMembership.toInfoScreenString(settlement.getInteractPermissions()));

        addComponent(player, "balance",
                "info-screens.settlement.balance",
                UnitedLandsEconomyManager.instance().format(UnitedLandsEconomyManager.instance().getBalance(settlement.getUuid())));

        var taxString = settlement.useTaxPercent() ? String.format("%.2f%%", settlement.getTax() * 100)
                : UnitedLandsEconomyManager.instance().format((double) settlement.getTax());

        addComponent(player, "taxes",
                "info-screens.settlement.taxes",
                taxString);

        var citizenNames = "(no citizens)";
        var citizenCount = 0;
        if (settlement.getCitizens() != null) {
            citizenCount = settlement.getCitizens().size();
            citizenNames = String.join(", ",
                    settlement.getCitizens().stream()
                            .map(Citizen::getName)
                            .collect(Collectors
                                    .toList()));
        }
        var citizens = MiniMessage.miniMessage().deserialize(United.messenger().get(player, "info-screens.settlement.citizens",
                String.valueOf(citizenCount)))
                .hoverEvent(
                        HoverEvent.showText(
                                Component.text(citizenNames)));
        addComponent("citizens", citizens);

        addComponent(player, "sizeupkeep", "info-screens.settlement.sizeupkeep",
                String.valueOf(settlement.getChunks().size()),
                UnitedLandsEconomyManager.instance().format(CostUtils.getSettlementUpkeep(settlement)));

        var metadata = settlement.getMetadata();

        if (metadata != null && !metadata.isEmpty()) {

            var metaDataWrapper = "info-screens.settlement.metadata";

            List<String> fields = new ArrayList<>();
            for (var m : metadata.values()) {
                if (!m.showInScreens())
                    continue;
                if (m.getValue() == null)
                    continue;
                var field = "<bold>" + m.getLabel() + "</bold>: ";
                if (m instanceof StringMetaDataField typedData) {
                    field += typedData.getValue();
                } else if (m instanceof IntegerMetaDataField typedData) {
                    field += typedData.getValue();
                } else if (m instanceof LongMetaDataField typedData) {
                    field += typedData.getValue();
                } else if (m instanceof FloatMetaDataField typedData) {
                    field += String.format("%.2f", typedData.getValue());
                } else if (m instanceof DoubleMetaDataField typedData) {
                    field += String.format("%.2f", typedData.getValue());
                } else if (m instanceof BooleanMetaDataField typedData) {
                    field += typedData.getValue() ? "Yes" : "No";
                }
                fields.add(field);
            }

            if (!fields.isEmpty()) {
                var finalMetaDataString = metaDataWrapper.replace("{metadata}",
                        String.join("<dark_gray> | </dark_gray>", fields));
                addComponent("metadata", MiniMessage.miniMessage().deserialize(finalMetaDataString));
            }
        }

        (new SettlementInfoScreenEvent(this, settlement)).callEvent();
    }
}
