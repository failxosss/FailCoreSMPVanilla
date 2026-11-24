package cz.failxos.failcoresmpvanilla.listener;

import cz.failxos.failcoresmpvanilla.FailCoreSMPVanilla;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Zakazane itemy: jakmile je hrac vezme do ruky, zmizi a napise se mu zprava. */
public final class BannedItemsListener implements Listener {

    private static final Set<Material> POTION_ITEMS = EnumSet.of(
            Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION, Material.TIPPED_ARROW);

    private final FailCoreSMPVanilla plugin;
    private Set<Material> materials = EnumSet.noneOf(Material.class);
    private List<String> potions = List.of();

    public BannedItemsListener(FailCoreSMPVanilla plugin) {
        this.plugin = plugin;
        reload();
        Bukkit.getScheduler().runTaskTimer(plugin, () -> enforce(), 20L, 2L);
    }

    public void reload() {
        Set<Material> set = EnumSet.noneOf(Material.class);
        for (String name : plugin.getConfig().getStringList("banned-items.materials")) {
            Material m = Material.matchMaterial(name);
            if (m == null) {
                plugin.getLogger().warning("Neznamy material v banned-items.materials: " + name);
            } else {
                set.add(m);
            }
        }
        materials = set;
        potions = plugin.getConfig().getStringList("banned-items.potions").stream()
                .map(s -> s.toLowerCase(Locale.ROOT)).toList();
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("banned-items.enabled", true);
    }

    private boolean isBanned(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        Material type = item.getType();
        if (materials.contains(type)) {
            return true;
        }
        if (!potions.isEmpty() && POTION_ITEMS.contains(type) && item.getItemMeta() instanceof PotionMeta meta) {
            PotionType base = meta.getBasePotionType();
            if (base != null) {
                String key = base.getKey().getKey();
                for (String banned : potions) {
                    if (key.contains(banned)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String readable(ItemStack item) {
        String name = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        if (POTION_ITEMS.contains(item.getType()) && item.getItemMeta() instanceof PotionMeta meta
                && meta.getBasePotionType() != null) {
            name = name + " (" + meta.getBasePotionType().getKey().getKey().replace('_', ' ') + ")";
        }
        return name;
    }

    private void enforce() {
        if (!enabled()) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerInventory inv = p.getInventory();
            ItemStack main = inv.getItemInMainHand();
            ItemStack off = inv.getItemInOffHand();
            boolean mainBanned = isBanned(main);
            boolean offBanned = isBanned(off);
            if ((!mainBanned && !offBanned) || p.hasPermission(FailCoreSMPVanilla.BYPASS)) {
                continue;
            }
            if (mainBanned) {
                inv.setItemInMainHand(null);
                plugin.messages().send(p, "banned-item", "%item%", readable(main));
            }
            if (offBanned) {
                inv.setItemInOffHand(null);
                plugin.messages().send(p, "banned-item", "%item%", readable(off));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null || !enabled() || !isBanned(item) || e.getPlayer().hasPermission(FailCoreSMPVanilla.BYPASS)) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent e) {
        if (enabled() && isBanned(e.getItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (!enabled()) {
            return;
        }
        ItemStack result = e.getInventory().getResult();
        if (result != null && isBanned(result) && !e.getView().getPlayer().hasPermission(FailCoreSMPVanilla.BYPASS)) {
            e.getInventory().setResult(null);
        }
    }
}
