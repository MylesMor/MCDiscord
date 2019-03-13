package dev.mylesmor.mcdiscord;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import org.bukkit.*;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.Hash;
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
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * CraftBukkit/Spigot plugin which links in-game chat and provides various commands.
 *
 * @author MylesMor
 * @version 1.21
 */
public class MCDiscord extends JavaPlugin implements Listener {

    public static String prefix;
    public static String chatChannel;
    private JDA jda;
    private List<TextChannel> channels;
    private static FileConfiguration stats;


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

        } catch (Exception e) {
            Bukkit.getLogger().info("Failed to initialise JDA.");
            jda = null;
        }

        setStatus();
    }

    @Override
    public void onDisable() {

        // Shuts down the connection to the bot.
        if (jda != null) jda.shutdownNow();
    }

    @EventHandler
    public void sendToDiscord(AsyncPlayerChatEvent e) {
        sendChatToDiscord(e.getPlayer().getName(), e.getMessage());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        sendLeaveToDiscord(e.getPlayer().getName());
        storeStats(e.getPlayer());
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

        if (!this.getConfig().isConfigurationSection("command-prefix")) {
            this.getConfig().set("command-prefix", "!");
        }

        prefix = this.getConfig().getString("command-prefix");

        if (!this.getConfig().isConfigurationSection("chat-channel")) {
            this.getConfig().set("chat-channel", "server-chat");
        }

        chatChannel = this.getConfig().get("chat-channel").toString();

        this.saveConfig();

        File statsFile = new File (this.getDataFolder() + File.separator + "stats.yml");
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                System.out.println("Failed to create stats.yml");
            }
        }

        stats = YamlConfiguration.loadConfiguration(statsFile);

    }


    /**
     * Stores stats for a player in stats.yml.
     * @param player The player to store stats for.
     */
    private void storeStats(Player player) {

        int deaths = getDeaths(player);
        int animalsBred = player.getStatistic(Statistic.ANIMALS_BRED);
        int playersKilled = player.getStatistic(Statistic.PLAYER_KILLS);
        int blocksMined = player.getStatistic(Statistic.MINE_BLOCK, Material.STONE);
        int fishCaught = player.getStatistic(Statistic.FISH_CAUGHT);
        int timesJumped = player.getStatistic(Statistic.JUMP);
        int tradedVillager = player.getStatistic(Statistic.TRADED_WITH_VILLAGER);
        int mobsKilled = player.getStatistic(Statistic.MOB_KILLS);
        int cakeSlicesEaten = player.getStatistic(Statistic.CAKE_SLICES_EATEN);

        if (!stats.isConfigurationSection(player.getUniqueId().toString())) {
            stats.createSection(player.getUniqueId().toString());
        }

        ConfigurationSection cs = stats.getConfigurationSection(player.getUniqueId().toString());
        cs.set("deaths", deaths);
        cs.set("animals_bred", animalsBred);
        cs.set("players_killed", playersKilled);
        cs.set("stone_blocks_mined", blocksMined);
        cs.set("fish_caught", fishCaught);
        cs.set("times_jumped", timesJumped);
        cs.set("villager_trades", tradedVillager);
        cs.set("mobs_killed", mobsKilled);
        cs.set("cake_slices_eaten", cakeSlicesEaten);

        try {
            stats.save(new File(getDataFolder() + File.separator + "stats.yml"));
        } catch (IOException e) {
            System.out.println("Failed to save " + player.getName() + "'s data to stats.yml");
        }

    }

    /**
     * Broadcasts Discord chat to the server. Called in {@link MessageListener}.
     *
     * @param msg  Message to send.
     * @param name Username who sent it.
     */
    public static void sendToServer(String msg, String name) {

        Bukkit.getServer().broadcastMessage(ChatColor.RED + "[DISCORD] " + ChatColor.WHITE + name +
                ChatColor.WHITE + ": " + msg);


    }

    /**
     * Displays the number of deaths a player has in Discord. Called by {@link MessageListener}.
     *
     * @param c          The message channel that the command was sent in.
     * @param playerName The name of the player to check deaths for.
     */
    public static void sendDeaths(MessageChannel c, String playerName) {

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        int deaths = getDeaths(player);
        if (deaths == -1) {
            c.sendMessage("Player **" + playerName + "** hasn't played yet!").queue();
            return;
        }
        c.sendMessage("Player **" + player.getName() + "** has " + deaths + " deaths!").queue();

    }

    /**
     * Returns deaths as an int, or -1 if the player hasn't played.
     * @param player The player to check deaths for.
     * @return Number of deaths or -1 if not played.
     */
    private static int getDeaths(OfflinePlayer player) {
        Scoreboard board = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        Objective objective = board.getObjective("Deaths");
        Score score = objective.getScore(player.getName());
        if (!player.hasPlayedBefore() || !score.isScoreSet()) {
            return -1;
        } else {
            return score.getScore();
        }
    }

    /**
     * Retrieves stats about a player.
     * @param c The message channel that the command was sent in.
     * @param playerName The name of the player to check stats for.
     */
    public static void getStats(MessageChannel c, String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

        if (!offlinePlayer.hasPlayedBefore()) {
            c.sendMessage("Player **" + playerName + "** hasn't played yet!").queue();
            return;
        }

        Player player = offlinePlayer.getPlayer();

        if (player == null) {
            getOfflineStats(c, offlinePlayer);
            return;
        }

        HashMap<String, Integer> statsMap = new HashMap<>();

        statsMap.put("deaths", getDeaths(offlinePlayer));
        statsMap.put("animals_bred", player.getStatistic(Statistic.ANIMALS_BRED));
        statsMap.put("players_killed", player.getStatistic(Statistic.PLAYER_KILLS));
        statsMap.put("stone_blocks_mined", player.getStatistic(Statistic.MINE_BLOCK, Material.STONE));
        statsMap.put("fish_caught", player.getStatistic(Statistic.FISH_CAUGHT));
        statsMap.put("times_jumped", player.getStatistic(Statistic.JUMP));
        statsMap.put("villager_trades", player.getStatistic(Statistic.TRADED_WITH_VILLAGER));
        statsMap.put("mobs_killed", player.getStatistic(Statistic.MOB_KILLS));
        statsMap.put("cake_slices_eaten", player.getStatistic(Statistic.CAKE_SLICES_EATEN));

        displayStats(c, offlinePlayer, statsMap);
    }

    /**
     * Displays the stats on discord.
     * @param c The channel to send the message to.
     * @param offlinePlayer The player to check stats for.
     * @param statsMap The map of stats.
     */
    private static void displayStats(MessageChannel c, OfflinePlayer offlinePlayer, HashMap<String, Integer> statsMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("Player stats: **").append(offlinePlayer.getName()).append("**\n```")
                .append("\nDeaths: ").append(statsMap.get("deaths"))
                .append("\nPlayers killed: ").append(statsMap.get("players_killed"))
                .append("\nMobs killed: ").append(statsMap.get("mobs_killed"))
                .append("\nAnimals bred: ").append(statsMap.get("animals_bred"))
                .append("\nTimes jumped: ").append(statsMap.get("times_jumped"))
                .append("\nStone blocks mined: ").append(statsMap.get("stone_blocks_mined"))
                .append("\nVillager trades: ").append(statsMap.get("villager_trades"))
                .append("\nFish caught: ").append(statsMap.get("fish_caught"))
                .append("\nCake slices eaten: ").append(statsMap.get("cake_slices_eaten"))
                .append("```");

        c.sendMessage(sb.toString()).queue();
    }


    /**
     * Retrieves offline stats about a player.
     * @param c The channel to send messages to.
     * @param player The player to check stats for.
     */
    private static void getOfflineStats(MessageChannel c, OfflinePlayer player) {
        if (!stats.isConfigurationSection(player.getUniqueId().toString())) {
            c.sendMessage("Offline stats not available for player **" + player.getName() + ".**").queue();
            return;
        }

        ConfigurationSection cs = stats.getConfigurationSection(player.getUniqueId().toString());
        HashMap<String, Integer> statsMap = new HashMap<>();
        statsMap.put("deaths", (int) cs.get("deaths"));
        statsMap.put("animals_bred", (int) cs.get("animals_bred"));
        statsMap.put("players_killed", (int) cs.get("players_killed"));
        statsMap.put("stone_blocks_mined", (int) cs.get("stone_blocks_mined"));
        statsMap.put("fish_caught", (int) cs.get("fish_caught"));
        statsMap.put("times_jumped", (int) cs.get("times_jumped"));
        statsMap.put("villager_trades", (int) cs.get("villager_trades"));
        statsMap.put("mobs_killed", (int) cs.get("mobs_killed"));
        statsMap.put("cake_slices_eaten", (int) cs.get("cake_slices_eaten"));

        displayStats(c, player, statsMap);
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
            for (TextChannel tc : channels) {
                if (tc.getName().equalsIgnoreCase(chatChannel)) {
                    tc.sendMessage("**" + name + ": **" + msg).queue();
                }
            }
        }
    }


    /**
     * Sets the status of the bot to the number of players online.
     */
    private void setStatus() {
        if (jda != null) {
            int count = Bukkit.getOnlinePlayers().size();
            String status = count + " player" + (count == 1 ? "" : "s") + " online.";
            jda.getPresence().setGame(Game.of(Game.GameType.WATCHING, status));
        }
    }


    /**
     * Updates status of bot and sends leave message.
     *
     * @param name Username of the player who left.
     */
    public void sendLeaveToDiscord(String name) {

        if (jda != null) {
            setStatus();
            for (TextChannel tc : channels) {
                if (tc.getName().equalsIgnoreCase("server-chat")) {
                    tc.sendMessage("**" + name + "** has left the server!").queue();
                }
            }
        }
    }


    /**
     * Updates status of bot and sends join message.
     *
     * @param name Username of player who joined.
     */
    public void sendJoinToDiscord(String name) {

        if (jda != null) {
            setStatus();
            for (TextChannel tc : channels) {
                if (tc.getName().equalsIgnoreCase("server-chat")) {
                    tc.sendMessage("**" + name + "** has joined the server!").queue();
                }
            }
        }
    }

}



