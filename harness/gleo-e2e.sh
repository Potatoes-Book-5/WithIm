#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 harness/check-pleos-device.py
./gradlew :app:installDebug :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.potatoes.book5.realestate.vehicle.gleo.GleoWireTest,com.potatoes.book5.realestate.vehicle.gleo.VoiceScrapE2ETest,com.potatoes.book5.realestate.vehicle.gleo.PreparedMarketE2ETest,com.potatoes.book5.realestate.vehicle.gleo.LivePriceE2ETest
