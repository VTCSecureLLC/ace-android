#!/bin/bash

# Only deploy master branch builds

if [ -z "$TRAVIS_BRANCH" ] ; then
  echo "TRAVIS_BRANCH not found. Deploy skipped"
  exit 0
fi

if [ "$TRAVIS_BRANCH" != "master" ] ; then
  echo "TRAVIS_BRANCH is not master. Deploy skipped"
  exit 0
fi

set -xe

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

# Prepare semantic versioning tag

SHA1=$(git rev-parse --short HEAD)

tag="$(bundle exec semver)-${TRAVIS_BUILD_NUMBER:-1}"-${SHA1}

# Prepare other variables

IFS=/ GITHUB_REPO=($TRAVIS_REPO_SLUG); IFS=" "

# Create a GitHub release if credentials are available, and upload apk files

set +ex
if [ -z "${GITHUB_TOKEN}" ]; then
  echo GITHUB_TOKEN is not defined. Neither uploading apk files, nor creating a GitHub release.
else
  set -x

  curl -sL https://github.com/aktau/github-release/releases/download/v0.6.2/linux-amd64-github-release.tar.bz2 | bunzip2 -cd | tar xf - --strip=3 -C /tmp/

  chmod 755 /tmp/github-release

  /tmp/github-release release \
      --user ${GITHUB_REPO[0]:-VTCSecureLLC} \
      --repo ${GITHUB_REPO[1]:-ace-android} \
      --tag $tag \
      --name "Travis-CI Automated $tag" \
      --description "$(git log -1 --pretty=format:%B)" \
      --pre-release

  echo "Uploading $APK_FILE as ACE-$tag-debug.apk to github release $tag"

  /tmp/github-release upload \
      --user ${GITHUB_REPO[0]:-VTCSecureLLC} \
      --repo ${GITHUB_REPO[1]:-ace-android} \
      --tag $tag \
      --name ACE-$tag-debug.apk \
      --file $APK_FILE
fi

# Create a HockeyApp release if credentials are available, and upload apk files

set +x
if [ -z "${HOCKEYAPP_TOKEN}" ]; then
    echo HOCKEYAPP_TOKEN is not defined. Neither uploading apk files, nor creating a HockeyApp release.
else

  echo "Uploading to HockeyApp"
  curl \
    -F "status=2" \
    -F "notify=0" \
    -F "commit_sha=${SHA1}" \
    -F "build_server_url=https://travis-ci.org/${TRAVIS_REPO_SLUG}/builds/${TRAVIS_BUILD_ID}" \
    -F "repository_url=http://github.com/${TRAVIS_REPO_SLUG}" \
    -F "release_type=2" \
    -F "notes=$(git log -1 --pretty=format:%B)" \
    -F "notes_type=0" \
    -F "ipa=$APK_FILE" \
    -H "X-HockeyAppToken: ${HOCKEYAPP_TOKEN}" \
    https://rink.hockeyapp.net/api/2/apps/d6280d4d277d6876c709f4143964f0dc/app_versions/upload \
  | python -m json.tool

fi

