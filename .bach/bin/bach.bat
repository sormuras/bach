@ECHO OFF

IF [%1]==[boot] GOTO BOOT
GOTO MAIN

:BOOT
jshell --module-path .bach\bin --add-modules com.github.sormuras.bach .bach\bin\boot.jsh
GOTO END

:MAIN
java --module-path .bach\bin --module com.github.sormuras.bach %*
GOTO END

:END
