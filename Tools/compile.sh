#!/bin/bash
set -xe
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/..
if [ -e WORK/Makefile ]; then
make generate-apk-without-sdk-build -j 8
else
make -j 8
cp Makefile WORK/Makefile
fi
#ninja -C WORK/cmake
# todo VATRP-2786
#wget http://ciscobinary.openh264.org/libopenh264-1.5.0-osx64.dylib.bz2
#bzip2 -d libopenh264-1.5.0-osx64.dylib.bz2
#mv -f  libopenh264-1.5.0-osx64.dylib  WORK/Build/linphone_package/linphone-sdk-tmp/lib/libopenh264.1.dylib



