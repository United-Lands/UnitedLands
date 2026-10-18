package org.unitedlands.unitedlands.classes;

public class ChatChannel {

    private final String key;
    private final String tag;
    private final String prefix;
    private final boolean notifyJoin;
    private final boolean removeEmpty;

    private String color;
    private String password;

    public ChatChannel(String key, String tag, String color, String prefix, boolean notifyJoin, boolean removeEmpty) {
        this.key = key;
        this.tag = tag;
        this.color = color;
        this.prefix = prefix;
        this.notifyJoin = notifyJoin;
        this.removeEmpty = removeEmpty;
    }

    public String getKey() {
        return key;
    }

    public String getTag() {
        return tag;
    }

    public String getColor() {
        return color != null ? color : "white";
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getPrefix() {
        return prefix != null ? prefix : "";
    }

    public boolean notifyJoin() {
        return notifyJoin;
    }

    public boolean removeEmpty() {
        return removeEmpty;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean hasPassword() {
        return password != null;
    }

}

// public enum ChatChannel {

// GLOBAL("white", "[<white><bold>G</bold></white>] %luckperms_prefix% ",
// false),
// SETTLEMENT("blue", "[<blue><bold>S</bold></blue>] %luckperms_prefix% ",
// true),
// COUNTRY("green", "[<green><bold>C</bold></green>] %luckperms_prefix% " ,
// true),
// STAFF("dark_green", "[<dark_green><bold>STAFF</bold></dark_green>]
// %luckperms_prefix% ", false),
// LOCAL("gold", "[<gold><bold>L</bold></gold>] %luckperms_prefix% ", false);

// private final String color;
// private final String prefix;
// private final boolean removeEmpty;

// private ChatChannel(String color, String prefix, boolean removeEmpty) {
// this.color = color;
// this.prefix = prefix;
// this.removeEmpty = removeEmpty;
// }

// public String getColor() {
// return color;
// }

// public String getPrefix() {
// return prefix;
// }

// public boolean removeEmpty() {
// return removeEmpty;
// }
// }
