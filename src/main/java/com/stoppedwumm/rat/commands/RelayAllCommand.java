package com.stoppedwumm.rat.commands;

import java.util.Arrays;

import com.stoppedwumm.rat.App;
import com.stoppedwumm.rat.ServerShit.Handler;

public class RelayAllCommand implements ServerCommand {
    @Override
    public void execute(Handler clientHandler, App serverInstance, String[] args) {
        if (args.length < 1) {
            clientHandler.sendMessage("SERVER_ERROR: Invalid RELAY command format. Use: " + getUsage());
            return;
        }
        // Re-join the rest of the arguments as the message, in case the message has spaces
        String messageToRelay = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        serverInstance.broadcastMessage(messageToRelay, clientHandler.getClientId());
    }

    @Override
    public String getDescription() {
        return "Relays a message to all connected clients.";
    }

    @Override
    public String getUsage() {
        return "RELAY_ALL <message>";
    }
}