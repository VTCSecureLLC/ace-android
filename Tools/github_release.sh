#!/bin/bash
set -x

APK_FILE=""

if [ -f bin/Linphone-debug.apk ]; then
  APK_FILE=bin/Linphone-debug.apk
fi

if [ -f build/outputs/apk/linphone-android-debug.apk ]; then
  APK_FILE=build/outputs/apk/linphone-android-debug.apk
fi

if [ -z "$APK_FILE" ]; then
  echo "Could not find an apk file to publish"
  exit 1
fi

curl -sL https://github.com/aktau/github-release/releases/download/v0.6.2/linux-amd64-github-release.tar.bz2 | bunzip2 -cd | tar xf - --strip=3 -C /tmp/

chmod 755 /tmp/github-release

tag="$(bundle exec semver)-${TRAVIS_BUILD_NUMBER:-1}"-$(git rev-parse --short HEAD)

/tmp/github-release release \
    --user VTCSecureLLC \
    --repo linphone-android \
    --tag $tag \
    --name "Travis-CI Automated $tag" \
    --description "This is an automatically generated tag that will eventually be expired" \
    --pre-release

gradle crashlyticsUploadDistributionDebug
gradle crashlyticsUploadSymbolsDebug

echo "Uploading $APK_FILE as Ace-$tag-debug.apk to github release $tag"

/tmp/github-release upload \
    --user VTCSecureLLC \
    --repo linphone-android \
    --tag $tag \
    --name Linphone-$tag-debug.apk \
    --file $APK_FILE

