@ECHO OFF

:BEGIN
IF [%1]==[boot] GOTO :BOOT
IF [%1]==[clean] GOTO :CLEAN
IF [%1]==[describe] GOTO :DESCRIBE
IF [%1]==[/?] GOTO :HELP
IF [%1]==[help] GOTO :HELP
IF [%1]==[version] GOTO :VERSION

java --module-path .bach\cache --module com.github.sormuras.bach %*
GOTO :END

:BOOT
jshell https://github.com/sormuras/bach/raw/HEAD/.bach/boot.jsh
GOTO :END

:CLEAN
IF EXIST .bach\workspace rmdir /q/s .bach\workspace
SHIFT
IF [%1]==[] GOTO :END
GOTO :BEGIN

:VERSION
IF [%2]==[] GOTO :DESCRIBE
jshell -R-Dversion=%2 https://github.com/sormuras/bach/raw/HEAD/.bach/pull.jsh
GOTO :END

:DESCRIBE
java --module-path .bach\cache --describe-module com.github.sormuras.bach
GOTO :END

:HELP
ECHO.
ECHO Usage: %0 [action] [args...]
ECHO.
ECHO Where actions include:
ECHO.
ECHO   boot
ECHO     Launch an interactive JShell session with Bach's module on the module-path.
ECHO.
ECHO   describe
ECHO     Describe module com.github.sormuras.bach and exit.
ECHO.
ECHO   version [tag]
ECHO     Download Bach for the specified version tag.
ECHO     Find released version tags at https://github.com/sormuras/bach/releases
ECHO     If no tag is given a description of the current module is printed.
ECHO     Example: %0 version 15-ea+2
ECHO.

:END
