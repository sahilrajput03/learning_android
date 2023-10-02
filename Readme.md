# Learning Android

**Quick Links:**
- **Learn Capacitor:** [Click here](https://github.com/sahilrajput03/sahilrajput03/blob/master/learn-capacitor.md)
- **Learn Google Play Console:** [Click here](learn-playconsole.md)

## Android

- Shared Preferences Storage is only accessible by the application itself: [Click here](https://developer.android.com/training/data-storage/shared-preferences#get-handle)

![image](https://user-images.githubusercontent.com/31458531/236378843-cf64bd11-3333-40c0-a7d9-ced1b74e4125.png)

- Android Shared Preferences Storage: [Source](https://stackoverflow.com/a/9244620/10012446)

![image](https://user-images.githubusercontent.com/31458531/236378634-80a988c0-ff74-41ea-bd8c-eb50aa393c26.png)



## You can also generate .aab i.e, Android App Bundle

**Q. Why .aap format instead of apk format ?**

Ans.  https://dev.to/srajesh636/how-we-reduced-our-production-apk-size-by-70-in-react-native-1lci

tldr; When you upload aab instead of apk file @ playstore you will allow playstore to generate separate apks for different platforms thus reducing the apks size for each type of device as there are different types of cpus for which apk is generated separately thus reducing apk file size.

tldr; When you generate apk you have a generic apk file which is supported for all andorid devices.

```bash
./gradlew bundleRelease
# resultng .aab file will be generated in say:
# hello_world_sahil/app/build/outputs/bundle/release/app-release.aab
```

***

**adb devices** - No permission error in adb is fixed simply via:

```bash
sudo adb kill-server
sudo adb start-server
# src: https://stackoverflow.com/a/9210397/10012446
```

My original works for ^ time @ [Setting up android environment with vscode@arch-noets](https://github.com/sahilrajput03/sahilrajput03/blob/master/arch-notes.md#setting-up-android-environment-with-vscode)

**am**: Open app with am

```bash
adb shell am start -n com.helloworld/com.helloworld.MainActivity
# e.g.2
adb shell am start -n com.helloworld/com.helloworld.SpareActivity
# source: https://stackoverflow.com/a/4567928/10012446

#OR
adb shell
am start -n com.package.name/com.package.name.ActivityName
# source: https://stackoverflow.com/a/12274218/10012446
```

Stop an app:

```bash
adb shell am force-stop com.helloworld
#OR
adb shell
am force-stop com.helloworld
```

**Monkey** - The Monkey is a program that runs on your emulator or device and generates pseudo-random streams of user events such as clicks, touches, or gestures, as well as a number of system-level events. You can use the Monkey to stress-test applications that you are developing, in a random yet repeatable manner. [Here](https://developer.android.com/studio/test/monkey).

```bash
# Open app and send 50 random actions (source: official docs link above)
adb shell monkey -p your.package.name -v 50
#OR
adb shell
monkey -p your.package.name -v 50
```

- All about on-device developer options with android studio: [Here](https://developer.android.com/studio/debug/dev-options).

## cli usage with android projects:

**Use below command to generate apk and install right away**:

```bash
#NOTE: IF below doesn't install app then it means you need to uninstall app for all users and it re-running command would definitely work!!
./gradlew installDebug
# This generates apk and installs to any available adb device that is connected

# Get generated apk
alias cd.apkNativeProject='cd app/build/outputs/apk/debug/'
# You'll see file with name `app-debug.apk`
```

If you only want to generate apk then:

For immediate app testing and debugging, you can build a debug APK. The debug APK is signed with a debug key provided by the SDK tools and allows debugging through adb.

To build a debug APK, open a command line and navigate to the root of your project directory. To initiate a debug build, invoke the assembleDebug task:

```bash
# Generates apk at app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug

# To get warnings causing the issues you can append --warning-mode option like that:
./gradlew assembleDebug --warning-mode=all
```
