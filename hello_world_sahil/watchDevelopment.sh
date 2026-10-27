#!/bin/bash
# Use nodemon's `--verbose` flag to identify which files trigger restarts.
# Add unnecessary files to `--ignore` to prevent restarts when they change.
nodemon -e java,xml,gradle -x './gradlew installDebug && adb shell am start -n com.helloworld/com.helloworld.MainActivity'
