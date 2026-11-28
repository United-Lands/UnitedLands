package org.unitedlands.unitedlands.utils;

import java.util.regex.Pattern;

import org.unitedlands.utils.United;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class TagUtils {

    private static final Pattern ILLEGAL_PLAYER_CHARACTERS = Pattern.compile("[<>\\\\§]");

    private TagUtils() {
    }

    public static boolean containsIllegalPlayerCharacters(String input) {
        return ILLEGAL_PLAYER_CHARACTERS.matcher(input).find();
    }

    public static String format(String input) {
        return LegacyFormatUtils.convert(input);
    }

    public static int visibleLength(String formattedTag) {
        var component = United.miniMessage().get().deserialize(formattedTag);
        var plain = PlainTextComponentSerializer.plainText().serialize(component);
        return plain.codePointCount(0, plain.length());
    }

    public static String escape(String input) {
        return United.miniMessage().get().escapeTags(input);
    }
}
