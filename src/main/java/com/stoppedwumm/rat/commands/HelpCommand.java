package com.stoppedwumm.rat.commands;

import com.stoppedwumm.rat.App;
import com.stoppedwumm.rat.CommandRegistry;
import com.stoppedwumm.rat.ServerShit.Handler;

public class HelpCommand implements ServerCommand {
    @Override
    public void execute(Handler clientHandler, App serverInstance, String[] args) {
        CommandRegistry registry = serverInstance.getCommandRegistry(); // This now works
        StringBuilder helpMessage = new StringBuilder("SERVER_HELP: Available server commands:\n");
        for (String commandName : registry.getRegisteredCommandNames()) {
            ServerCommand command = registry.getCommand(commandName);
            if (command != null) {
                helpMessage.append(String.format("  %-18s : %s (Usage: %s)\n",
                        commandName, command.getDescription(), command.getUsage()));
            }
        }
        clientHandler.sendMessage(helpMessage.toString().trim());
    }

    @Override
    public String getDescription() {
        return "Displays this help message with available commands.";
    }

    @Override
    public String getUsage() {
        return "HELP";
    }
}