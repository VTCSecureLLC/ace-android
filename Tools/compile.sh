#!/bin/bash
set -xe
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/..



SUBMODULE_HASH=$(md5sum current.txt | awk '{print $1}')

if [ -e WORK/${SUBMODULE_HASH} ]; then
make generate-apk-without-sdk-build -j 8
else
make -j 8
touch WORK/${SUBMODULE_HASH}
cp Makefile WORK/Makefile
fi
#ninja -C WORK/cmake
# todo VATRP-2786
#wget http://ciscobinary.openh264.org/libopenh264-1.5.0-osx64.dylib.bz2
#bzip2 -d libopenh264-1.5.0-osx64.dylib.bz2
#mv -f  libopenh264-1.5.0-osx64.dylib  WORK/Build/linphone_package/linphone-sdk-tmp/lib/libopenh264.1.dylib



