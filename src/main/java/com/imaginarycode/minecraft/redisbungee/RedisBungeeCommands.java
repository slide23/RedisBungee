/**
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */
package com.imaginarycode.minecraft.redisbungee;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * This class contains subclasses that are used for the commands RedisBungee overrides or includes: /glist, /find and /lastseen.
 * <p>
 * All classes use the {@link RedisBungeeAPI}.
 *
 * @author tuxed
 * @since 0.2.3
 */
class RedisBungeeCommands {
    private static final BaseComponent[] NO_PLAYER_SPECIFIED =
            new ComponentBuilder("You must specify a player name.").color(ChatColor.RED).create();
    private static final BaseComponent[] PLAYER_NOT_FOUND =
            new ComponentBuilder("No such player found.").color(ChatColor.RED).create();
    private static final BaseComponent[] NO_COMMAND_SPECIFIED =
            new ComponentBuilder("You must specify a command to be run.").color(ChatColor.RED).create();

    private static String playerPlural(int num) {
        return num == 1 ? num + " player is" : num + " players are";
    }

    public static class GlistCommand extends Command {
        private final RedisBungee plugin;

        GlistCommand(RedisBungee plugin) {
            super("glist", "bungeecord.command.list", "redisbungee", "rglist");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    int count = RedisBungee.getApi().getPlayerCount();
                    BaseComponent[] playersOnline = new ComponentBuilder("").color(ChatColor.YELLOW)
                            .append(playerPlural(count) + " currently online.").create();
                    if (args.length > 0 && args[0].equals("showall")) {
                        Multimap<String, UUID> serverToPlayers = RedisBungee.getApi().getServerToPlayers();
                        Multimap<String, String> human = HashMultimap.create();
                        for (Map.Entry<String, UUID> entry : serverToPlayers.entries()) {
                            human.put(entry.getKey(), plugin.getUuidTranslator().getNameFromUuid(entry.getValue(), false));
                        }
                        for (String server : new TreeSet<>(serverToPlayers.keySet())) {
                            TextComponent serverName = new TextComponent();
                            serverName.setColor(ChatColor.GREEN);
                            serverName.setText("[" + server + "] ");
                            TextComponent serverCount = new TextComponent();
                            serverCount.setColor(ChatColor.YELLOW);
                            serverCount.setText("(" + serverToPlayers.get(server).size() + "): ");
                            TextComponent serverPlayers = new TextComponent();
                            serverPlayers.setColor(ChatColor.WHITE);
                            serverPlayers.setText(Joiner.on(", ").join(human.get(server)));
                            sender.sendMessage(serverName, serverCount, serverPlayers);
                        }
                        sender.sendMessage(playersOnline);
                    } else {
                        sender.sendMessage(playersOnline);
                        sender.sendMessage(new ComponentBuilder("To see all players online, use /glist showall.").color(ChatColor.YELLOW).create());
                    }
                }
            });
        }
    }

    public static class FindCommand extends Command {
        private final RedisBungee plugin;

        FindCommand(RedisBungee plugin) {
            super("find", "bungeecord.command.find", "rfind");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    if (args.length > 0) {
                        UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                        if (uuid == null) {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                            return;
                        }
                        ServerInfo si = RedisBungee.getApi().getServerFor(uuid);
                        if (si != null) {
                            TextComponent message = new TextComponent();
                            message.setColor(ChatColor.BLUE);
                            message.setText(args[0] + " is on " + si.getName() + ".");
                            sender.sendMessage(message);
                        } else {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                        }
                    } else {
                        sender.sendMessage(NO_PLAYER_SPECIFIED);
                    }
                }
            });
        }
    }

    public static class LastSeenCommand extends Command {
        private final RedisBungee plugin;

        LastSeenCommand(RedisBungee plugin) {
            super("lastseen", "redisbungee.command.lastseen", "rlastseen");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    if (args.length > 0) {
                        UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                        if (uuid == null) {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                            return;
                        }
                        long secs = RedisBungee.getApi().getLastOnline(uuid);
                        TextComponent message = new TextComponent();
                        if (secs == 0) {
                            message.setColor(ChatColor.GREEN);
                            message.setText(args[0] + " is currently online.");
                        } else if (secs != -1) {
                            message.setColor(ChatColor.BLUE);
                            message.setText(args[0] + " was last online on " + new SimpleDateFormat().format(secs) + ".");
                        } else {
                            message.setColor(ChatColor.RED);
                            message.setText(args[0] + " has never been online.");
                        }
                        sender.sendMessage(message);
                    } else {
                        sender.sendMessage(NO_PLAYER_SPECIFIED);
                    }
                }
            });
        }
    }

    public static class IpCommand extends Command {
        private final RedisBungee plugin;

        IpCommand(RedisBungee plugin) {
            super("ip", "redisbungee.command.ip", "playerip", "rip", "rplayerip");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    if (args.length > 0) {
                        UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                        if (uuid == null) {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                            return;
                        }
                        InetAddress ia = RedisBungee.getApi().getPlayerIp(uuid);
                        if (ia != null) {
                            TextComponent message = new TextComponent();
                            message.setColor(ChatColor.GREEN);
                            message.setText(args[0] + " is connected from " + ia.toString() + ".");
                            sender.sendMessage(message);
                        } else {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                        }
                    } else {
                        sender.sendMessage(NO_PLAYER_SPECIFIED);
                    }
                }
            });
        }
    }

    public static class PlayerProxyCommand extends Command {
        private final RedisBungee plugin;

        PlayerProxyCommand(RedisBungee plugin) {
            super("pproxy", "redisbungee.command.pproxy");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    if (args.length > 0) {
                        UUID uuid = plugin.getUuidTranslator().getTranslatedUuid(args[0], true);
                        if (uuid == null) {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                            return;
                        }
                        String proxy = RedisBungee.getApi().getProxy(uuid);
                        if (proxy != null) {
                            TextComponent message = new TextComponent();
                            message.setColor(ChatColor.GREEN);
                            message.setText(args[0] + " is connected to " + proxy + ".");
                            sender.sendMessage(message);
                        } else {
                            sender.sendMessage(PLAYER_NOT_FOUND);
                        }
                    } else {
                        sender.sendMessage(NO_PLAYER_SPECIFIED);
                    }
                }
            });
        }
    }

    public static class SendToAll extends Command {
        private final RedisBungee plugin;

        SendToAll(RedisBungee plugin) {
            super("sendtoall", "redisbungee.command.sendtoall", "rsendtoall");
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length > 0) {
                String command = Joiner.on(" ").skipNulls().join(args);
                RedisBungee.getApi().sendProxyCommand(command);
                TextComponent message = new TextComponent();
                message.setColor(ChatColor.GREEN);
                message.setText("Sent the command /" + command + " to all proxies.");
                sender.sendMessage(message);
            } else {
                sender.sendMessage(NO_COMMAND_SPECIFIED);
            }
        }
    }

    public static class ServerId extends Command {
        private final RedisBungee plugin;

        ServerId(RedisBungee plugin) {
            super("serverid", "redisbungee.command.serverid", "rserverid");
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            TextComponent textComponent = new TextComponent();
            textComponent.setText("You are on " + RedisBungee.getApi().getServerId() + ".");
            textComponent.setColor(ChatColor.YELLOW);
            sender.sendMessage(textComponent);
        }
    }

    public static class ServerIds extends Command {
        public ServerIds() {
            super("serverids", "redisbungee.command.serverids");
        }

        @Override
        public void execute(CommandSender sender, String[] strings) {
            TextComponent textComponent = new TextComponent();
            textComponent.setText("All server IDs: " + Joiner.on(", ").join(RedisBungee.getApi().getAllServers()));
            textComponent.setColor(ChatColor.YELLOW);
            sender.sendMessage(textComponent);
        }
    }

    public static class PlistCommand extends Command {
        private final RedisBungee plugin;

        PlistCommand(RedisBungee plugin) {
            super("plist", "redisbungee.command.plist", "redisbungee", "rplist");
            this.plugin = plugin;
        }

        @Override
        public void execute(final CommandSender sender, final String[] args) {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    String proxy = args.length >= 1 ? args[0] : RedisBungee.getConfiguration().getServerId();
                    if (!plugin.getServerIds().contains(proxy)) {
                        sender.sendMessage(new ComponentBuilder(proxy + " is not a valid proxy. See /serverids for valid proxies.").color(ChatColor.RED).create());
                        return;
                    }
                    Set<UUID> players = RedisBungee.getApi().getPlayersOnProxy(proxy);
                    BaseComponent[] playersOnline = new ComponentBuilder("").color(ChatColor.YELLOW)
                            .append(playerPlural(players.size()) + " currently on proxy " + proxy + ".").create();
                    if (args.length >= 2 && args[1].equals("showall")) {
                        Multimap<String, UUID> serverToPlayers = RedisBungee.getApi().getServerToPlayers();
                        Multimap<String, String> human = HashMultimap.create();
                        for (Map.Entry<String, UUID> entry : serverToPlayers.entries()) {
                            if (players.contains(entry.getValue())) {
                                human.put(entry.getKey(), plugin.getUuidTranslator().getNameFromUuid(entry.getValue(), false));
                            }
                        }
                        for (String server : new TreeSet<>(human.keySet())) {
                            TextComponent serverName = new TextComponent();
                            serverName.setColor(ChatColor.RED);
                            serverName.setText("[" + server + "] ");
                            TextComponent serverCount = new TextComponent();
                            serverCount.setColor(ChatColor.YELLOW);
                            serverCount.setText("(" + serverToPlayers.get(server).size() + "): ");
                            TextComponent serverPlayers = new TextComponent();
                            serverPlayers.setColor(ChatColor.WHITE);
                            serverPlayers.setText(Joiner.on(", ").join(human.get(server)));
                            sender.sendMessage(serverName, serverCount, serverPlayers);
                        }
                        sender.sendMessage(playersOnline);
                    } else {
                        sender.sendMessage(playersOnline);
                        sender.sendMessage(new ComponentBuilder("To see all players online, use /plist " + proxy + " showall.").color(ChatColor.YELLOW).create());
                    }
                }
            });
        }
    }
}
