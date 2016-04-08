#!/bin/bash
set -xe
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/../submodules


CMAKECURRENTSOURCEDIR="cmake-builder"

cp -f $CMAKECURRENTSOURCEDIR/builders/opencoreamr/CMakeLists.txt externals/opencore-amr/

cp -f $CMAKECURRENTSOURCEDIR/builders/opus/CMakeLists.txt externals/opus/
cp -f $CMAKECURRENTSOURCEDIR/builders/opus/config.h.cmake externals/opus/

cp -f $CMAKECURRENTSOURCEDIR/builders/sqlite3/CMakeLists.txt externals/sqlite3/

cp -f $CMAKECURRENTSOURCEDIR/builders/voamrwbenc/CMakeLists.txt externals/vo-amrwbenc/

cp -f $CMAKECURRENTSOURCEDIR/builders/xml2/CMakeLists.txt externals/libxml2/
cp -f $CMAKECURRENTSOURCEDIR/builders/xml2/config.h.cmake externals/libxml2/