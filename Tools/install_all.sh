#!/bin/bash

adb devices | grep -v List | awk '{ print $1 }' |xargs -L1 -I% -P10 adb -s % install -r $(find . -name Linphone-debug.apk)
adb devices | grep -v List | awk '{ print $1 }' |xargs -L1 -I% -P10 adb -s % shell am start -n com.vtcsecure/com.vtcsecure.LinphoneLauncherActivity
