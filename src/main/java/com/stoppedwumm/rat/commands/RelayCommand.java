package com.stoppedwumm.rat.commands;

import java.util.Arrays;

import com.stoppedwumm.rat.App;
import com.stoppedwumm.rat.ServerShit.Handler;

public class RelayCommand implements ServerCommand {
    @Override
    public void execute(Handler clientHandler, App serverInstance, String[] args) {
        if (args.length < 2) {
            clientHandler.sendMessage("SERVER_ERROR: Invalid RELAY command format. Use: " + getUsage());
            return;
        }
        String targetId = args[0];
        // Re-join the rest of the arguments as the message, in case the message has spaces
        String messageToRelay = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        serverInstance.relayMessage(clientHandler.getClientId(), targetId, messageToRelay);
    }

    @Override
    public String getDescription() {
        return "Relays a message to another connected client.";
    }

    @Override
    public String getUsage() {
        return "RELAY <targetClientId> <message>";
    }
}