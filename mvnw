#!/bin/bash
MAVEN_HOME="${MAVEN_HOME:-$HOME/.m2/wrapper/dists/apache-maven-3.9.6-bin/maven}"
if [ ! -d "$MAVEN_HOME" ]; then
    mkdir -p "$HOME/.m2/wrapper/dists"
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz | tar -xz -C "$HOME/.m2/wrapper/dists"
    mv "$HOME/.m2/wrapper/dists/apache-maven-3.9.6" "$MAVEN_HOME"
fi
exec "$MAVEN_HOME/bin/mvn" "$@"
