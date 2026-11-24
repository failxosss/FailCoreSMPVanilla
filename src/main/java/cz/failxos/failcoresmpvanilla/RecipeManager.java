package cz.failxos.failcoresmpvanilla;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RecipeManager implements Listener {

    private final FailCoreSMPVanilla plugin;
    private final List<NamespacedKey> keys = new ArrayList<>();

    public RecipeManager(FailCoreSMPVanilla plugin) {
        this.plugin = plugin;
    }

    public void register() {
        for (NamespacedKey key : keys) {
            Bukkit.removeRecipe(key);
        }
        keys.clear();

        FileConfiguration c = plugin.getConfig();
        registerBuiltIn(c);
        registerCustom(c);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.discoverRecipes(keys);
        }
    }

    // ---------- vestavene recepty ----------

    private void registerBuiltIn(FileConfiguration c) {
        // Golden apple: jablko + 4 gold ingoty
        if (c.getBoolean("recipes.golden-apple", true)) {
            Bukkit.removeRecipe(NamespacedKey.minecraft("golden_apple"));
            ShapedRecipe r = new ShapedRecipe(key("golden_apple"), new ItemStack(Material.GOLDEN_APPLE));
            r.shape(" G ", "GAG", " G ");
            r.setIngredient('G', Material.GOLD_INGOT);
            r.setIngredient('A', Material.APPLE);
            addRecipe(r, r.getKey());
        }

        // Anvil: 7 iron ingotu
        if (c.getBoolean("recipes.anvil", true)) {
            Bukkit.removeRecipe(NamespacedKey.minecraft("anvil"));
            ShapedRecipe r = new ShapedRecipe(key("anvil"), new ItemStack(Material.ANVIL));
            r.shape("III", " I ", "III");
            r.setIngredient('I', Material.IRON_INGOT);
            addRecipe(r, r.getKey());
        }

        // 9 string -> 4 cobweb
        if (c.getBoolean("recipes.cobweb", true)) {
            ShapedRecipe r = new ShapedRecipe(key("cobweb"), new ItemStack(Material.COBWEB, 4));
            r.shape("SSS", "SSS", "SSS");
            r.setIngredient('S', Material.STRING);
            addRecipe(r, r.getKey());
        }

        // Netherite upgrade template: stejne jako vanilla, ale misto sablony jde dat netherite ingot
        if (c.getBoolean("recipes.netherite-template-with-ingot", true)) {
            ShapedRecipe r = new ShapedRecipe(key("netherite_upgrade_ingot"),
                    new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2));
            r.shape("DID", "DND", "DDD");
            r.setIngredient('D', Material.DIAMOND);
            r.setIngredient('I', Material.NETHERITE_INGOT);
            r.setIngredient('N', Material.NETHERRACK);
            addRecipe(r, r.getKey());
        }
        if (c.getBoolean("recipes.ender-chest", true)) {
            Bukkit.removeRecipe(NamespacedKey.minecraft("ender_chest"));
            ShapedRecipe r = new ShapedRecipe(key("ender_chest"), new ItemStack(Material.ENDER_CHEST));
            r.shape(" O ", "OEO", " O ");
            r.setIngredient('O', Material.OBSIDIAN);
            r.setIngredient('E', Material.ENDER_EYE);
            addRecipe(r, r.getKey());
        }
        if (c.getBoolean("recipes.shulker-box", true)) {
            Bukkit.removeRecipe(NamespacedKey.minecraft("shulker_box"));
            ShapedRecipe r = new ShapedRecipe(key("shulker_box"), new ItemStack(Material.SHULKER_BOX));
            r.shape(" D ", "DCD", " D ");
            r.setIngredient('D', Material.DIAMOND);
            r.setIngredient('C', Material.CHEST);
            addRecipe(r, r.getKey());
        }
        if (c.getBoolean("recipes.notch-apple", true)) {
            ShapedRecipe r = new ShapedRecipe(key("notch_apple"), new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
            r.shape(" G ", "GNG", " G ");
            r.setIngredient('G', Material.GOLD_BLOCK);
            r.setIngredient('N', Material.NETHERITE_INGOT);
            addRecipe(r, r.getKey());
        }
    }

    // ---------- vlastni recepty z configu ----------

    private void registerCustom(FileConfiguration c) {
        for (String vanilla : c.getStringList("remove-vanilla-recipes")) {
            NamespacedKey key = vanilla.contains(":")
                    ? NamespacedKey.fromString(vanilla.toLowerCase(Locale.ROOT))
                    : NamespacedKey.minecraft(vanilla.toLowerCase(Locale.ROOT));
            if (key == null || !Bukkit.removeRecipe(key)) {
                plugin.getLogger().warning("Vanilla recept nenalezen (remove-vanilla-recipes): " + vanilla);
            }
        }

        ConfigurationSection root = c.getConfigurationSection("custom-recipes");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }
            try {
                NamespacedKey key = key("custom_" + sanitize(id));
                Recipe recipe = build(key, section);
                addRecipe(recipe, key);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Vlastni recept '" + id + "' preskocen: " + e.getMessage());
            }
        }
    }

    private Recipe build(NamespacedKey key, ConfigurationSection s) {
        Material resultMaterial = material(s.getString("result"), "result");
        int amount = Math.max(1, Math.min(64, s.getInt("amount", 1)));
        ItemStack result = new ItemStack(resultMaterial, amount);

        String name = s.getString("name");
        if (name != null && !name.isBlank()) {
            ItemMeta meta = result.getItemMeta();
            meta.displayName(MiniMessage.miniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false));
            result.setItemMeta(meta);
        }

        String type = s.getString("type", "shaped").toLowerCase(Locale.ROOT);
        if (type.equals("shapeless")) {
            List<String> list = s.getStringList("ingredients");
            if (list.isEmpty() || list.size() > 9) {
                throw new IllegalArgumentException("shapeless recept potrebuje 1-9 ingredienci");
            }
            ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            for (String material : list) {
                recipe.addIngredient(material(material, "ingredients"));
            }
            return recipe;
        }
        if (!type.equals("shaped")) {
            throw new IllegalArgumentException("neznamy type '" + type + "' (shaped / shapeless)");
        }

        List<String> shape = s.getStringList("shape");
        if (shape.isEmpty() || shape.size() > 3) {
            throw new IllegalArgumentException("shape musi mit 1-3 radky");
        }
        int width = 0;
        for (String row : shape) {
            if (row.isEmpty() || row.length() > 3) {
                throw new IllegalArgumentException("kazdy radek shape musi mit 1-3 znaky");
            }
            width = Math.max(width, row.length());
        }
        String[] rows = new String[shape.size()];
        for (int i = 0; i < rows.length; i++) {
            StringBuilder padded = new StringBuilder(shape.get(i));
            while (padded.length() < width) {
                padded.append(' ');
            }
            rows[i] = padded.toString();
        }

        ConfigurationSection ingredients = s.getConfigurationSection("ingredients");
        if (ingredients == null) {
            throw new IllegalArgumentException("chybi ingredients (znak: MATERIAL)");
        }
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(rows);
        for (String row : rows) {
            for (char ch : row.toCharArray()) {
                if (ch == ' ') {
                    continue;
                }
                String material = ingredients.getString(String.valueOf(ch));
                if (material == null) {
                    throw new IllegalArgumentException("pro znak '" + ch + "' chybi ingredience");
                }
                recipe.setIngredient(ch, material(material, "ingredients." + ch));
            }
        }
        return recipe;
    }

    private static Material material(String name, String where) {
        Material material = name == null ? null : Material.matchMaterial(name.trim());
        if (material == null || !material.isItem() || material.isAir()) {
            throw new IllegalArgumentException("neznamy material '" + name + "' v " + where);
        }
        return material;
    }

    private static String sanitize(String id) {
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    // ---------- spolecne ----------

    private NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }

    private void addRecipe(Recipe recipe, NamespacedKey key) {
        try {
            Bukkit.addRecipe(recipe);
            keys.add(key);
        } catch (IllegalStateException | IllegalArgumentException e) {
            plugin.getLogger().warning("Recept " + key.getKey() + " se nepodarilo pridat: " + e.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.getPlayer().discoverRecipes(keys);
    }
}
