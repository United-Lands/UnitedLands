package org.unitedlands.unitedlands.classes;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig.BlockRestrictionSettings;
import org.unitedlands.unitedlands.classes.configs.UnitedLandsConfig.SettlementChunkSettings;
import org.unitedlands.utils.United;

public record PlotType(
        String name,
        int maxPerSettlement,
        int breakPermissions,
        int placePermissions,
        int containerPermissions,
        int switchPermissions,
        int blockUsePermissions,
        int interactPermissions,
        PlotTypeFlag pvp,
        PlotTypeFlag monsters,
        PlotTypeFlag animals,
        PlotTypeFlag fire,
        PlotTypeFlag explosions,
        int restrictionExempt,
        Set<Material> breakWhitelist,
        Set<Material> placeWhitelist,
        Set<EntityType> interactWhitelist,
        Set<PlotTypeBehavior> behaviors) {

    public static PlotType fromSettings(String name, SettlementChunkSettings settings) {
        var perms = settings.permissions();
        var flags = settings.flags();
        var restrictions = settings.restrictions();
        return new PlotType(
                name,
                settings.maxPerSettlement(),
                LocationMembership.fromNames(perms.breakPermissions(), name),
                LocationMembership.fromNames(perms.placePermissions(), name),
                LocationMembership.fromNames(perms.containerPermissions(), name),
                LocationMembership.fromNames(perms.switchPermissions(), name),
                LocationMembership.fromNames(perms.blockUsePermissions(), name),
                LocationMembership.fromNames(perms.interactPermissions(), name),
                orInherit(flags.pvp()),
                orInherit(flags.monsters()),
                orInherit(flags.animals()),
                orInherit(flags.fire()),
                orInherit(flags.explosions()),
                LocationMembership.fromNames(restrictions.exempt(), name),
                buildMaterialWhitelist(restrictions.breakRestriction(), name),
                buildMaterialWhitelist(restrictions.placeRestriction(), name),
                buildEntityWhitelist(restrictions.interactEntities()),
                buildBehaviors(settings.behaviors()));
    }

    public boolean hasBehavior(PlotTypeBehavior behavior) {
        return behaviors.contains(behavior);
    }

    public boolean allowsMaterial(PermissionType type, Material material, int membership) {
        var whitelist = switch (type) {
            case BREAK -> breakWhitelist;
            case PLACE -> placeWhitelist;
            default -> null;
        };
        if (whitelist == null || isExempt(membership))
            return true;
        return material != null && whitelist.contains(material);
    }

    public boolean allowsEntity(PermissionType type, EntityType entityType, int membership) {
        if (type != PermissionType.INTERACT || interactWhitelist == null || isExempt(membership))
            return true;
        return entityType != null && interactWhitelist.contains(entityType);
    }

    private boolean isExempt(int membership) {
        return (restrictionExempt & membership) != 0;
    }

    private static PlotTypeFlag orInherit(PlotTypeFlag flag) {
        return flag != null ? flag : PlotTypeFlag.INHERIT;
    }

    private static Set<Material> buildMaterialWhitelist(BlockRestrictionSettings settings, String source) {
        var materials = settings.materials() != null ? settings.materials() : List.<Material>of();
        var tags = settings.tags() != null ? settings.tags() : List.<String>of();
        if (materials.isEmpty() && tags.isEmpty())
            return null;

        var result = EnumSet.noneOf(Material.class);
        result.addAll(materials);
        for (var strTag : tags) {
            var key = NamespacedKey.fromString(strTag.toLowerCase());
            var tag = key != null ? Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class) : null;
            if (tag != null) {
                result.addAll(tag.getValues());
            } else {
                United.logger().warning("Unknown block tag '" + strTag + "' in plot type " + source, "UnitedLands");
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Set<PlotTypeBehavior> buildBehaviors(List<PlotTypeBehavior> behaviors) {
        if (behaviors == null || behaviors.isEmpty())
            return Collections.emptySet();
        return Collections.unmodifiableSet(EnumSet.copyOf(behaviors));
    }

    private static Set<EntityType> buildEntityWhitelist(List<EntityType> entities) {
        if (entities == null || entities.isEmpty())
            return null;
        return Collections.unmodifiableSet(EnumSet.copyOf(entities));
    }

}
