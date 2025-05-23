package com.stoppedwumm.rat;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.stoppedwumm.rat.ServerShit.Handler;
import com.stoppedwumm.rat.commands.ServerCommand;

public class CommandRegistry {
    private final Map<String, ServerCommand> commands = new ConcurrentHashMap<>();

    public void registerCommand(String commandName, ServerCommand command) {
        commands.put(commandName.toUpperCase(), command);
    }

    public ServerCommand getCommand(String commandName) {
        return commands.get(commandName.toUpperCase());
    }

    public Set<String> getRegisteredCommandNames() {
        return commands.keySet();
    }

    public void processCommand(Handler clientHandler, App serverInstance, String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return;
        }

        String[] parts = rawInput.trim().split("\\s+", 2);
        String commandName = parts[0].toUpperCase();
        String argumentsString = (parts.length > 1 && parts[1] != null) ? parts[1] : "";

        ServerCommand command = commands.get(commandName);

        if (command != null) {
            try {
                String[] argsArray;
                if ("RELAY".equalsIgnoreCase(commandName)) {
                    // For RELAY, the argumentsString is "<targetId> <message>"
                    // We need to split this into targetId and the rest as the message
                    String[] relayParts = argumentsString.split("\\s+", 2);
                    if (relayParts.length > 0) {
                        String targetId = relayParts[0];
                        String message = (relayParts.length > 1) ? relayParts[1] : "";
                        argsArray = new String[]{targetId, message};
                    } else {
                        clientHandler.sendMessage("SERVER_ERROR: RELAY command needs a target ID and message.");
                        return;
                    }
                } else {
                    // For other commands, split argumentsString by space, or pass empty if none
                    argsArray = argumentsString.isEmpty() ? new String[0] : argumentsString.split("\\s+");
                }
                command.execute(clientHandler, serverInstance, argsArray);
            } catch (Exception e) {
                clientHandler.sendMessage("SERVER_ERROR: Error executing command '" + commandName + "': " + e.getMessage());
                System.err.println("Error executing command " + commandName + " for " + clientHandler.getClientId() + ": " + e);
                e.printStackTrace();
            }
        } else {
            clientHandler.sendMessage("SERVER_ERROR: Unknown command '" + commandName + "'. Type 'HELP' for available commands.");
            System.out.println("[" + Thread.currentThread().getName() + "] Unknown command from [" + clientHandler.getClientId() + "]: " + rawInput);
        }
    }
}