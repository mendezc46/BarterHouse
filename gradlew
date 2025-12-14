#!/usr/bin/env bash
# Gradle Wrapper para BarterHouse
# Este script descarga y ejecuta Gradle 7.6

if [ ! -d "gradle/wrapper" ]; then
    mkdir -p gradle/wrapper
fi

GRADLE_VERSION="7.6"
GRADLE_HOME="${HOME}/.gradle/wrapper/gradle-${GRADLE_VERSION}"

if [ ! -d "${GRADLE_HOME}" ]; then
    echo "Descargando Gradle ${GRADLE_VERSION}..."
    mkdir -p "${HOME}/.gradle/wrapper"
    cd "${HOME}/.gradle/wrapper"
    wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip
    unzip -q gradle-${GRADLE_VERSION}-bin.zip
    rm gradle-${GRADLE_VERSION}-bin.zip
    cd -
fi

"${GRADLE_HOME}/bin/gradle" "$@"
