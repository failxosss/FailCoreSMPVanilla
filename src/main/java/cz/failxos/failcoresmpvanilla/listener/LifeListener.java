package cz.failxos.failcoresmpvanilla.listener;

import cz.failxos.failcoresmpvanilla.FailCoreSMPVanilla;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Zabiti hrace = zivot navic, ale veci zabiteho hrace zustavaji jemu. */
public final class LifeListener implements Listener {

    private final FailCoreSMPVanilla plugin;
    private final CombatListener combat;
    /** zabijak -> (obet -> cas posledniho zivota) */
    private final Map<UUID, Map<UUID, Long>> recent = new HashMap<>();

    public LifeListener(FailCoreSMPVanilla plugin, CombatListener combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) {
            return;
        }
        FileConfiguration cfg = plugin.getConfig();
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

        if (!cfg.getBoolean("life.enabled", true) || killer.hasPermission(FailCoreSMPVanilla.BYPASS)) {
            return;
        }

        long cooldownMs = cfg.getLong("life.same-victim-cooldown-seconds", 600L) * 1000L;
        Map<UUID, Long> kills = recent.computeIfAbsent(killer.getUniqueId(), k -> new HashMap<>());
        long now = System.currentTimeMillis();
        Long last = kills.get(victim.getUniqueId());
        if (last != null && now - last < cooldownMs) {
            plugin.messages().send(killer, "life-cooldown", "%player%", victim.getName());
            return;
        }

        AttributeInstance attr = killer.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        double gain = cfg.getDouble("life.gain-hp", 2.0);
        double max = cfg.getDouble("life.max-hp", 40.0);
        double base = attr.getBaseValue();
        if (base >= max) {
            plugin.messages().send(killer, "life-max");
        } else {
            double updated = Math.min(max, base + gain);
            attr.setBaseValue(updated);
            killer.setHealth(Math.min(updated, killer.getHealth() + gain));
            kills.put(victim.getUniqueId(), now);
            plugin.messages().send(killer, "life-gained",
                    "%player%", victim.getName(),
                    "%hearts%", String.valueOf((int) (updated / 2.0)));
        }

        double loss = cfg.getDouble("life.victim-loses-hp", 0.0);
        if (loss > 0) {
            AttributeInstance victimAttr = victim.getAttribute(Attribute.MAX_HEALTH);
            if (victimAttr != null) {
                double min = cfg.getDouble("life.min-hp", 2.0);
                victimAttr.setBaseValue(Math.max(min, victimAttr.getBaseValue() - loss));
                plugin.messages().send(victim, "life-lost", "%player%", killer.getName());
            }
        }
    }
}
