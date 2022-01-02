#!/bin/bash
nodemon -e java,xml,gradle -x './gradlew installDebug && adb shell am start -n com.helloworld/com.helloworld.MainActivity'
