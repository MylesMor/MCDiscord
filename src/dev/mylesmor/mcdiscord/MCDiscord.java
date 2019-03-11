package dev.mylesmor.mcdiscord;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import javax.security.auth.login.LoginException;
import java.util.List;

public class MCDiscord extends JavaPlugin implements Listener {

    JDA jda;
    List<TextChannel> channels;
    public static String prefix;
    public static String chatChannel;

    @Override
    public void onEnable() {

        setupConfig();

        // Registers events
        getServer().getPluginManager().registerEvents(this, this);

        // Initialises the connection to the Bot.
        try {
            jda = new JDABuilder(AccountType.BOT).setToken("BOT_TOKEN")
                    .addEventListener(new MessageListener())  // An instance of a class that will handle events.
                    .build();
            jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
            System.out.println("Finished Building JDA!");
            channels = jda.getTextChannels();

        } catch (LoginException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Sets status to number of players.
            if (Bukkit.getOnlinePlayers().size() != 1) {
                jda.getPresence().setGame(Game.of(Game.GameType.WATCHING, Bukkit.getOnlinePlayers().size() + " players online."));
            } else {
                jda.getPresence().setGame(Game.of(Game.GameType.WATCHING, Bukkit.getOnlinePlayers().size() + " player online."));
            }
        }


    @Override
    public void onDisable() {

        // Shuts down the connection to the bot.
        jda.shutdownNow();
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
     *  Sets up the config.
     */
    public void setupConfig() {
        this.saveDefaultConfig();

        if (!this.getConfig().isConfigurationSection("command-prefix")) {
            this.getConfig().set("command-prefix", "!");
        }

        prefix = this.getConfig().getString("command-prefix");

        if (!this.getConfig().isConfigurationSection("chat-channel")) {
            this.getConfig().set("chat-channel", "server-chat");
        }

        chatChannel = this.getConfig().get("chat-channel").toString();

        this.saveConfig();
    }


    /**
     * Broadcasts Discord chat to the server. Called in {@link MessageListener}.
     * @param msg Message to send.
     * @param name Username who sent it.
     */
    public static void sendToServer(String msg, String name) {

            Bukkit.getServer().broadcastMessage(ChatColor.RED + "[DISCORD] " + ChatColor.WHITE + name +
                    ChatColor.WHITE + ": " + msg);


    }

    /**
     * Sends message to Discord.
     * @param name Username of who sent it.
     * @param msg Message to send.
     */
    private void sendChatToDiscord(String name, String msg) {

           for (TextChannel tc : channels) {
               if (tc.getName().equalsIgnoreCase("server-chat")) {
                   tc.sendMessage("**" + name + ": **" + msg).queue();
               }
           }

    }


    /**
     * Updates status of bot and sends leave message.
     * @param name Username of the player who left.
     */
    public void sendLeaveToDiscord(String name) {

            if (Bukkit.getOnlinePlayers().size() - 1 != 1) {
                jda.getPresence().setGame(Game.of(Game.GameType.WATCHING, Bukkit.getOnlinePlayers().size() - 1 + " players online."));
            } else {
                jda.getPresence().setGame(Game.of(Game.GameType.WATCHING, Bukkit.getOnlinePlayers().size() - 1 + " player online."));
            }
            for (TextChannel tc : channels) {
                if (tc.getName().equalsIgnoreCase("server-chat")) {
                    tc.sendMessage("**" + name + "** has left the server!").queue();
                }

        }

    }


    /**
     * Updates status of bot and sends join message.
     * @param name Username of player who joined.
     */
    public void sendJoinToDiscord(String name) {

            if (Bukkit.getOnlinePlayers().size() != 1) {
                jda.getPresence().setGame(Game.of(Game.GameType.WATCHING, Bukkit.getOnlinePlayers().size() + " players online."));
            } else {
                jda.getPresence().setGame(Game.of(Game.GameType.WATCHING, Bukkit.getOnlinePlayers().size() + " player online."));
            }
            for (TextChannel tc : channels) {
                if (tc.getName().equalsIgnoreCase("server-chat")) {
                    tc.sendMessage("**" + name + "** has joined the server!").queue();
                }

        }
    }


    /**
     * Displays the number of deaths a player has in Discord. Called by {@link MessageListener}.
     * @param c The message channel that the command was sent in.
     * @param playerName The name of the player to check deaths for.
     */
    public static void getDeaths(MessageChannel c, String playerName) {
        Scoreboard board = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        Objective objective = board.getObjective("Deaths");
        OfflinePlayer player;
        Score score;
        try {
            player = Bukkit.getOfflinePlayer(playerName);
            score = objective.getScore(player.getName());
        } catch (Exception e) {
            c.sendMessage("Player **" + playerName + "** hasn't played yet!").queue();
            return;
        }
        c.sendMessage("Player **" + player.getName() + "** has " + score.getScore() + " deaths!").queue();

    }

    /**
     * Displays the number of online players when !list is sent. Called by {@link MessageListener}.
     * @param c
     */
    public static void displayOnlinePlayers(MessageChannel c) {
        String online = "**Online players (" + Bukkit.getOnlinePlayers().size() + "): **" ;

        for (Player p : Bukkit.getOnlinePlayers()) {
            online += p.getName() + ", ";
        }

        online = online.substring(0,online.length()-2);
        online += ".";

        if (Bukkit.getOnlinePlayers().size() == 0) {
            online = "**Online players (0): **";
        }

        c.sendMessage(online).queue();
    }

}



