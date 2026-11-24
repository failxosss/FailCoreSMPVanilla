package cz.failxos.failcoresmpvanilla.listener;

import cz.failxos.failcoresmpvanilla.FailCoreSMPVanilla;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Ochrana proti string duperum (piston + tripwire, strojove farmy stringu). */
public final class StringDupeListener implements Listener {

    private static final class Window {
        long start;
        int count;

        Window(long start) {
            this.start = start;
        }
    }

    private final FailCoreSMPVanilla plugin;
    private final Map<String, Window> windows = new HashMap<>();
    /** Chunky, kde se prave rusi hracem/smrti - tam se limit neuplatnuje (rozbity chest, smrt hrace). */
    private final Map<String, Long> grace = new HashMap<>();

    public StringDupeListener(FailCoreSMPVanilla plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> cleanup(), 1200L, 1200L);
    }

    private static String chunkKey(Location loc) {
        return loc.getWorld().getName() + ':' + (loc.getBlockX() >> 4) + ':' + (loc.getBlockZ() >> 4);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        long windowMs = Math.max(1, plugin.getConfig().getInt("string-dupe.window-seconds", 10)) * 2000L;
        windows.values().removeIf(w -> now - w.start > windowMs);
        grace.values().removeIf(until -> until < now);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("string-dupe.enabled", true) || !c.getBoolean("string-dupe.block-piston-tripwire", true)) {
            return;
        }
        BlockFace face = e.getDirection();
        Block piston = e.getBlock();
        int steps = e.getBlocks().size() + 1;
        for (int i = 1; i <= steps; i++) {
            if (piston.getRelative(face, i).getType() == Material.TRIPWIRE) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        grace.put(chunkKey(e.getBlock().getLocation()), System.currentTimeMillis() + 500L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent e) {
        grace.put(chunkKey(e.getEntity().getLocation()), System.currentTimeMillis() + 500L);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("string-dupe.enabled", true) || !c.getBoolean("string-dupe.limit-spawns", true)) {
            return;
        }
        ItemStack stack = e.getEntity().getItemStack();
        if (stack.getType() != Material.STRING) {
            return;
        }
        long now = System.currentTimeMillis();
        String key = chunkKey(e.getLocation());
        Long graceUntil = grace.get(key);
        if (graceUntil != null && graceUntil > now) {
            return;
        }
        long windowMs = Math.max(1, c.getInt("string-dupe.window-seconds", 10)) * 1000L;
        int max = c.getInt("string-dupe.max-per-chunk", 256);
        Window w = windows.computeIfAbsent(key, k -> new Window(now));
        if (now - w.start > windowMs) {
            w.start = now;
            w.count = 0;
        }
        w.count += stack.getAmount();
        if (w.count > max) {
            e.setCancelled(true);
        }
    }
}
