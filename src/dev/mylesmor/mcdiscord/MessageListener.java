package dev.mylesmor.mcdiscord;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
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



        // If message starts with the prefix defined in config.yml (this variable is set in setupConfig() in MCDiscord.java
        if (msg.startsWith(MCDiscord.prefix)) {

            // If excluding the first letter, the string equals list.
            if (msg.substring(1).equalsIgnoreCase("list")) {
                MCDiscord.displayOnlinePlayers(channel);
                return;
            }

            // If excluding the first letter, the string equals deaths.
            try {
                if (msg.split(" ")[0].substring(1).equalsIgnoreCase("deaths")) {
                    MCDiscord.getDeaths(channel, msg.split(" ")[1]);
                    return;
                }
            } catch (Exception e) {
                channel.sendMessage("Incorrect usage. Example: !deaths MylesMor").queue();
                return;
            }

            channel.sendMessage("Unknown command.").queue();
            return;
        }

        // Send to server.
        if (channel.getName().equalsIgnoreCase(MCDiscord.chatChannel) &&
                !author.getName().equalsIgnoreCase("Survival MC Bot")) {
            MCDiscord.sendToServer(msg, author.getName());
        }

    }
}
