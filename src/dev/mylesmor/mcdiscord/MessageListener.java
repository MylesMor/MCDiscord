package dev.mylesmor.mcdiscord;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //These are provided with every event in JDA

        //JDA, the core of the api.
        JDA jda = event.getJDA();

        //Event specific information
        //The user that sent the message
        User author = event.getAuthor();

        //The message that was received.
        Message message = event.getMessage();

        //This is the MessageChannel that the message was sent to.
        MessageChannel channel = event.getChannel();

        //This returns a human readable version of the Message
        String msg = message.getContentDisplay();

        String[] input = msg.split(" ");
        String command;
        try {
            command = input[0].toLowerCase().substring(MCDiscord.prefix.length());
        } catch (StringIndexOutOfBoundsException e) {
            command = msg.toLowerCase().substring(MCDiscord.prefix.length());
        }


        // If message starts with the prefix defined in config.yml (this variable is set in setupConfig() in MCDiscord.java
        if (msg.startsWith(MCDiscord.prefix)) {

            if (command.equals("list")) {
                MCDiscord.displayOnlinePlayers(channel);
                return;
            }


            if (command.equals("stats")) {
                try {
                    MCDiscord.getStats(channel, input[1]);
                } catch (Exception e) {
                    e.printStackTrace();
                    channel.sendMessage("Incorrect usage. Example: !stats MylesMor").queue();
                }
                return;
            }


            if (command.equals("help")) {
                StringBuilder sb = new StringBuilder();
                sb.append("**MC SURVIVAL BOT COMMANDS**\n```")
                        .append("\n!list - Shows all online players.")
                        .append("\n!stats <player> - Displays some fun stats about a player.\n```");
                channel.sendMessage(sb.toString()).queue();
                return;
            }

            channel.sendMessage("Unknown command. Please type !help for available commands.").queue();
            return;
        }

        // Send to server.
        if (channel.getName().equalsIgnoreCase(MCDiscord.chatChannel) &&
                !author.getName().equalsIgnoreCase("Survival MC Bot")) {
            MCDiscord.sendToServer(msg, author.getName());
        }

    }
}
