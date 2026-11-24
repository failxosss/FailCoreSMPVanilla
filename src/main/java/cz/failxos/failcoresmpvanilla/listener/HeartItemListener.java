package cz.failxos.failcoresmpvanilla.listener;

import cz.failxos.failcoresmpvanilla.FailCoreSMPVanilla;
import cz.failxos.failcoresmpvanilla.HeartItem;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Item Srdce: nejde snist ani pouzit na zvirata, pravym klikem prida 1 srdce (max zivoty). */
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
        if (!HeartItem.isHeart(plugin, e.getItem())) {
            return;
        }
        // zabrani zacatku "jedeni" (animace), interakce s blokem zustava
        e.setUseItemInHand(Event.Result.DENY);

        // klik na truhlu, dvere apod. srdce nespotrebuje
        if (a == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
                && e.getClickedBlock().getType().isInteractable() && !e.getPlayer().isSneaking()) {
            return;
        }
        EquipmentSlot hand = e.getHand();
        if (hand == null) {
            return;
        }
        use(e.getPlayer(), hand);
    }

    private void use(Player p, EquipmentSlot hand) {
        ItemStack inHand = p.getInventory().getItem(hand);
        if (!HeartItem.isHeart(plugin, inHand)) {
            return;
        }
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        double max = plugin.getConfig().getDouble("life.max-hp", 40.0);
        double base = attr.getBaseValue();
        if (base >= max) {
            plugin.messages().send(p, "life-max");
            return;
        }
        double updated = Math.min(max, base + 2.0);
        attr.setBaseValue(updated);
        p.setHealth(Math.min(updated, p.getHealth() + 2.0));

        if (inHand.getAmount() <= 1) {
            p.getInventory().setItem(hand, null);
        } else {
            inHand.setAmount(inHand.getAmount() - 1);
            p.getInventory().setItem(hand, inHand);
        }
        plugin.messages().send(p, "life-used", "%hearts%", String.valueOf((int) (updated / 2.0)));
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
