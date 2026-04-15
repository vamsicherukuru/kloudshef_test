#!/bin/bash
# Run Kloudshef backend with Java 21 (required)
export JAVA_HOME=/Users/vamsicherukuru/Library/Java/JavaVirtualMachines/openjdk-21.0.2/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

echo "Using Java: $(java -version 2>&1 | head -1)"
mvn spring-boot:run
