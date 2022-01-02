# Learning Android

## cli usage with android projects:

**Use below command to generate apk and install right away**:

```bash
#NOTE: IF below doesn't install app then it means you need to uninstall app for all users and it re-running command would definitely work!!
./gradlew installDebug
# This generates apk and installs to any available adb device that is connected
```

If you only want to generate apk then:

For immediate app testing and debugging, you can build a debug APK. The debug APK is signed with a debug key provided by the SDK tools and allows debugging through adb.

To build a debug APK, open a command line and navigate to the root of your project directory. To initiate a debug build, invoke the assembleDebug task:

```bash
./gradlew assembleDebug

# To get warnings causing the issues you can append --warning-mode option like that:
./gradlew assembleDebug --warning-mode=all
```
