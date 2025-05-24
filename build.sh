#!/bin/bash

set -e

# pipefail remove because fucking github actions and stuff

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

echo Building server

# build the jar
mvn compile # Compile first to ensure dependencies are downloaded/checked
mvn clean compile assembly:single # Then clean and build the fat jar

mv target/rat-*-jar-with-dependencies.jar dist/Server.jar

echo Server built
echo Building Agent

cd AgentClient
if [ "$#" -eq 1 ] && [ "$1" == "--prebuilt" ]; then
    echo "Using prebuilt agent"
    echo '{ "prebuilt": true }' > app/src/main/resources/config.json
else
    echo "Using custom agent"
    echo '{ "prebuilt": false }' > app/src/main/resources/config.json
fi

./gradlew clean
./gradlew jar
mv app/build/libs/app.jar ../dist/AgentClient.jar

echo Agent built
echo Building Controller

cd ../ControllerClient
./gradlew clean
./gradlew jar
mv app/build/libs/app.jar ../dist/ControllerClient.jar

echo Controller built

echo Build complete
echo Generating checksums

# cd into dist *after* all files are moved there
cd ../dist
md5sum Server.jar > Server.jar.md5
md5sum AgentClient.jar > AgentClient.jar.md5
md5sum ControllerClient.jar > ControllerClient.jar.md5

END=$(date +%s)
DIFF=$(( $END - $START ))
echo "It took $DIFF seconds"