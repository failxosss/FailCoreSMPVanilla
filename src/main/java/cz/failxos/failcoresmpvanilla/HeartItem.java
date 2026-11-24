package cz.failxos.failcoresmpvanilla;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Item "Srdce" - vypada jako srdce, ale je to jen znaceny beetroot (nejde ho snist). */
public final class HeartItem {

    private HeartItem() {
    }

    private static NamespacedKey key(FailCoreSMPVanilla plugin) {
        return new NamespacedKey(plugin, "heart");
    }

    public static ItemStack create(FailCoreSMPVanilla plugin, int amount) {
        FileConfiguration c = plugin.getConfig();
        Material material = Material.matchMaterial(c.getString("heart-item.material", "BEETROOT"));
        if (material == null || !material.isItem() || material.isAir()) {
            material = Material.BEETROOT;
        }
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage()
                .deserialize(c.getString("heart-item.name", "<red>Srdce"))
                .decoration(TextDecoration.ITALIC, false));
        String model = c.getString("heart-item.item-model", "");
        if (model != null && !model.isBlank()) {
            NamespacedKey modelKey = NamespacedKey.fromString(model.trim().toLowerCase());
            if (modelKey != null) {
                meta.setItemModel(modelKey);
            }
        }
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isHeart(FailCoreSMPVanilla plugin, ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(key(plugin), PersistentDataType.BYTE);
    }
}
