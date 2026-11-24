package cz.failxos.failcoresmpvanilla.listener;

import cz.failxos.failcoresmpvanilla.FailCoreSMPVanilla;
import cz.failxos.failcoresmpvanilla.HeartItem;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Item Srdce nejde snist ani pouzit na zvirata - bez jakekoli zpravy. */
public final class HeartItemListener implements Listener {

    private final FailCoreSMPVanilla plugin;

    public HeartItemListener(FailCoreSMPVanilla plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        if (HeartItem.isHeart(plugin, e.getItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (HeartItem.isHeart(plugin, e.getItem())) {
            // zabrani zacatku "jedeni" (animace), interakce s blokem zustava
            e.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        EquipmentSlot hand = e.getHand();
        var inv = e.getPlayer().getInventory();
        var item = hand == EquipmentSlot.OFF_HAND ? inv.getItemInOffHand() : inv.getItemInMainHand();
        if (HeartItem.isHeart(plugin, item)) {
            e.setCancelled(true);
        }
    }
}
