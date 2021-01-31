@ECHO OFF

IF [%1]==[boot] GOTO BOOT
GOTO MAIN

:BOOT
jshell --module-path .bach\cache --add-modules com.github.sormuras.bach .bach\boot.jsh
GOTO END

:MAIN
java --module-path .bach\cache --module com.github.sormuras.bach %*
GOTO END

:END
