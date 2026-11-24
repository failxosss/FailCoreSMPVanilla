package cz.failxos.failcoresmpvanilla.listener;

import cz.failxos.failcoresmpvanilla.FailCoreSMPVanilla;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** S elytrou na sobe nejde mit mace ani spear (kopi) v ruce. */
public final class ElytraMaceListener implements Listener {

    private final FailCoreSMPVanilla plugin;

    public ElytraMaceListener(FailCoreSMPVanilla plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> enforce(), 20L, 2L);
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("elytra-mace.enabled", true)
                || plugin.getConfig().getBoolean("elytra-spear.enabled", true);
    }

    private static boolean isMace(ItemStack item) {
        return item != null && item.getType() == Material.MACE;
    }

    /** Vsechny spears (wooden, stone, copper, iron, golden, diamond, netherite). */
    private static boolean isSpear(ItemStack item) {
        return item != null && item.getType().name().endsWith("_SPEAR");
    }

    private boolean isRestricted(ItemStack item) {
        if (isMace(item)) {
            return plugin.getConfig().getBoolean("elytra-mace.enabled", true);
        }
        if (isSpear(item)) {
            return plugin.getConfig().getBoolean("elytra-spear.enabled", true);
        }
        return false;
    }

    private void warn(Player p, ItemStack item) {
        plugin.messages().send(p, isSpear(item) ? "elytra-spear" : "elytra-mace");
    }

    private static boolean wearsElytra(Player p) {
        ItemStack chest = p.getInventory().getChestplate();
        return chest != null && chest.getType() == Material.ELYTRA;
    }

    private boolean applies(Player p) {
        return enabled() && wearsElytra(p) && !p.hasPermission(FailCoreSMPVanilla.BYPASS);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        if (!applies(p)) {
            return;
        }
        ItemStack target = p.getInventory().getItem(e.getNewSlot());
        if (isRestricted(target)) {
            e.setCancelled(true);
            warn(p, target);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (!applies(p)) {
            return;
        }
        ItemStack bad = isRestricted(e.getMainHandItem()) ? e.getMainHandItem()
                : isRestricted(e.getOffHandItem()) ? e.getOffHandItem() : null;
        if (bad != null) {
            e.setCancelled(true);
            warn(p, bad);
        }
    }

    /** Zachyti vsechny ostatni cesty (klik v inventari, nasazeni elytry s mace v ruce, ...). */
    private void enforce() {
        if (!enabled()) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!wearsElytra(p)) {
                continue;
            }
            PlayerInventory inv = p.getInventory();
            boolean main = isRestricted(inv.getItemInMainHand());
            boolean off = isRestricted(inv.getItemInOffHand());
            if (!main && !off) {
                continue;
            }
            if (p.hasPermission(FailCoreSMPVanilla.BYPASS)) {
                continue;
            }
            if (main) {
                evict(p, false);
            }
            if (off) {
                evict(p, true);
            }
        }
    }

    private void evict(Player p, boolean offHand) {
        PlayerInventory inv = p.getInventory();
        ItemStack item = offHand ? inv.getItemInOffHand() : inv.getItemInMainHand();
        if (!isRestricted(item)) {
            return;
        }
        if (offHand) {
            inv.setItemInOffHand(null);
        } else {
            inv.setItemInMainHand(null);
        }

        int held = inv.getHeldItemSlot();
        int target = -1;
        for (int i = 9; i < 36; i++) {
            if (isEmpty(inv.getItem(i))) {
                target = i;
                break;
            }
        }
        if (target < 0) {
            for (int i = 0; i < 9; i++) {
                if (i != held && isEmpty(inv.getItem(i))) {
                    target = i;
                    break;
                }
            }
        }
        if (target >= 0) {
            inv.setItem(target, item);
        } else {
            p.getWorld().dropItemNaturally(p.getLocation(), item);
        }
        warn(p, item);
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
