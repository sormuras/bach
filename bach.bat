@ECHO OFF

IF [%1]==[init] GOTO INIT

java --module-path .bach\cache --module com.github.sormuras.bach %*

GOTO END

:INIT
del .bach\cache\com.github.sormuras.bach@*.jar >nul 2>&1
SETLOCAL
IF [%2]==[] ( SET tag=early-access ) ELSE ( SET tag=%2 )
jshell -R-Dreboot -R-Dversion=%tag% https://bit.ly/bach-main-init
ENDLOCAL

:END
