package org.unitedlands.unitedlands.utils;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unitedlands.utils.United;

public class ColorUtils {

    public static Color hexToColor(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        return switch (hex.length()) {
            case 6 -> new Color(
                    Integer.valueOf(hex.substring(0, 2), 16),
                    Integer.valueOf(hex.substring(2, 4), 16),
                    Integer.valueOf(hex.substring(4, 6), 16));
            case 8 -> new Color(
                    Integer.valueOf(hex.substring(0, 2), 16),
                    Integer.valueOf(hex.substring(2, 4), 16),
                    Integer.valueOf(hex.substring(4, 6), 16),
                    Integer.valueOf(hex.substring(6, 8), 16));
            default -> throw new IllegalArgumentException("Invalid hex color: " + hex);
        };
    }

    public static String argbToHex(int argb) {
        return String.format("#%02X%02X%02X",
                (argb >> 16) & 0xFF,
                (argb >> 8) & 0xFF,
                argb & 0xFF);
    }

    private static final String HEX_WEBCOLOR_PATTERN = "^#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})$";
    private static final Pattern hexPattern = Pattern.compile(HEX_WEBCOLOR_PATTERN);

    private static final String COLOUR_CHARACTERS_PATTERN = "[$&<>.;:#]";
    private static final Pattern colorPattern = Pattern.compile(COLOUR_CHARACTERS_PATTERN);

    public static boolean isValidHexColor(final String colorCode) {
        Matcher matcher = hexPattern.matcher(colorCode);
        return matcher.matches();
    }

    public static boolean containsColourCharacters(final String str) {
        United.logger().debug("Testing " + str);
        Matcher matcher = colorPattern.matcher(str);
        return matcher.find();
    }

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)[§&][0-9a-fk-orx]");

    public static String stripAllColors(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String cleanLegacy = LEGACY_COLOR_PATTERN.matcher(input).replaceAll("");
        return United.miniMessage().get().stripTags(cleanLegacy);
    }
}
