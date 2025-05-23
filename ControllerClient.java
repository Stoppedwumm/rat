import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class ControllerClient {
    private static final String SERVER_HOSTNAME = "localhost";
    private static final int SERVER_PORT = 3000;
    private static String myControllerId = null;
    private static PrintWriter out;
    private static BufferedReader in;
    private static BufferedReader consoleReader;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        System.out.println("Controller Client starting...");

        try (Socket socket = new Socket(SERVER_HOSTNAME, SERVER_PORT)) {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            consoleReader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Connected to middleware server at " + SERVER_HOSTNAME + ":" + SERVER_PORT);

            // 1. Receive the ID from the server
            String serverMessage = in.readLine();
            if (serverMessage != null && serverMessage.startsWith("ID:")) {
                myControllerId = serverMessage.substring(3);
                System.out.println("Assigned Controller ID by server: " + myControllerId);
            } else {
                System.err.println("Error: Did not receive a valid ID from the server. Received: " + serverMessage);
                return;
            }

            // Thread to listen for incoming messages from the server (responses, alerts)
            Thread serverListenerThread = new Thread(() -> {
                try {
                    String messageFromServer;
                    while (running && (messageFromServer = in.readLine()) != null) {
                        System.out.println("\n[Server/Agent Response]: " + messageFromServer.replace("#nl#", "\n"));
                        System.out.print("Controller> "); // Re-prompt
                    }
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error reading from server: " + e.getMessage());
                    }
                } finally {
                    running = false; // Stop main loop if server connection lost
                     System.out.println("\nConnection to server lost. Exiting listener.");
                }
            });
            serverListenerThread.setDaemon(true); // Allow JVM to exit if only this thread is running
            serverListenerThread.start();

            // 2. Main command loop for the controller
            String userInput;
            System.out.println("Controller [" + myControllerId + "] ready. Type 'help' for commands.");
            System.out.print("Controller> ");
            while (running && (userInput = consoleReader.readLine()) != null) {
                if (!running) break; // Check flag in case listener thread changed it

                String trimmedInput = userInput.trim();
                if (trimmedInput.equalsIgnoreCase("exit")) {
                    out.println("EXIT");
                    running = false;
                    break;
                } else if (trimmedInput.equalsIgnoreCase("list")) {
                    out.println("LIST_CLIENTS");
                } else if (trimmedInput.toLowerCase().startsWith("cmd ")) {
                    // Format: cmd <targetAgentId> <command_for_agent>
                    String[] parts = trimmedInput.split(" ", 3);
                    if (parts.length == 3) {
                        String targetAgentId = parts[1];
                        String commandForAgent = parts[2];
                        out.println("RELAY " + targetAgentId + " " + commandForAgent);
                    } else {
                        System.out.println("Invalid cmd format. Use: cmd <targetAgentId> <command>");
                    }
                } else if (trimmedInput.equalsIgnoreCase("help")) {
                    System.out.println("Available commands:");
                    System.out.println("  list         - List connected agent clients.");
                    System.out.println("  cmd <id> <cmd> - Send <cmd> to agent with <id>.");
                    System.out.println("  exit         - Disconnect and exit.");
                } else if (trimmedInput.isEmpty()){
                    // just re-prompt
                } else if (trimmedInput.toLowerCase().startsWith("cmdtoall ")) {
                    // Format: relay <targetAgentId> <message>
                    String[] parts = trimmedInput.split(" ", 2);
                    if (parts.length == 2) {
                        String message = parts[1];
                        out.println("RELAY_ALL " + message);
                    } else {
                        System.out.println("Invalid relay format. Use: relay <targetAgentId> <message>");
                    }

                }
                else {
                    System.out.println("Unknown command. Type 'help'.");
                }
                 if(running) System.out.print("Controller> ");
            }

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + SERVER_HOSTNAME);
        } catch (IOException e) {
             if(running) System.err.println("Controller I/O error or connection failed: " + e.getMessage());
        } finally {
            running = false;
            System.out.println("Controller Client [" + (myControllerId != null ? myControllerId : "UNKNOWN") + "] shutting down.");
            try { if (consoleReader != null) consoleReader.close(); } catch (IOException e) {/*ignore*/}
            // Socket and its streams are closed by try-with-resources
            // Server listener thread will see running=false and/or IOException and exit.
        }
    }
}