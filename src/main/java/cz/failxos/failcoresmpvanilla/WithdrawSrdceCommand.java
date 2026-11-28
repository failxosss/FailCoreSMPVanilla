package cz.failxos.failcoresmpvanilla;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class WithdrawSrdceCommand implements CommandExecutor {
    private final FailCoreSMPVanilla plugin;
    public WithdrawSrdceCommand(FailCoreSMPVanilla plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            plugin.messages().send(sender, "withdraw-player-only");
            return true;
        }
        if (args.length != 0) return false;
        if (!plugin.getConfig().getBoolean("life.enabled", true)) {
            plugin.messages().send(p, "withdraw-disabled");
            return true;
        }
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        double min = Math.max(2.0, plugin.getConfig().getDouble("life.min-hp", 2.0));
        if (attr == null || attr.getBaseValue() - 2.0 < min) {
            plugin.messages().send(p, "withdraw-min");
            return true;
        }
        PlayerInventory inv = p.getInventory();
        ItemStack heart = HeartItem.create(plugin, 1);
        ItemStack held = inv.getItemInMainHand();
        int free = -1;
        boolean empty = held.getType().isAir();
        boolean stack = held.isSimilar(heart) && held.getAmount() < held.getMaxStackSize();
        if (!empty && !stack) {
            free = inv.firstEmpty();
            if (free < 0) {
                plugin.messages().send(p, "withdraw-full");
                return true;
            }
        }
        double updated = attr.getBaseValue() - 2.0;
        attr.setBaseValue(updated);
        if (p.getHealth() > attr.getValue()) p.setHealth(attr.getValue());
        if (stack) {
            held.setAmount(held.getAmount() + 1);
            inv.setItemInMainHand(held);
        } else {
            if (!empty) inv.setItem(free, held.clone());
            inv.setItemInMainHand(heart);
        }
        plugin.messages().send(p, "withdraw-success", "%hearts%", Integer.toString((int)(updated / 2.0)));
        return true;
    }
}
