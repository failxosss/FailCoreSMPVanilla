package cz.failxos.failcoresmpvanilla;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final FailCoreSMPVanilla plugin;

    public Messages(FailCoreSMPVanilla plugin) {
        this.plugin = plugin;
    }

    public String raw(String key) {
        return plugin.getConfig().getString("messages." + key, "<red>Chybi zprava: " + key);
    }

    private static String apply(String text, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace(pairs[i], pairs[i + 1]);
        }
        return text;
    }

    public Component mini(String text) {
        return MM.deserialize(text);
    }

    public Component component(String key, String... pairs) {
        return MM.deserialize(apply(raw(key), pairs));
    }

    public Component prefixed(String key, String... pairs) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        return MM.deserialize(prefix + apply(raw(key), pairs));
    }

    public void send(CommandSender to, String key, String... pairs) {
        if (raw(key).isBlank()) {
            return;
        }
        to.sendMessage(prefixed(key, pairs));
    }

    public void actionBar(Player to, String key, String... pairs) {
        if (raw(key).isBlank()) {
            return;
        }
        to.sendActionBar(component(key, pairs));
    }
}
