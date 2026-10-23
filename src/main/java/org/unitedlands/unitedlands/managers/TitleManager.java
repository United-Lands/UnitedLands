package org.unitedlands.unitedlands.managers;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.GeopolAttributeModifier;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsTitlesConfig;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsTitlesConfig.UniqueTitleDefinition;

public class TitleManager {

    private static TitleManager instance;

    public static TitleManager instance() {
        return instance;
    }

    private List<GenericTitleEntry> entries = new ArrayList<>();
    private Map<String, Integer> tierByTitle = new HashMap<>();

    public TitleManager() {
        instance = this;
        loadSettings();
    }

    public void loadSettings() {

        entries = new ArrayList<>();
        tierByTitle = new HashMap<>();

        var tiers = UnitedLandsTitlesConfig.get().genericTitleTiers();

        for (int tier = 1; tier <= tiers; tier++) {
            for (String title : UnitedLandsTitlesConfig.get().genericTitles().get("tier-" + tier).titles()) {
                // Match whole words only: no letter or digit directly before or after
                Pattern pattern = Pattern.compile(
                        "(?<![\\p{L}\\p{N}])" + Pattern.quote(normalize(title)) + "(?![\\p{L}\\p{N}])");
                entries.add(new GenericTitleEntry(title, tier, pattern));
                tierByTitle.put(title, tier);
            }
        }

        entries.sort(Comparator.comparingInt((GenericTitleEntry e) -> e.title().length()).reversed());
    }

    public List<String> findTitles(String countryName) {
        if (countryName == null || countryName.isBlank())
            return List.of();

        String text = normalize(countryName);
        boolean[] claimed = new boolean[text.length()];
        TreeMap<Integer, String> found = new TreeMap<>();

        for (GenericTitleEntry entry : entries) {
            Matcher m = entry.pattern().matcher(text);
            while (m.find()) {
                if (isClaimed(claimed, m.start(), m.end()))
                    continue;
                Arrays.fill(claimed, m.start(), m.end(), true);
                found.put(m.start(), entry.title());
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(found.values()));
    }

    public int tierOf(String title) {
        return tierByTitle.getOrDefault(title, -1);
    }

    private static boolean isClaimed(boolean[] claimed, int start, int end) {
        for (int i = start; i < end; i++) {
            if (claimed[i])
                return true;
        }
        return false;
    }

    public int getRequiredRegions(String title) {
        var tier = tierOf(title);
        if (tier != -1) {
            return UnitedLandsTitlesConfig.get().genericTitles().get("tier-" + tier).regionCount();
        } else {
            return 1;
        }
    }

    public Country getUniqueTitleHolder(String title) {
        return UnitedLandsDataManager.instance().getCountries().stream().filter(c -> c.hasClaimedTitle(title))
                .findFirst().orElse(null);
    }

    public boolean isUniqueTitleClaimed(String title) {
        return getUniqueTitleHolder(title) != null;
    }

    public void equipTitle(Country country, String title, UniqueTitleDefinition titleDefinition) {

        for (var titleModifier : titleDefinition.effects().modifiers().values()) {
            GeopolAttributeModifier countryModifer = new GeopolAttributeModifier(
                    titleModifier.attributeKey(),
                    "unique-title-" + title,
                    titleModifier.mode(),
                    titleModifier.valueModifier(),
                    titleModifier.minValueModifier(),
                    titleModifier.maxValueModifier(),
                    titleModifier.dailyChangeModifier());
            country.addAttributeModifier(countryModifer);
        }

        country.setDisplayName(titleDefinition.displayName());
        country.setEquippedTitle(title);
        country.save();
    }

    public void unequipTitle(Country country, UniqueTitleDefinition titleDefinition) {

        var title = country.getEquippedTitle();
        if (title == null)
            return;

        for (var titleModifier : titleDefinition.effects().modifiers().values()) {
            country.removeAttributeModifiers(titleModifier.attributeKey(), "unique-title-" + title);
        }

        country.setDisplayName(null);
        country.setEquippedTitle(null);
        country.save();
    }

    public UniqueTitleValidation validateUniqueTitle(Country country, String titleName) {

        var titleConfig = UnitedLandsTitlesConfig.get().uniqueTitles().get(titleName);

        var ownedRegions = country.getRegions().stream().map(Region::getDefaultName).toList();
        var requiredPrimaryRegions = titleConfig.primaryRegions();
        var requiredSecondaryRegions = titleConfig.secondaryRegions();

        var ownedPrimaryRegions = requiredPrimaryRegions.stream().filter(r -> ownedRegions.contains(r)).toList();
        var missingPrimaryRegions = requiredPrimaryRegions.stream().filter(r -> !ownedRegions.contains(r)).toList();
        var ownedSecondaryRegions = requiredSecondaryRegions.stream().filter(r -> ownedRegions.contains(r)).toList();
        var missingSecondaryRegions = requiredSecondaryRegions.stream().filter(r -> !ownedRegions.contains(r)).toList();

        boolean claimable = missingPrimaryRegions.size() == 0
                && ownedSecondaryRegions.size() >= titleConfig.requiredSecondaryRegions();

        return new UniqueTitleValidation(
                ownedPrimaryRegions,
                missingPrimaryRegions,
                requiredPrimaryRegions.size(),
                ownedSecondaryRegions,
                missingSecondaryRegions,
                titleConfig.requiredSecondaryRegions(),
                claimable);

    }

    // Helpers

    private record GenericTitleEntry(
            String title,
            int tier,
            Pattern pattern) {
    }

    public record UniqueTitleValidation(
            List<String> ownedPrimaryRegions,
            List<String> missingPrimaryRegions,
            int requiredPrimaryRegionCount,
            List<String> ownedSecondaryRegions,
            List<String> missingecondaryRegions,
            int requiredSecondaryRegionCount,
            boolean claimable) {
    }

    private static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = n.replaceAll("[ʻ'’`]", "");
        n = n.toLowerCase(Locale.ROOT);
        return n.replaceAll("[\\s_]+", "_");
    }

}
