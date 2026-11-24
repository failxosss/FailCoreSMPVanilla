package cz.failxos.failcoresmpvanilla.listener;

import cz.failxos.failcoresmpvanilla.FailCoreSMPVanilla;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Na mace nejde dat enchant v kovadline ani v enchanting table. */
public final class MaceEnchantListener implements Listener {

    private final FailCoreSMPVanilla plugin;

    public MaceEnchantListener(FailCoreSMPVanilla plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("mace.block-enchants", true);
    }

    private static boolean isMace(ItemStack item) {
        return item != null && item.getType() == Material.MACE;
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        if (!enabled()) {
            return;
        }
        ItemStack result = e.getResult();
        if (!isMace(result)) {
            return;
        }
        if (e.getView().getPlayer().hasPermission(FailCoreSMPVanilla.BYPASS)) {
            return;
        }
        ItemStack first = e.getInventory().getItem(0);
        Map<Enchantment, Integer> before = first == null ? Map.of() : first.getEnchantments();
        if (!result.getEnchantments().equals(before)) {
            e.setResult(null);
        }
    }

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent e) {
        if (enabled() && isMace(e.getItem()) && !e.getEnchanter().hasPermission(FailCoreSMPVanilla.BYPASS)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent e) {
        if (enabled() && isMace(e.getItem()) && !e.getEnchanter().hasPermission(FailCoreSMPVanilla.BYPASS)) {
            e.setCancelled(true);
        }
    }
}
