package cz.failxos.failcoresmpvanilla;

import cz.failxos.failcoresmpvanilla.listener.BannedItemsListener;
import cz.failxos.failcoresmpvanilla.listener.CombatListener;
import cz.failxos.failcoresmpvanilla.listener.CooldownListener;
import cz.failxos.failcoresmpvanilla.listener.ElytraMaceListener;
import cz.failxos.failcoresmpvanilla.listener.HeartItemListener;
import cz.failxos.failcoresmpvanilla.listener.LifeListener;
import cz.failxos.failcoresmpvanilla.listener.MaceEnchantListener;
import cz.failxos.failcoresmpvanilla.listener.VillagerListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FailCoreSMPVanilla extends JavaPlugin {

    public static final String BYPASS = "failcoresmpvanilla.bypass";

    private Messages messages;
    private RecipeManager recipes;
    private CombatListener combat;
    private BannedItemsListener banned;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);

        recipes = new RecipeManager(this);
        recipes.register();

        combat = new CombatListener(this);
        banned = new BannedItemsListener(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(recipes, this);
        pm.registerEvents(combat, this);
        pm.registerEvents(banned, this);
        pm.registerEvents(new ElytraMaceListener(this), this);
        pm.registerEvents(new MaceEnchantListener(this), this);
        pm.registerEvents(new CooldownListener(this), this);
        pm.registerEvents(new LifeListener(this, combat), this);
        pm.registerEvents(new HeartItemListener(this), this);
        pm.registerEvents(new VillagerListener(this), this);

        FailCoreSMPVanillaCommand handler = new FailCoreSMPVanillaCommand(this);
        PluginCommand main = getCommand("failcoresmpvanilla");
        if (main != null) {
            main.setExecutor(handler);
            main.setTabCompleter(handler);
        }
        PluginCommand rules = getCommand("rules");
        if (rules != null) {
            rules.setExecutor(handler);
        }

        PluginCommand withdraw = getCommand("withdrawsrdce");
        if (withdraw != null) withdraw.setExecutor(new WithdrawSrdceCommand(this));

        getLogger().info("FailCoreSMPVanilla " + getPluginMeta().getVersion() + " by Failxos nacten.");
    }

    @Override
    public void onDisable() {
        if (combat != null) {
            combat.clear();
        }
    }

    public void reloadAll() {
        reloadConfig();
        banned.reload();
        recipes.register();
    }

    public Messages messages() {
        return messages;
    }
}
