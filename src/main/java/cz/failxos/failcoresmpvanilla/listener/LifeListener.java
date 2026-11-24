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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Zabiti hrace = zabijak dostane item "Srdce". Zabity hrac s vice nez 10 srdci o srdce prijde
 * (nikdy ne pod 10) a pri smrti dropne cast srdci nad 10. Veci zabiteho hrace zustavaji jemu.
 */
public final class LifeListener implements Listener {

    private final FailCoreSMPVanilla plugin;
    private final CombatListener combat;
    /** zabijak -> (obet -> cas posledniho srdce) */
    private final Map<UUID, Map<UUID, Long>> recent = new HashMap<>();

    public LifeListener(FailCoreSMPVanilla plugin, CombatListener combat) {
        this.plugin = plugin;
        this.combat = combat;
    }

    @EventHandler(priority = EventPriority.HIGH)
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

            if (cfg.getBoolean("life.enabled", true) && !killer.hasPermission(FailCoreSMPVanilla.BYPASS)) {
                pvpHeart(killer, victim, cfg);
            }
        }

        if (cfg.getBoolean("life.enabled", true) && !victim.hasPermission(FailCoreSMPVanilla.BYPASS)) {
            dropSurplusHearts(victim, cfg);
        }
    }

    /** LifeSteal: zabity prijde o srdce (nejnize na 1), zabijak ho dostane jako item. */
    private void pvpHeart(Player killer, Player victim, FileConfiguration cfg) {
        long cooldownMs = cfg.getLong("life.same-victim-cooldown-seconds", 600L) * 1000L;
        Map<UUID, Long> kills = recent.computeIfAbsent(killer.getUniqueId(), k -> new HashMap<>());
        long now = System.currentTimeMillis();
        Long last = kills.get(victim.getUniqueId());
        if (last != null && now - last < cooldownMs) {
            plugin.messages().send(killer, "life-cooldown", "%player%", victim.getName());
            return;
        }

        boolean victimLost = false;
        double loss = cfg.getDouble("life.victim-loses-hp", 2.0);
        AttributeInstance victimAttr = victim.getAttribute(Attribute.MAX_HEALTH);
        if (victimAttr != null && loss > 0) {
            double min = cfg.getDouble("life.min-hp", 2.0);
            double base = victimAttr.getBaseValue();
            if (base > min) {
                victimAttr.setBaseValue(Math.max(min, base - loss));
                victimLost = true;
                plugin.messages().send(victim, "life-lost", "%player%", killer.getName());
            }
        }
        if (!victimLost && cfg.getBoolean("life.steal-only-if-victim-has-hearts", true)) {
            plugin.messages().send(killer, "life-victim-min", "%player%", victim.getName());
            return;
        }
        kills.put(victim.getUniqueId(), now);

        Map<Integer, ItemStack> left = killer.getInventory().addItem(HeartItem.create(plugin, 1));
        for (ItemStack rest : left.values()) {
            killer.getWorld().dropItemNaturally(killer.getLocation(), rest);
        }
        plugin.messages().send(killer, "life-gained", "%player%", victim.getName());
    }

    /** Pri smrti dropne X % srdci nad minimem (napr. 15 srdci -> 5 nad 10 -> 1,5 srdce). */
    private void dropSurplusHearts(Player victim, FileConfiguration cfg) {
        AttributeInstance attr = victim.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        double min = Math.max(cfg.getDouble("life.min-hp", 2.0), cfg.getDouble("life.death-drop-above-hp", 20.0));
        double base = attr.getBaseValue();
        double surplusHearts = (base - min) / 2.0;
        if (surplusHearts <= 0) {
            return;
        }
        double percent = Math.max(0.0, Math.min(100.0, cfg.getDouble("life.death-drop-percent", 30.0)));
        double exact = surplusHearts * percent / 100.0;

        // cele srdce se dropne vzdy, zbytek (napr. 0,5) jako sance
        int hearts = (int) Math.floor(exact);
        if (ThreadLocalRandom.current().nextDouble() < exact - hearts) {
            hearts++;
        }
        hearts = Math.min(hearts, (int) Math.floor(surplusHearts));
        if (hearts <= 0) {
            return;
        }

        // zivoty nikdy neklesnou pod minimum
        attr.setBaseValue(Math.max(min, base - hearts * 2.0));

        int remaining = hearts;
        while (remaining > 0) {
            int stack = Math.min(64, remaining);
            victim.getWorld().dropItemNaturally(victim.getLocation(), HeartItem.create(plugin, stack));
            remaining -= stack;
        }
    }
}
