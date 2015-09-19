#!/bin/bash
set -x

curl -sL https://github.com/aktau/github-release/releases/download/v0.5.3/darwin-amd64-github-release.tar.bz2 | bunzip2 -cd | tar xf - --strip=3 -C /tmp/

tag="$(bundle exec semver)-${TRAVIS_BUILD_NUMBER:-1}"-$(git rev-parse --short HEAD)

/tmp/github-release release \
    --user VTCSecureLLC \
    --repo linphone-android \
    --tag $tag \
    --name "CI Automated $tag" \
    --description "This is an automatically generated tag that will eventually be expired" \
    --pre-release

/tmp/github-release upload \
    --user VTCSecureLLC \
    --repo linphone-android \
    --tag $tag \
    --name Ace-$(tag)-debug.apk \
    --file bin/Linphone-debug.apk

