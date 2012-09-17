These are just notes, bo not follow to tightly!

Plug in your device to your development machine with a USB cable.

Ensure that USB debugging is enabled in the device Settings 
(open Settings and navitage to Applications > Development 
on most devices, or click Developer options on Android 4.0 and higher).

This application is designed for Android 2.2 (API8)

android list targets | grep 'Google Inc.:Google APIs:8'
android update project --target <number from list> --subprojects --path .
ant debug // ant release

Sign application:
jarsigner -verbose -sigalg MD5withRSA -digestalg SHA1 -keystore my-release-key.keystore my_application.apk alias_name

TO verify that it is signed:
jarsigner -verify my_signed.apk

zipalign -v 4 your_project_name-unaligned.apk your_project_name.apk

adb install bin/MyFirstApp-debug.apk
On your device, locate MyFirstActivity and open it.
