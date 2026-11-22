package ru.spawnshield;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class SpawnCommand implements CommandExecutor, TabCompleter {

    private final SpawnShieldPlugin plugin;

    public SpawnCommand(SpawnShieldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(SpawnShieldPlugin.component(
                    plugin.text("messages.only-players", "<#FF6B6B>Только для игроков.</#FF6B6B>")));
            return true;
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set" -> {
                    return setSpawn(player);
                }
                case "reload" -> {
                    return reload(player);
                }
                case "edit" -> {
                    return edit(player, args);
                }
                default -> {
                    // /spawn <anything else> behaves like /spawn
                }
            }
        }

        plugin.startTeleport(player, false);
        return true;
    }

    private boolean setSpawn(Player player) {
        if (!player.hasPermission("spawnshield.admin")) {
            plugin.send(player, plugin.text("messages.no-permission",
                    "<#FF6B6B>Недостаточно прав.</#FF6B6B>"));
            return true;
        }
        plugin.saveSpawn(player.getLocation());
        plugin.send(player, plugin.text("messages.spawn-set",
                "<#7DE3F4>Точка спавна обновлена.</#7DE3F4>"));
        return true;
    }

    private boolean reload(Player player) {
        if (!player.hasPermission("spawnshield.admin")) {
            plugin.send(player, plugin.text("messages.no-permission",
                    "<#FF6B6B>Недостаточно прав.</#FF6B6B>"));
            return true;
        }
        plugin.reloadAll();
        plugin.send(player, plugin.text("messages.reloaded",
                "<#7DE3F4>Настройки SpawnShield перезагружены.</#7DE3F4>"));
        return true;
    }

    private boolean edit(Player player, String[] args) {
        if (!player.hasPermission("spawnshield.admin")) {
            plugin.send(player, plugin.text("messages.no-permission",
                    "<#FF6B6B>Недостаточно прав.</#FF6B6B>"));
            return true;
        }
        Boolean value = args.length < 2 ? !plugin.editMode() : parseBoolean(args[1]);
        if (value == null) {
            plugin.send(player, plugin.text("messages.edit-usage",
                    "<#FF6B6B>Использование: /spawn edit true|false</#FF6B6B>"));
            return true;
        }
        plugin.setEditMode(value);
        plugin.send(player, plugin.text(value ? "messages.edit-on" : "messages.edit-off",
                value
                        ? "<#FFD166>Режим редактирования ВКЛ — защита спавна снята.</#FFD166>"
                        : "<#7DE3F4>Режим редактирования ВЫКЛ — защита спавна включена.</#7DE3F4>"));
        return true;
    }

    private static Boolean parseBoolean(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "true", "on", "1", "yes", "вкл", "да" -> Boolean.TRUE;
            case "false", "off", "0", "no", "выкл", "нет" -> Boolean.FALSE;
            default -> null;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("spawnshield.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return List.of("set", "edit", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("edit")) {
            return List.of("true", "false");
        }
        return List.of();
    }
}
