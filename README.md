# RAT (Remote Administration Toolkit)

A lightweight Java-based remote administration toolkit (RAT) that demonstrates client-server-agent architecture for remote command execution and system information gathering across distributed systems.

## Features

- **Server (Middleware):** Acts as a central relay between controllers and agent clients.
- **Agent Client:** Connects to the server, receives commands, and executes them on the host machine (e.g., run shell commands, collect system info).
- **Controller Client:** Connects to the server and allows an operator to list agents, send commands to specific agents or all agents, and receive responses.
- **Multi-client support:** Multiple controllers and agents can connect simultaneously.
- **Command relay:** Supports command/response workflow through the middleware server.
- **System information gathering:** Agents can report OS details, memory usage, processor info, and more.
- **Extensible:** Easy to add more commands to the agent.

## Project Structure

- `Server.jar` – The middleware server (built via Maven).
- `AgentClient.java` – Java source file for the agent client.
- `ControllerClient.java` – Java source file for the controller client.
- `build.sh` – Shell script to automate compilation and packaging.

## Build Instructions

Requirements:
- Java (version 8+ recommended)
- Maven

To build everything (server, agent, controller), simply run:

```bash
./build.sh
```

This will:
- Compile and package the server as `dist/Server.jar`
- Compile and package the agent and controller clients as `dist/AgentClient.jar` and `dist/ControllerClient.jar`
- Generate `.md5` checksums for all artifacts in the `dist` directory

## Usage
Following segment **WILL** require you to do the Build Instruction steps (if there is no direct execution in the step)
### 1. Start the Server

```bash
java -jar dist/Server.jar
```
Or, if you want to run directly from source:
```bash
mvn exec:java
```

### 2. Start One or More Agent Clients

```bash
java -jar dist/AgentClient.jar
```
Or, if you want to run directly from source:
```bash
java AgentClient.java
```

### 3. Start One or More Controller Clients

```bash
java -jar dist/ControllerClient.jar
```
Or, directly from source:
```bash
java ControllerClient.java
```

### 4. Controller Commands

After connecting, the controller supports:
- `list` – List connected agents
- `cmd <agentId> <command>` – Send command to specific agent (e.g., `cmd 123 GET_SYS_INFO` or `cmd 123 EXEC whoami`)
- `cmdtoall <command>` – Broadcast command to all agents
- `help` – Show help
- `exit` – Disconnect controller

### 5. Agent Supported Commands

- `GET_SYS_INFO` – Returns system information (OS, memory, CPU, etc.)
- `EXEC <command>` – Executes the given system command (caution: executes arbitrary shell command on agent host!)

## Extending

To add new commands, update the `AgentClient.java` switch statement in the main command loop.

## Security Notice

This project is for **educational and research purposes only**. Running or modifying this code on systems without explicit permission is **illegal** and unethical. The code executes arbitrary commands on agent hosts and is **not secure** for production use.

## License

                    Copyright 2025 Stoppedwumm

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

---

*Project maintained by [Stoppedwumm](https://github.com/Stoppedwumm)*