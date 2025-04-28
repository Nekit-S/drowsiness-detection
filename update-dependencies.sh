#!/bin/bash

echo "Updating system packages..."
sudo apt update

echo "Checking for Java installation..."
if ! command -v java &>/dev/null; then
    echo "Java not found. Installing OpenJDK 17..."
    sudo apt install -y openjdk-17-jdk
else
    java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    echo "Java is installed: $java_version"
fi

echo "Checking for Gradle..."
if ! command -v gradle &>/dev/null; then
    echo "Gradle not found. Installing via SDKMAN..."
    if ! command -v sdk &>/dev/null; then
        echo "Installing SDKMAN..."
        curl -s "https://get.sdkman.io" | bash
        source "$HOME/.sdkman/bin/sdkman-init.sh"
    fi
    sdk install gradle
else
    gradle_version=$(gradle --version | grep Gradle | head -1)
    echo "Gradle is installed: $gradle_version"
fi

echo "Checking for Docker..."
if ! command -v docker &>/dev/null; then
    echo "Docker not found. Please install Docker manually."
else
    docker_version=$(docker --version)
    echo "Docker is installed: $docker_version"
fi

echo "Updating project dependencies..."
if [ -f "build.gradle" ]; then
    echo "Updating Gradle dependencies..."
    ./gradlew --refresh-dependencies
fi

echo "Installing OpenCV dependencies..."
sudo apt install -y libopencv-dev libgtk2.0-dev pkg-config libavcodec-dev libavformat-dev libswscale-dev

echo "All dependencies are up to date!"
