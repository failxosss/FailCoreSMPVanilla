package cz.failxos.failcoresmpvanilla.listener;

import cz.failxos.failcoresmpvanilla.FailCoreSMPVanilla;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Combat tag: casovac, zakaz prikazu, smrt pri odpojeni. */
public final class CombatListener implements Listener {

    private final FailCoreSMPVanilla plugin;
    private final Map<UUID, Long> tagged = new HashMap<>();
    private final Set<UUID> logoutDeaths = new HashSet<>();

    public CombatListener(FailCoreSMPVanilla plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(), 20L, 20L);
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("combat.enabled", true);
    }

    public boolean isTagged(Player p) {
        Long until = tagged.get(p.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    /** True jen v okamziku, kdy je hrac zabit kvuli odpojeni v combatu. */
    public boolean isLogoutDeath(UUID id) {
        return logoutDeaths.contains(id);
    }

    public void clear() {
        tagged.clear();
    }

    private static Player attackerOf(Entity damager) {
        if (damager instanceof Player direct) {
            return direct;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    private void tag(Player p, String opponent) {
        if (p.hasPermission(FailCoreSMPVanilla.BYPASS)
                || p.getGameMode() == GameMode.CREATIVE
                || p.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        int seconds = plugin.getConfig().getInt("combat.duration-seconds", 30);
        boolean already = isTagged(p);
        tagged.put(p.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
        if (!already) {
            plugin.messages().send(p, "combat-start", "%player%", opponent, "%time%", String.valueOf(seconds));
        }
        plugin.messages().actionBar(p, "combat-actionbar", "%time%", String.valueOf(seconds));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!enabled() || !(e.getEntity() instanceof Player victim)) {
            return;
        }
        if (e.getDamage() <= 0) {
            return;
        }
        Player attacker = attackerOf(e.getDamager());
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        tag(victim, attacker.getName());
        if (plugin.getConfig().getBoolean("combat.tag-attacker", true)) {
            tag(attacker, victim.getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (!enabled() || !isTagged(p) || p.hasPermission(FailCoreSMPVanilla.BYPASS)) {
            return;
        }
        String message = e.getMessage();
        if (message.length() < 2) {
            return;
        }
        String cmd = message.substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        int colon = cmd.indexOf(':');
        if (colon >= 0) {
            cmd = cmd.substring(colon + 1);
        }
        for (String allowed : plugin.getConfig().getStringList("combat.allowed-commands")) {
            if (allowed.equalsIgnoreCase(cmd)) {
                return;
            }
        }
        e.setCancelled(true);
        plugin.messages().send(p, "combat-command-blocked");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        boolean wasTagged = isTagged(p);
        tagged.remove(p.getUniqueId());
        if (!wasTagged || !enabled() || p.isDead() || p.hasPermission(FailCoreSMPVanilla.BYPASS)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("combat.kill-on-logout", true)) {
            return;
        }
        // kick od admina / vypnuti serveru neni combat log
        if (e.getReason() == PlayerQuitEvent.QuitReason.KICKED) {
            return;
        }
        UUID id = p.getUniqueId();
        logoutDeaths.add(id);
        try {
            p.setHealth(0.0);
        } finally {
            logoutDeaths.remove(id);
        }
        Bukkit.getServer().sendMessage(plugin.messages().prefixed("combat-logout", "%player%", p.getName()));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        tagged.remove(e.getEntity().getUniqueId());
    }

    private void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = tagged.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) {
                it.remove();
                continue;
            }
            long left = entry.getValue() - now;
            if (left <= 0) {
                it.remove();
                plugin.messages().send(p, "combat-end");
            } else {
                plugin.messages().actionBar(p, "combat-actionbar", "%time%", String.valueOf((left + 999L) / 1000L));
            }
        }
    }
}
