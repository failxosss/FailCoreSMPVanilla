package cz.failxos.failcoresmpvanilla;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FailCoreSMPVanillaCommand implements CommandExecutor, TabCompleter {

    private final FailCoreSMPVanilla plugin;

    public FailCoreSMPVanillaCommand(FailCoreSMPVanilla plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rules")) {
            for (String line : plugin.getConfig().getStringList("rules")) {
                sender.sendMessage(plugin.messages().mini(line));
            }
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(plugin.messages().mini("<gold>FailCoreSMPVanilla <white>v" + plugin.getPluginMeta().getVersion()
                    + " <gray>by <white>Failxos <gray>(MC 1.21.11)"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("failcoresmpvanilla.admin")) {
                plugin.messages().send(sender, "no-permission");
                return true;
            }
            plugin.reloadAll();
            plugin.messages().send(sender, "reloaded");
            return true;
        }

        plugin.messages().send(sender, "usage");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("failcoresmpvanilla") && args.length == 1) {
            String start = args[0].toLowerCase(Locale.ROOT);
            for (String s : List.of("info", "reload")) {
                if (s.startsWith(start)) {
                    out.add(s);
                }
            }
        }
        return out;
    }
}
