package org.unitedlands.unitedlands.classes;

import java.util.ArrayList;
import java.util.List;

import org.unitedlands.utils.United;

public class LocationMembership {
    public static final int UNSET = 0;                      // 0
    public static final int FOREIGNER = 1 << 0;             // 1
    public static final int OWNER = 1 << 1;                 // 2
    public static final int SETTLEMENT_RESIDENT = 1 << 2;   // 4
    public static final int REGION_RESIDENT = 1 << 3;       // 8
    public static final int COUNTRY_RESIDENT = 1 << 4;      // 16
    public static final int TRUSTED = 1 << 5;               // 32
    public static final int ALLY = 1 << 6;                  // 64
    public static final int ENEMY = 1 << 7;                 // 128
    public static final int OUTLAW = 1 << 8;                // 256

    public static int fromNames(List<String> names, String source) {
        int result = UNSET;
        if (names == null)
            return result;

        for (var name : names) {
            switch (name.toUpperCase()) {
                case "FOREIGNER" -> result |= FOREIGNER;
                case "OWNER" -> result |= OWNER;
                case "SETTLEMENT_RESIDENT" -> result |= SETTLEMENT_RESIDENT;
                case "REGION_RESIDENT" -> result |= REGION_RESIDENT;
                case "COUNTRY_RESIDENT" -> result |= COUNTRY_RESIDENT;
                case "TRUSTED" -> result |= TRUSTED;
                case "ALLY" -> result |= ALLY;
                case "ENEMY" -> result |= ENEMY;
                case "OUTLAW" -> result |= OUTLAW;
                default -> United.logger().warning("Unknown membership '" + name + "' in " + source, "UnitedLands");
            }
        }
        return result;
    }

    public static String toString(int membership) {
        if (membership == UNSET)
            return "UNSET";

        List<String> flags = new ArrayList<>();
        if ((membership & FOREIGNER) != 0)
            flags.add("FOREIGNER");
        if ((membership & OWNER) != 0)
            flags.add("OWNER");
        if ((membership & SETTLEMENT_RESIDENT) != 0)
            flags.add("SETTLEMENT_RESIDENT");
        if ((membership & REGION_RESIDENT) != 0)
            flags.add("REGION_RESIDENT");
        if ((membership & COUNTRY_RESIDENT) != 0)
            flags.add("COUNTRY_RESIDENT");
        if ((membership & TRUSTED) != 0)
            flags.add("TRUSTED");
        if ((membership & ALLY) != 0)
            flags.add("ALLY");
        if ((membership & ENEMY) != 0)
            flags.add("ENEMY");
        if ((membership & OUTLAW) != 0)
            flags.add("OUTLAW");

        return String.join(" | ", flags);
    }

    public static String toInfoScreenString(int membership) {
        if (membership == UNSET)
            return "<gray>-</gray>";

        List<String> flags = new ArrayList<>();

        if ((membership & SETTLEMENT_RESIDENT) != 0)
            flags.add("<blue>S</blue>");
        else
            flags.add("-");
        if ((membership & REGION_RESIDENT) != 0)
            flags.add("<red>R</red>");
        else
            flags.add("-");
        if ((membership & COUNTRY_RESIDENT) != 0)
            flags.add("<green>C</green>");
        else
            flags.add("-");
        if ((membership & FOREIGNER) != 0)
            flags.add("<gray>F</gray>");
        else
            flags.add("-");

        return String.join("", flags);
    }

}
