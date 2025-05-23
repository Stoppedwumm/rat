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

        String[] modifiedArgs = Arrays.copyOfRange(args, 0, args.length);

        modifiedArgs[0] = modifiedArgs[0].toUpperCase();

        String finalString = String.join(" ", Arrays.copyOfRange(modifiedArgs, 0, modifiedArgs.length));
        System.out.println(finalString);
        // Re-join the rest of the arguments as the message, in case the message has spaces
        String messageToRelay = finalString;
        
        // get all clients
        serverInstance.broadcastMessage("FROM " + clientHandler.getClientId() + ": " + messageToRelay, clientHandler.getClientId());
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