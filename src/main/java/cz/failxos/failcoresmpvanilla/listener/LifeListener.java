package cz.failxos.failcoresmpvanilla.listener;

import cz.failxos.failcoresmpvanilla.FailCoreSMPVanilla;
import cz.failxos.failcoresmpvanilla.HeartItem;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/** PvP LifeSteal: presun jednoho maximalniho srdce z obeti do itemu viteze. */
public final class LifeListener implements Listener {

    private final FailCoreSMPVanilla plugin;
    private final CombatListener combat;
    private record KillPair(UUID killer, UUID victim) {}
    private final Map<KillPair, Long> lastTransfer = new HashMap<>();

    public LifeListener(FailCoreSMPVanilla plugin, CombatListener combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        FileConfiguration cfg = plugin.getConfig();

        if (killer != null && !killer.equals(victim)) {
            boolean logout = combat.isLogoutDeath(victim.getUniqueId());

            // Veci zabiteho hrace mu zustavaji (pri combat logu se normalne dropnou)
            if (cfg.getBoolean("pvp.keep-items-on-death", true) && !logout
                    && !victim.hasPermission(FailCoreSMPVanilla.BYPASS)) {
                e.setKeepInventory(true);
                e.getDrops().clear();
                e.setKeepLevel(true);
                e.setDroppedExp(0);
                plugin.messages().send(killer, "items-kept", "%player%", victim.getName());
            }

            if (cfg.getBoolean("life.enabled", true)) {
                pvpHeart(killer, victim, cfg);
            }
        }

    }

    private void pvpHeart(Player killer, Player victim, FileConfiguration cfg) {
        AttributeInstance attr = victim.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        double min = Math.max(2.0, cfg.getDouble("life.min-hp", 2.0));
        double base = attr.getBaseValue();
        if (base - 2.0 < min) {
            plugin.messages().send(killer, "life-victim-min", "%player%", victim.getName());
            return;
        }
        long now = System.nanoTime();
        long cooldown = Math.max(0, cfg.getInt("life.same-victim-cooldown-seconds", 30)) * 1_000_000_000L;
        lastTransfer.entrySet().removeIf(entry -> now - entry.getValue() >= cooldown);
        KillPair pair = new KillPair(killer.getUniqueId(), victim.getUniqueId());
        Long previous = lastTransfer.get(pair);
        if (previous != null && now - previous < cooldown) {
            long remaining = (cooldown - (now - previous) + 999_999_999L) / 1_000_000_000L;
            plugin.messages().send(killer, "life-cooldown", "%player%", victim.getName(), "%time%", Long.toString(remaining));
            return;
        }
        attr.setBaseValue(base - 2.0);
        if (cooldown > 0) lastTransfer.put(pair, now);
        plugin.messages().send(victim, "life-lost", "%player%", killer.getName());
        Map<Integer, ItemStack> left = killer.getInventory().addItem(HeartItem.create(plugin, 1));
        for (ItemStack rest : left.values()) {
            killer.getWorld().dropItemNaturally(killer.getLocation(), rest);
        }
        plugin.messages().send(killer, "life-gained", "%player%", victim.getName());
    }
}
