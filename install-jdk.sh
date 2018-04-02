#!/usr/bin/env bash

#
# Install JDK for Linux
#
# This script determines the most recent early-access build number,
# downloads the JDK archive to the user home directory and extracts
# it there.
#
# Example usage
#
#   install-jdk.sh                 | install most recent (early-access) JDK
#   install-jdk.sh -W /usr/opt     | install most recent (early-access) JDK to /usr/opt
#   install-jdk.sh -C              | install most recent (early-access) JDK with linked system CA certificates
#   install-jdk.sh -F 9            | install most recent OpenJDK 9
#   install-jdk.sh -F 10           | install most recent OpenJDK 10
#   install-jdk.sh -F 11           | install most recent OpenJDK 11
#   install-jdk.sh -F 11 -L BCL    | install most recent Oracle JDK 11
#
# Options
#
#   -F f | Feature number of the JDK release  [9|10|...]
#   -B b | Build number of the JDK release    [?|1|2...]
#   -L l | License of the JDK                 [GPL|BCL]
#   -W w | Working directory and install path [${HOME}]
#   -C c | Use system CA certificates (currently only Debian/Ubuntu is supported)
#
# Exported environment variables
#
#   JAVA_HOME is set to the extracted JDK directory
#   PATH is prepended with ${JAVA_HOME}/bin
#
# (C) 2018 Christian Stein
#
# https://github.com/sormuras/bach/blob/master/install-jdk.sh
#
set -e

JDK_FEATURE='11'
JDK_BUILD='?'
JDK_LICENSE='GPL'
JDK_WORKSPACE=${HOME}
JDK_SYSTEM_CACERTS='0'

while getopts F:B:L:W:C option
do
  case "${option}" in
    F) JDK_FEATURE=${OPTARG};;
    B) JDK_BUILD=${OPTARG};;
    L) JDK_LICENSE=${OPTARG};;
    W) JDK_WORKSPACE=${OPTARG};;
    C) JDK_SYSTEM_CACERTS="1";;
 esac
done

#
# Other constants
#
JDK_DOWNLOAD='https://download.java.net/java'
JDK_BASENAME='openjdk'
if [ "${JDK_LICENSE}" != 'GPL' ]; then
  JDK_BASENAME='jdk'
fi

#
# 9 or 10
#
if [ "${JDK_FEATURE}" == '9' ] || [ "${JDK_FEATURE}" = '10' ]; then
  if [ "${JDK_LICENSE}" == 'GPL' ]; then
    if [ "${JDK_BUILD}" == '?' ]; then
      TMP=$(curl -L jdk.java.net/${JDK_FEATURE})
      TMP="${TMP#*<h1>JDK}"                                   # remove everything before the number
      TMP="${TMP%%General-Availability Release*}"             # remove everything after the number
      JDK_BUILD="$(echo -e "${TMP}" | tr -d '[:space:]')"     # remove all whitespace
    fi
    JDK_ARCHIVE=${JDK_BASENAME}-${JDK_BUILD}_linux-x64_bin.tar.gz
    JDK_URL=${JDK_DOWNLOAD}/GA/jdk${JDK_FEATURE}/${JDK_BUILD}/binaries/${JDK_ARCHIVE}
    JDK_HOME=jdk-${JDK_BUILD}
  fi
  # TODO: Support Oracle JDK 9?
  if [ "${JDK_LICENSE}" == 'BCL' ] && [ "${JDK_FEATURE}" == '10' ]; then
    JDK_ARCHIVE=jdk-10_linux-x64_bin.tar.gz
    JDK_URL=http://download.oracle.com/otn-pub/java/jdk/10+46/76eac37278c24557a3c4199677f19b62/jdk-10_linux-x64_bin.tar.gz
    JDK_HOME=jdk-10
  fi
fi

#
# 11
#
if [ "${JDK_FEATURE}" == '11' ]; then
  if [ "${JDK_BUILD}" == '?' ]; then
    TMP=$(curl -L jdk.java.net/${JDK_FEATURE})
    TMP="${TMP#*Most recent build: jdk-${JDK_FEATURE}-ea+}" # remove everything before the number
    TMP="${TMP%%<*}"                                        # remove everything after the number
    JDK_BUILD="$(echo -e "${TMP}" | tr -d '[:space:]')"     # remove all whitespace
  fi

  JDK_ARCHIVE=${JDK_BASENAME}-${JDK_FEATURE}-ea+${JDK_BUILD}_linux-x64_bin.tar.gz
  JDK_URL=${JDK_DOWNLOAD}/early_access/jdk${JDK_FEATURE}/${JDK_BUILD}/${JDK_LICENSE}/${JDK_ARCHIVE}
  JDK_HOME=jdk-${JDK_FEATURE}
fi

#
# Create any missing intermediate paths, switch to workspace, download, unpack, switch back.
#
mkdir -p ${JDK_WORKSPACE}
cd ${JDK_WORKSPACE}
wget --continue --header "Cookie: oraclelicense=accept-securebackup-cookie" ${JDK_URL}
tar -xzf ${JDK_ARCHIVE}
cd -

#
# Update environment and test-drive.
#
export JAVA_HOME=${JDK_WORKSPACE}/${JDK_HOME}
export PATH=${JAVA_HOME}/bin:$PATH

#
# Link to system certificates to prevent:
# SunCertPathBuilderException: unable to find valid certification path to requested target
#
if [ "${JDK_SYSTEM_CACERTS}" == '1' ]; then
  mv "${JAVA_HOME}/lib/security/cacerts" "${JAVA_HOME}/lib/security/cacerts.jdk"
  # TODO: Support for other distros than Debian/Ubuntu could be provided
  ln -s /etc/ssl/certs/java/cacerts "${JAVA_HOME}/lib/security/cacerts"
fi

java --version
