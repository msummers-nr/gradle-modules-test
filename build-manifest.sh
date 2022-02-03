#!/usr/bin/env bash
#
# For Java
# Set Java version and distribution to match https://github.com/actions/setup-java
echo "::set-output name=JAVA_VERSION::11"
echo "::set-output name=JAVA_DISTRIBUTION::temurin"
#
# For Gradle
# 1. You must commit gradlew and the gradle directory
# 2. JAVA_VERSION (above) must be compatible with version of Gradle the wrapper invokes
#    - You can identfy the Gradle version by looking at distributionUrl in gradle/wrapper/gradle-wrapper.properties 
#    - The Gradle version compatibility matrix is here https://docs.gradle.org/current/userguide/compatibility.html
echo "::set-output name=GRADLE_VERSION::4.10.2"
