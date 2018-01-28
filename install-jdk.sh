#!/usr/bin/env bash

#
# Install JDK for Linux
#
# This script determines the most recent early-access build number,
# downloads the JDK archive to the user home directory and extracts
# it there.
#
# Exported environment variables
#
#   JAVA_HOME is set to the extracted JDK
#   PATH is prepended with ${JAVA_HOME}/bin
#

set -e

JDK_FEATURE='10'
JDK_BUILD='?'
JDK_LICENSE='GPL'

while getopts F:B:L: option
do
  case "${option}" in
    F) JDK_FEATURE=${OPTARG};;
    B) JDK_BUILD=${OPTARG};;
    L) JDK_LICENSE=${OPTARG};;
 esac
done

JDK_DOWNLOAD='https://download.java.net/java'
JDK_BASENAME='openjdk'
if [ "${JDK_LICENSE}" != 'GPL' ]; then
  JDK_BASENAME='jdk'
fi

if [ "${JDK_FEATURE}" == '9' ]; then
  if [ "${JDK_BUILD}" == '?' ]; then
    TMP=$(curl -L jdk.java.net/${JDK_FEATURE})
    TMP="${TMP#*<h1>JDK}"                                   # remove everything before the number
    TMP="${TMP%%General-Availability Release*}"             # remove everything after the number
    JDK_BUILD="$(echo -e "${TMP}" | tr -d '[:space:]')"     # remove all whitespace
  fi

  JDK_ARCHIVE=${JDK_BASENAME}-${JDK_BUILD}_linux-x64_bin.tar.gz
  JDK_URL=${JDK_DOWNLOAD}/GA/jdk${JDK_FEATURE}/${JDK_BUILD}/binaries/${JDK_ARCHIVE}
fi

if [ "${JDK_FEATURE}" == '10' ]; then
  if [ "${JDK_BUILD}" == '?' ]; then
    TMP=$(curl -L jdk.java.net/${JDK_FEATURE})
    TMP="${TMP#*Most recent build: jdk-${JDK_FEATURE}-ea+}" # remove everything before the number
    TMP="${TMP%%<*}"                                        # remove everything after the number
    JDK_BUILD="$(echo -e "${TMP}" | tr -d '[:space:]')"     # remove all whitespace
  fi

  JDK_ARCHIVE=${JDK_BASENAME}-${JDK_FEATURE}-ea+${JDK_BUILD}_linux-x64_bin.tar.gz
  JDK_URL=${JDK_DOWNLOAD}/jdk${JDK_FEATURE}/archive/${JDK_BUILD}/${JDK_LICENSE}/${JDK_ARCHIVE}
fi

# TODO Let user override target directory
cd ~
wget ${JDK_URL}
tar -xzf ${JDK_ARCHIVE}
export JAVA_HOME=~/jdk-${JDK_FEATURE}
export PATH=${JAVA_HOME}/bin:$PATH
cd -

java --version
