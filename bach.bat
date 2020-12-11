@ECHO OFF

IF [%1]==[init] GOTO INIT

java --module-path .bach\cache --module com.github.sormuras.bach %*

GOTO END

:INIT
del .bach\cache\com.github.sormuras.bach@*.jar >nul 2>&1
SETLOCAL
SET tag=%2
IF [%tag%]==[] SET tag=early-access
jshell -R-Dreboot -R-Dversion=%tag% https://bit.ly/bach-main-init
ENDLOCAL

:END
