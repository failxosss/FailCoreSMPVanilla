package cz.failxos.failcoresmpvanilla.listener;

import cz.failxos.failcoresmpvanilla.FailCoreSMPVanilla;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cooldown na ender perly (30s) a wind charge (5s). */
public final class CooldownListener implements Listener {

    private enum Kind {
        PEARL(Material.ENDER_PEARL, EntityType.ENDER_PEARL, "ender-pearl", "pearl-cooldown"),
        WIND(Material.WIND_CHARGE, EntityType.WIND_CHARGE, "wind-charge", "wind-cooldown");

        final Material material;
        final EntityType entity;
        final String configKey;
        final String messageKey;

        Kind(Material material, EntityType entity, String configKey, String messageKey) {
            this.material = material;
            this.entity = entity;
            this.configKey = configKey;
            this.messageKey = messageKey;
        }
    }

    private final FailCoreSMPVanilla plugin;
    private final Map<Kind, Map<UUID, Long>> expiry = new EnumMap<>(Kind.class);

    public CooldownListener(FailCoreSMPVanilla plugin) {
        this.plugin = plugin;
        for (Kind kind : Kind.values()) {
            expiry.put(kind, new HashMap<>());
        }
    }

    private static Kind kindOf(Material material) {
        for (Kind kind : Kind.values()) {
            if (kind.material == material) {
                return kind;
            }
        }
        return null;
    }

    private static Kind kindOf(EntityType type) {
        for (Kind kind : Kind.values()) {
            if (kind.entity == type) {
                return kind;
            }
        }
        return null;
    }

    private long remaining(Kind kind, UUID id) {
        Long until = expiry.get(kind).get(id);
        if (until == null) {
            return 0L;
        }
        long left = until - System.currentTimeMillis();
        if (left <= 0) {
            expiry.get(kind).remove(id);
            return 0L;
        }
        return left;
    }

    private static String seconds(long millis) {
        return String.valueOf((millis + 999L) / 1000L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = e.getItem();
        if (item == null) {
            return;
        }
        Kind kind = kindOf(item.getType());
        if (kind == null) {
            return;
        }
        Player p = e.getPlayer();
        if (p.hasPermission(FailCoreSMPVanilla.BYPASS)) {
            return;
        }
        long left = remaining(kind, p.getUniqueId());
        if (left > 0) {
            e.setUseItemInHand(Event.Result.DENY);
            plugin.messages().actionBar(p, kind.messageKey, "%time%", seconds(left));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player p)) {
            return;
        }
        Kind kind = kindOf(e.getEntityType());
        if (kind == null || p.hasPermission(FailCoreSMPVanilla.BYPASS)) {
            return;
        }
        long left = remaining(kind, p.getUniqueId());
        if (left > 0) {
            e.setCancelled(true);
            plugin.messages().actionBar(p, kind.messageKey, "%time%", seconds(left));
            return;
        }
        int secs = plugin.getConfig().getInt("cooldowns." + kind.configKey, 0);
        if (secs <= 0) {
            return;
        }
        expiry.get(kind).put(p.getUniqueId(), System.currentTimeMillis() + secs * 1000L);
        p.setCooldown(kind.material, secs * 20);
    }

    /** Po relogu se znovu ukaze zbyvajici cooldown u itemu. */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        for (Kind kind : Kind.values()) {
            long left = remaining(kind, p.getUniqueId());
            if (left > 0) {
                p.setCooldown(kind.material, (int) Math.max(1L, left / 50L));
            }
        }
    }
}
