#!/bin/bash
set -e

CONTENT=$(curl -L jdk.java.net/10/)
TMP="${CONTENT#*Most recent build: jdk-10-ea+}" # remove preamble

JDK_FEATURE=10
JDK_BUILD="${TMP%%<*}" # remove everything after the first <
JDK_ARCHIVE=jdk-${JDK_FEATURE}-ea+${JDK_BUILD}_linux-x64_bin.tar.gz

cd ~
wget http://download.java.net/java/jdk${JDK_FEATURE}/archive/${JDK_BUILD}/binaries/${JDK_ARCHIVE}
tar -xzf ${JDK_ARCHIVE}
export JAVA_HOME=~/jdk-${JDK_FEATURE}
export PATH=${JAVA_HOME}/bin:$PATH
cd -
