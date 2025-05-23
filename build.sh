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
    rm -rf dist
fi

if [ -d build ]; then
    rm -rf build
fi

mkdir dist
mkdir build

echo Building server

# build the jar
mvn compile
mvn clean compile assembly:single

mv target/rat-1.0-SNAPSHOT-jar-with-dependencies.jar dist/Server.jar

echo Server built
echo Building Agent

# build the agent
javac -d build AgentClient.java
jar cvfe dist/AgentClient.jar AgentClient build/AgentClient.class

echo Agent built
echo Building Controller

# build the controller
javac -d build ControllerClient.java
jar cvfe dist/ControllerClient.jar ControllerClient build/ControllerClient.class

echo Controller built

END=$(date +%s)
DIFF=$(( $END - $START ))
echo "It took $DIFF seconds"