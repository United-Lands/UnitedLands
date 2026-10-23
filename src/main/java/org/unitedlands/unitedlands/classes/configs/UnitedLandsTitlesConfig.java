package org.unitedlands.unitedlands.classes.configs;

import java.util.List;

import org.unitedlands.annotations.UnitedConfig;
import org.unitedlands.annotations.UnitedSection;
import org.unitedlands.annotations.UnitedSetting;
import org.unitedlands.registrars.config.UnitedConfigHandler;
import org.unitedlands.registrars.config.UnitedConfigs;
import org.unitedlands.registrars.config.UnitedDynamicSection;
import org.unitedlands.unitedlands.classes.GeopolAttributeModifier.Mode;

@UnitedConfig(file = "titles.yml")
public interface UnitedLandsTitlesConfig extends UnitedConfigHandler {

    static UnitedLandsTitlesConfig get() {
        return UnitedConfigs.get(UnitedLandsTitlesConfig.class);
    }

    @UnitedSetting(key = "generic-title-tiers", def = "4")
    int genericTitleTiers();

    @UnitedSection(key = "generic-titles")
    UnitedDynamicSection<GenericTitleDefinition> genericTitles();

    @UnitedSection(key = "unique-titles")
    UnitedDynamicSection<UniqueTitleDefinition> uniqueTitles();


    record GenericTitleDefinition(
            @UnitedSetting(key = "region-count", def = "1") int regionCount,
            @UnitedSetting(key = "titles") List<String> titles) {
    }

    public record UniqueTitleDefinition(
            @UnitedSetting(key = "display-name", def = "Unnamed Title") String displayName,
            @UnitedSetting(key = "description", def = "") String description,
            @UnitedSetting(key = "effects-description", def = "") String effectsDescription,
            @UnitedSetting(key = "primary-regions") List<String> primaryRegions,
            @UnitedSetting(key = "secondary-regions") List<String> secondaryRegions,
            @UnitedSetting(key = "required-secondary-regions", def = "1") int requiredSecondaryRegions,
            @UnitedSection(key = "effects") UniqueTitleEffects effects) {
    }

    record UniqueTitleEffects(
            @UnitedSetting(key = "country-title-unlocks") List<String> countryTitleUnlocks,
            @UnitedSetting(key = "cosmetic-unlocks") List<String> cosmeticUnlocks,
            @UnitedSection(key = "modifiers") UnitedDynamicSection<UniqueTitleEffectsModifier> modifiers) {
    }

    record UniqueTitleEffectsModifier(
            @UnitedSetting(key = "attribute-key") String attributeKey,
            @UnitedSetting(key = "mode", def = "ADD") Mode mode,
            @UnitedSetting(key = "value-modifier") double valueModifier,
            @UnitedSetting(key = "min-value-modifier") double minValueModifier,
            @UnitedSetting(key = "max-value-modifier") double maxValueModifier,
            @UnitedSetting(key = "daily-change-modifier") double dailyChangeModifier) {
    }

}
