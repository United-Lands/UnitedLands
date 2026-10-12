package org.unitedlands.unitedlands.utils;

import java.util.List;

import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.Region;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsTitlesConfig;

public class ClaimableTitleUtils {

    public record ClaimableTitleValidation(
            List<String> ownedPrimaryRegions,
            List<String> missingPrimaryRegions,
            int requiredPrimaryRegionCount,
            List<String> ownedSecondaryRegions,
            List<String> missingecondaryRegions,
            int requiredSecondaryRegionCount,
            boolean claimeble) {
    }

    public static ClaimableTitleValidation validateTitle(Country country, String titleName) {

        var titleConfig = UnitedLandsTitlesConfig.get().titles().get(titleName);
        
        var ownedRegions = country.getRegions().stream().map(Region::getDefaultName).toList();
        var requiredPrimaryRegions = titleConfig.primaryRegions();
        var requiredSecondaryRegions = titleConfig.secondaryRegions();

        var ownedPrimaryRegions = requiredPrimaryRegions.stream().filter(r -> ownedRegions.contains(r)).toList();
        var missingPrimaryRegions = requiredPrimaryRegions.stream().filter(r -> !ownedRegions.contains(r)).toList();
        var ownedSecondaryRegions = requiredSecondaryRegions.stream().filter(r -> ownedRegions.contains(r)).toList();
        var missingSecondaryRegions = requiredSecondaryRegions.stream().filter(r -> !ownedRegions.contains(r)).toList();

        boolean claimable = missingPrimaryRegions.size() == 0 && ownedSecondaryRegions.size() >= titleConfig.requiredSecondaryRegions();

        return new ClaimableTitleValidation(
            ownedPrimaryRegions, 
            missingPrimaryRegions,
            requiredPrimaryRegions.size(),
            ownedSecondaryRegions,
            missingSecondaryRegions,
            titleConfig.requiredSecondaryRegions(),
            claimable);

    }
}
