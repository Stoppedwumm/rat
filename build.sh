START=$(date +%s)
echo Build starting
# check if java is installed
if ! [ -x "$(command -v java)" ]; then
    echo 'Error: java is not installed.' >&2
    exit 1
fi

# check if maven is installed
if ! [ -x "$(command -v mvn)" ]; then
    echo 'Error: maven is not installed.' >&2
    exit 1
fi

# check if directories already exist
if [ -d dist ]; then
    echo "Removing existing dist directory..."
    rm -rf dist
fi

if [ -d build ]; then
    echo "Removing existing build directory..."
    rm -rf build
fi

mkdir dist
mkdir build

echo Building server

# build the jar
mvn compile # Compile first to ensure dependencies are downloaded/checked
mvn clean compile assembly:single # Then clean and build the fat jar

mv target/rat-1.0-SNAPSHOT-jar-with-dependencies.jar dist/Server.jar

echo Server built
echo Building Agent

# build the agent
javac -d build AgentClient.java
# --- FIX: Use subshell to change directory for jar command ---
(cd build && jar cvfe ../dist/AgentClient.jar AgentClient AgentClient.class)
# -------------------------------------------------------------

echo Agent built
echo Building Controller

# build the controller
javac -d build ControllerClient.java
# --- FIX: Use subshell to change directory for jar command ---
(cd build && jar cvfe ../dist/ControllerClient.jar ControllerClient ControllerClient.class)
# -------------------------------------------------------------

echo Controller built

echo Build complete
echo Generating checksums

# cd into dist *after* all files are moved there
cd dist
md5sum Server.jar > Server.jar.md5
md5sum AgentClient.jar > AgentClient.jar.md5
md5sum ControllerClient.jar > ControllerClient.jar.md5

END=$(date +%s)
DIFF=$(( $END - $START ))
echo "It took $DIFF seconds"