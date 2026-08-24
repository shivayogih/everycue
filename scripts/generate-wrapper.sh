#!/usr/bin/env bash
set -euo pipefail

gradle wrapper --gradle-version 9.5.0 --distribution-type bin
chmod +x gradlew
printf 'Gradle wrapper generated. Run: ./gradlew testDebugUnitTest lintDebug assembleDebug\n'
