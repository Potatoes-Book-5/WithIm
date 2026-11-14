#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew :app:installDebug :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.potatoes.book5.realestate.vehicle.gleo.GleoWireTest,com.potatoes.book5.realestate.vehicle.gleo.VoiceScrapE2ETest
