package org.unitedlands.unitedlands.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.unitedlands.unitedlands.classes.Coordinates;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig;
import org.unitedlands.unitedlands.utils.CoordinateUtils;
import org.unitedlands.utils.United;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;

public class DisplayManager {

    private static DisplayManager instance;

    public static DisplayManager instance() {
        return instance;
    }

    private Map<Settlement, BossBar> settlementNameDisplays = new HashMap<>();
    private Map<Settlement, Set<Player>> settlementNameDisplayViewers = new HashMap<>();

    private Map<Player, Scoreboard> playerScoreboards = new HashMap<>();

    public DisplayManager() {
        instance = this;
    }

    public void showRegionName(Region region, Player player, List<String> addionalDisplay) {
        String addition = "";
        if (addionalDisplay != null && !addionalDisplay.isEmpty()) {
            addition = " (" + String.join(" | ", addionalDisplay) + ")";
        }
        player.sendActionBar(
                United.miniMessage().get().deserialize("<red>" + region.getCleanName() + "</red>" + addition));
    }

    public void showSettlementNameDisplay(Settlement settlement, Player player) {

        var display = settlementNameDisplays.computeIfAbsent(settlement,
                k -> createSettlementNameDisplay(settlement));

        var viewers = settlementNameDisplayViewers.computeIfAbsent(settlement, k -> new HashSet<>());
        if (!viewers.contains(player)) {
            display.addViewer(player);
            viewers.add(player);
        }
    }

    public void refreshSettlementDisplay(Settlement settlement) {
        var display = settlementNameDisplays.get(settlement);
        if (display == null)
            return;
        var name = settlement.getCleanName();
        if (settlement.hasCountry())
            name += " (" + settlement.getCountry().getDisplayName() + ")";
        display.name(United.miniMessage().get().deserialize(name));
    }

    private BossBar createSettlementNameDisplay(Settlement settlement) {
        var name = settlement.getCleanName();
        if (settlement.hasCountry())
            name += " (" + settlement.getCountry().getDisplayName() + ")";

        return BossBar.bossBar(United.miniMessage().get().deserialize(name), 1f, Color.WHITE, Overlay.PROGRESS);
    }

    public void hideSettlementNameDisplay(Settlement settlement, Player player) {

        var display = settlementNameDisplays.get(settlement);
        var viewers = settlementNameDisplayViewers.get(settlement);

        if (display != null && viewers != null && viewers.contains(player)) {
            display.removeViewer(player);
            viewers.remove(player);

            if (viewers.size() == 0) {
                settlementNameDisplays.remove(settlement);
                settlementNameDisplayViewers.remove(settlement);
            }
        }
    }

    // #region Chunk Map Display

    public boolean isPlayerViewingMap(Player player) {
        return playerScoreboards.containsKey(player);
    }

    public void hideMap(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreBoard = playerScoreboards.get(player);
        if (scoreBoard != null) {
            if (!scoreBoard.equals(manager.getMainScoreboard())) {
                for (Objective obj : scoreBoard.getObjectives()) {
                    obj.unregister();
                }
            }
            player.setScoreboard(manager.getMainScoreboard());
            playerScoreboards.remove(player);
        }
    }

    public void showMap(Player player, Location location) {

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreBoard = playerScoreboards.computeIfAbsent(player, k -> manager.getNewScoreboard());

        var playerChunkCoords = CoordinateUtils.locationToChunkCoordinates(location);

        Objective obj = scoreBoard.getObjective(player.getUniqueId().toString());
        if (obj != null)
            obj.unregister();

        obj = scoreBoard.registerNewObjective(
                player.getUniqueId().toString(),
                Criteria.DUMMY,
                Component.text("§6§lMap §r§7" + playerChunkCoords.toCleanShortString()));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank());

        for (int z = -4; z <= 4; z++) {
            String padding = "";
            for (int p = 0; p <= z + 4; p++) {
                padding += "§f";
            }

            String mapLine = "";
            for (int x = -8; x <= 8; x++) {
                var symbol = getChunkSymbol(playerChunkCoords.clone().add(x, z));
                if (z == 0 && x == 0)
                    symbol = "§6" + symbol;
                mapLine += symbol + "§r";
            }

            var line = obj.getScore(padding + mapLine);
            line.setScore(0);
        }

        player.setScoreboard(scoreBoard);
        playerScoreboards.put(player, scoreBoard);
    }

    private String getChunkSymbol(Coordinates coords) {
        var chunk = UnitedLandsDataManager.instance().getSettlementChunk(coords);
        if (chunk != null) {
            if (chunk.isForSale())
                return "$";
            if (chunk.getChunkType() != null)
                return UnitedLandsConfig.get().settlementChunks().get(chunk.getChunkType()).mapIcon();
            return "+";
        }
        return "-";
    }

    // #endregion

    // #region Effects

    public void playFireworkEffect(Location location) {

        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        FireworkEffect effect = FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE) // Shape: BALL, BALL_LARGE, STAR, CREEPER, BURST
                .withColor(org.bukkit.Color.RED, org.bukkit.Color.BLUE) // Main colors
                .withFade(org.bukkit.Color.WHITE) // Fade out color
                .flicker(true) // Twinkle/Crackle effect
                .trail(true) // Tail effect
                .build();

        meta.addEffect(effect);
        firework.setFireworkMeta(meta);

        firework.detonate();
    }

    //#endregion

}
