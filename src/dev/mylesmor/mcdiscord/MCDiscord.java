package dev.mylesmor.mcdiscord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * CraftBukkit/Spigot plugin which links in-game chat and provides various commands.
 *
 * @author MylesMor
 * @version 1.21
 */
public class MCDiscord extends JavaPlugin implements Listener {

    public static String prefix;
    public static String botName;
    public static String chatPrefix = null;
    public static TextChannel channel = null;

    private JDA jda;
    private static String chatChannel;
    private String token;


    @Override
    public void onEnable() {

        setupConfig();

        // Registers events
        getServer().getPluginManager().registerEvents(this, this);

        // Initialises the connection to the Bot.
        if (token.equalsIgnoreCase("")) {
            Bukkit.getLogger().warning("Failed to initialise JDA. Please ensure your bot token is in the configuration file: MCDiscord/config.yml");
            jda = null;
            this.setEnabled(false);
        } else {
            try {
                jda = JDABuilder.createDefault(token).build();
                jda.addEventListener(new MessageListener());
                jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
                System.out.println("Finished Building JDA!");
                List<TextChannel> channels = jda.getTextChannels();
                for (TextChannel ch : channels) {
                    if (ch.getName().equalsIgnoreCase(chatChannel)) {
                        channel = ch;
                    }
                }
                if (channel == null) {
                    throw new Exception();
                }
                botName = jda.getSelfUser().getName();

            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to initialise JDA. Please ensure the channel name specified in MCDiscord/config.yml is valid!");
                jda = null;
                this.setEnabled(false);
            }
        }

        setStatus(false);
    }

    @Override
    public void onDisable() {
        try {
            // Shuts down the connection to the bot.
            if (jda != null) jda.shutdown();
        } catch (Exception  e) {
            Bukkit.getLogger().warning("Failed to shutdown JDA gracefully!");
        }
    }

    @EventHandler
    public void sendToDiscord(AsyncPlayerChatEvent e) {
        sendChatToDiscord(e.getPlayer().getName(), e.getMessage());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        sendLeaveToDiscord(e.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        sendJoinToDiscord(e.getPlayer().getName());
    }

    /**
     * Sets up the config.
     */
    private void setupConfig() {
        this.saveDefaultConfig();

        if (!this.getConfig().contains("command-prefix")) {
            this.getConfig().set("command-prefix", "!");
        }

        prefix = this.getConfig().getString("command-prefix");

        if (!this.getConfig().contains("token")) {
            this.getConfig().set("token", "");
        }

        token = this.getConfig().getString("token");

        if (!this.getConfig().contains("chat-channel")) {
            this.getConfig().set("chat-channel", "general");
        }

        chatChannel = this.getConfig().get("chat-channel").toString();

        if (!this.getConfig().contains("chat-prefix")) {
            this.getConfig().set("chat-prefix", "&9[DISCORD]");
        }

        String chat = this.getConfig().getString("chat-prefix");
        if (chat != null) {
            chatPrefix = ChatColor.translateAlternateColorCodes('&', chat);
        } else {
            chatPrefix = ChatColor.BLUE + "[DISCORD]";
        }

        this.saveConfig();
    }


    /**
     * Broadcasts Discord chat to the server. Called in {@link MessageListener}.
     *
     * @param msg  Message to send.
     * @param name Username who sent it.
     */
    public static void sendToServer(String msg, String name) {
        String messageToBroadcast = String.format(chatPrefix, name, msg);
        Bukkit.getServer().broadcastMessage(messageToBroadcast);
    }


    /**
     * Displays the number of online players when !list is sent. Called by {@link MessageListener}.
     *
     * @param c
     */
    public static void displayOnlinePlayers(MessageChannel c) {
        int count = Bukkit.getOnlinePlayers().size();
        Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
        Iterator<? extends Player> iterator = players.iterator();

        StringBuilder sb = new StringBuilder();

        if (count == 0) {
            sb.append("**Online players (0): **");
        } else {

            sb.append("**Online players (").append(count).append("): **");

            for (int i = 0; i < count; i++) {
                Player p = iterator.next();
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(p.getName());
                if (i == count - 1) {
                    sb.append(".");
                }
            }
        }

        c.sendMessage(sb.toString()).queue();
    }

    /**
     * Sends message to Discord.
     *
     * @param name Username of who sent it.
     * @param msg  Message to send.
     */
    private void sendChatToDiscord(String name, String msg) {

        if (jda != null) {
            channel.sendMessage("**" + name + ": **" + msg).queue();
        }
    }


    /**
     * Sets the status of the bot to the number of players online.
     */
    private void setStatus(boolean quit) {
        if (jda != null) {
            int count = Bukkit.getOnlinePlayers().size();
            if (quit) count -=1;
            String status = count + " player" + (count == 1 ? "" : "s") + " online.";
            jda.getPresence().setActivity(Activity.watching(status));
        }
    }


    /**
     * Updates status of bot and sends leave message.
     *
     * @param name Username of the player who left.
     */
    public void sendLeaveToDiscord(String name) {

        if (jda != null) {
            setStatus(true);
            channel.sendMessage("**" + name + "** has left the server!").queue();
        }
    }


    /**
     * Updates status of bot and sends join message.
     *
     * @param name Username of player who joined.
     */
    public void sendJoinToDiscord(String name) {

        if (jda != null) {
            setStatus(false);
            channel.sendMessage("**" + name + "** has joined the server!").queue();
        }
    }
}



