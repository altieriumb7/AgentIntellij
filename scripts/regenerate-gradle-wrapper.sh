#!/usr/bin/env bash
set -euo pipefail

# Regenerates Gradle wrapper artifacts locally (including gradle-wrapper.jar)
# Usage: ./scripts/regenerate-gradle-wrapper.sh [gradle-version]

GRADLE_VERSION="${1:-8.14.3}"

gradle wrapper --gradle-version "${GRADLE_VERSION}" --no-validate-url

echo "Gradle wrapper regenerated for ${GRADLE_VERSION}."
