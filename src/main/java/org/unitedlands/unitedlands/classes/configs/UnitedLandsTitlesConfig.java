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

    @UnitedSection(key = "titles")
    UnitedDynamicSection<ClaimableTitleDefinition> titles();

    record ClaimableTitleDefinition(
            @UnitedSetting(key = "display-name", def = "Unnamed Title") String displayName,
            @UnitedSetting(key = "description", def = "") String description,
            @UnitedSetting(key = "effects-description", def = "") String effectsDescription,
            @UnitedSetting(key = "primary-regions") List<String> primaryRegions,
            @UnitedSetting(key = "secondary-regions") List<String> secondaryRegions,
            @UnitedSetting(key = "required-secondary-regions", def = "1") int requiredSecondaryRegions,
            @UnitedSection(key = "effects") ClaimableTitleEffects effects) {
    }

    record ClaimableTitleEffects(
            @UnitedSetting(key = "country-title-unlocks") List<String> countryTitleUnlocks,
            @UnitedSetting(key = "cosmetic-unlocks") List<String> cosmeticUnlocks,
            @UnitedSection(key = "modifiers") UnitedDynamicSection<ClaimableTitleEffectsModifier> modifiers) {
    }

    record ClaimableTitleEffectsModifier(
            @UnitedSetting(key = "attribute-key") String attributeKey,
            @UnitedSetting(key = "mode", def = "ADD") Mode mode,
            @UnitedSetting(key = "value-modifier") double valueModifier,
            @UnitedSetting(key = "min-value-modifier") double minValueModifier,
            @UnitedSetting(key = "max-value-modifier") double maxValueModifier,
            @UnitedSetting(key = "daily-change-modifier") double dailyChangeModifier) {
    }

}
