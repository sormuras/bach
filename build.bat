@echo off

cls

jshell --show-version ^
       --enable-preview ^
       -R-ea ^
       -R-Djava.util.logging.config.file=src/logging.properties ^
       -R-Debug ^
       build.jsh

java .bach/src/Bach.java help

.bach/workspace/image/bin/bach version
