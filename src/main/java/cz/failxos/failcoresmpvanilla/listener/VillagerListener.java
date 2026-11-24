package cz.failxos.failcoresmpvanilla.listener;

import cz.failxos.failcoresmpvanilla.FailCoreSMPVanilla;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MerchantRecipe;

/** Villageri a wandering trader maji neomezene trady. */
public final class VillagerListener implements Listener {

    private static final int UNLIMITED = 1_000_000;

    private final FailCoreSMPVanilla plugin;

    public VillagerListener(FailCoreSMPVanilla plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!plugin.getConfig().getBoolean("villagers.unlimited-trades", true)) {
            return;
        }
        if (!(e.getRightClicked() instanceof AbstractVillager villager)) {
            return;
        }
        for (int i = 0; i < villager.getRecipeCount(); i++) {
            MerchantRecipe recipe = villager.getRecipe(i);
            recipe.setMaxUses(UNLIMITED);
            recipe.setUses(0);
            villager.setRecipe(i, recipe);
        }
    }
}
