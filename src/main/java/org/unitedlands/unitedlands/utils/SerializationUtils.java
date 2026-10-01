package org.unitedlands.unitedlands.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.unitedlands.unitedlands.classes.db.Identifiable;
import org.unitedlands.utils.United;

public class SerializationUtils {

    public static String serializeLocation(Location l) {
        return l.getWorld().getName() + ";"
                + l.getX() + ";"
                + l.getY() + ";"
                + l.getZ() + ";"
                + l.getYaw() + ";"
                + l.getPitch();
    }

    public static Location deserializeLocation(String s) {
        try {
            var ls = s.split(";");
            return new Location(
                    Bukkit.getWorld(ls[0]),
                    Double.parseDouble(ls[1]),
                    Double.parseDouble(ls[2]),
                    Double.parseDouble(ls[3]),
                    Float.parseFloat(ls[4]),
                    Float.parseFloat(ls[5]));
        } catch (Exception ex) {
            United.logger().error("Parsing error in location: " + ex.getMessage());
            return null;
        }
    }

    public static String serializeIdentifiableList(Collection<? extends Identifiable> list) {
        if (list != null && !list.isEmpty()) {
            try {
                return list.stream()
                        .map(c -> c.getUuid().toString())
                        .collect(Collectors.joining("#"));
            } catch (Exception ex) {
                United.logger().error("Serialization error in serializeIdentifiableList: " + ex.getMessage());
                return null;
            }
        }
        return null;
    }

     public static String serializeUuidList(Collection<UUID> list) {
        if (list != null && !list.isEmpty()) {
            try {
                return list.stream()
                        .map(UUID::toString)
                        .collect(Collectors.joining("#"));
            } catch (Exception ex) {
                United.logger().error("Serialization error in serializeUuidList: " + ex.getMessage());
                return null;
            }
        }
        return null;
    }

    public static String serializeUuidList(Collection<? extends Identifiable> list, Supplier<UUID> supplier) {
        if (list != null && !list.isEmpty()) {
            try {
                return list.stream()
                        .map(c -> supplier.get().toString())
                        .collect(Collectors.joining("#"));
            } catch (Exception ex) {
                United.logger().error("Serialization error in serializeUuidList: " + ex.getMessage());
                return null;
            }
        }
        return null;
    }

    
    public static Set<UUID> deSerializeUuidListToSet(String serialized) {
        if (serialized != null) {
            try {
                return Arrays.stream(serialized.split("#"))
                        .map(c -> UUID.fromString(c))
                        .collect(Collectors.toSet());
            } catch (Exception ex) {
                United.logger().error("Deserialization error in deserializeUuidListToSet: " + ex.getMessage());
                return new HashSet<UUID>();
            }
        } else {
            return new HashSet<UUID>();
        }
    }

    public static <T> Set<T> deserializeUuidListToSet(String serialized, Function<UUID, T> supplier) {
        if (serialized != null) {
            try {
                return Arrays.stream(serialized.split("#"))
                        .map(c -> supplier.apply(UUID.fromString(c)))
                        .collect(Collectors.toSet());
            } catch (Exception ex) {
                United.logger().error("Deserialization error in deserializeUuidListToSet: " + ex.getMessage());
                return new HashSet<T>();
            }
        } else {
            return new HashSet<T>();
        }
    }

    public static String serializeStringIntegerMap(Map<String, Integer> map) {
        if (map != null && !map.isEmpty()) {
            try {
                return map.entrySet().stream()
                        .map(entry -> entry.getKey() + ":" + (entry.getValue() == null ? "null" : entry.getValue()))
                        .collect(Collectors.joining("#"));
            } catch (Exception ex) {
                United.logger().error("Serialization error in serializeStringIntegerMap: " + ex.getMessage());
                return null;
            }
        }
        return null;
    }

    public static Map<String, Integer> deserializeStringIntegerMap(String serialized) {
        if (serialized != null) {
            try {
                Map<String, Integer> values = new HashMap<>();
                Arrays.stream(serialized.split("#"))
                        .filter(entry -> entry != null && !entry.isEmpty())
                        .map(entry -> entry.split(":", 2))
                        .filter(parts -> parts.length == 2)
                        .forEach(parts -> values.put(parts[0], "null".equalsIgnoreCase(parts[1]) ? null : Integer.parseInt(parts[1])));
                return values;
            } catch (Exception ex) {
                United.logger().error("Deserialization error in serializeStringIntegerMap: " + ex.getMessage());
                return new HashMap<>();
            }
        } else {
            return new HashMap<>();
        }
    }

}
