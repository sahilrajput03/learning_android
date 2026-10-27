#!/bin/bash
# Use nodemon's `--verbose` flag to identify which files trigger restarts.
# Add unnecessary files to `--ignore` to prevent restarts when they change.
nodemon \
    --ignore './app/build/**' \
    -e java,xml,gradle \
    -x './gradlew installDebug && adb shell am start -n com.sahilrajput.pomodoro/com.sahilrajput.pomodoro.MainActivity'


# Source: file:///Users/apple/Documents/github_repos/pwa-to-apk-using-twa/sahilrajput-bw-project/watchDevelopment.sh
# --ignore ./app/src/main/res/xml/shortcuts.xml \