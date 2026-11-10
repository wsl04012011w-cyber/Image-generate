#!/usr/bin/env sh
# Lightweight wrapper launcher. CI provisions the pinned Gradle version with setup-gradle.
set -eu
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
VERSION="8.10.2"
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/manual-wrapper"
DIST="$BASE/gradle-$VERSION"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$BASE"
  ARCHIVE="$BASE/gradle-$VERSION-bin.zip"
  if ! command -v curl >/dev/null 2>&1; then
    echo "Gradle $VERSION is required (install Gradle or curl)." >&2
    exit 1
  fi
  curl -fL "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ARCHIVE"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ARCHIVE" -d "$BASE"
  else
    echo "unzip is required to install Gradle $VERSION." >&2
    exit 1
  fi
fi
exec "$DIST/bin/gradle" "$@"
