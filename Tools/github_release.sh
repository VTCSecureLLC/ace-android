#!/bin/bash
set -x

if [ ! -f bin/Linphone-debug.apk ]; then
  echo "Could not find a bin/Linphone-debug.apk to publish"
  exit 1
fi

curl -sL https://github.com/aktau/github-release/releases/download/v0.5.3/darwin-amd64-github-release.tar.bz2 | bunzip2 -cd | tar xf - --strip=3 -C /tmp/

chmod 755 /tmp/github-release

tag="$(bundle exec semver)-${TRAVIS_BUILD_NUMBER:-1}"-$(git rev-parse --short HEAD)

/tmp/github-release release \
    --user VTCSecureLLC \
    --repo linphone-android \
    --tag $tag \
    --name "CI Automated $tag" \
    --description "This is an automatically generated tag that will eventually be expired" \
    --pre-release

gradle crashlyticsUploadDistributionDebug
gradle crashlyticsUploadSymbolsDebug

/tmp/github-release upload \
    --user VTCSecureLLC \
    --repo linphone-android \
    --tag $tag \
    --name Ace-$(tag)-debug.apk \
    --file bin/Linphone-debug.apk

