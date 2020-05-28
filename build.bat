@echo off
cls
jshell --show-version ^
       --enable-preview ^
       -R-ea ^
       -R-Djava.util.logging.config.file=src/logging.properties ^
       -R-Debug ^
       build.jsh
