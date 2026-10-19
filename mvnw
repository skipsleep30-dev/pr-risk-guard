#!/bin/sh
set -eu

MAVEN_VERSION="3.9.11"
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAVEN_DIR="$PROJECT_DIR/.mvn/apache-maven-$MAVEN_VERSION"
ARCHIVE="$PROJECT_DIR/.mvn/apache-maven-$MAVEN_VERSION-bin.tar.gz"
DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz"

if [ ! -x "$MAVEN_DIR/bin/mvn" ]; then
    mkdir -p "$PROJECT_DIR/.mvn"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "$DOWNLOAD_URL" -o "$ARCHIVE"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "$DOWNLOAD_URL" -O "$ARCHIVE"
    else
        echo "curl or wget is required to download Maven" >&2
        exit 1
    fi
    tar -xzf "$ARCHIVE" -C "$PROJECT_DIR/.mvn"
fi

exec "$MAVEN_DIR/bin/mvn" "$@"
