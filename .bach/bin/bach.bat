@ECHO OFF

IF "%~1" == "boot" (
  REM SHIFT 1
  jshell --module-path .bach\bin --add-modules ALL-MODULE-PATH .bach\bin\bach.jshell %2 %3 %4 %5 %6 %7 %8 %9
  EXIT /B %ERRORLEVEL%
)

IF "%~1" == "clean" (
  IF EXIST .bach\workspace rmdir /S /Q .bach\workspace
  EXIT /B %ERRORLEVEL%
)

IF "%~1" == "init" (
  IF "%~2" == "" (
    ECHO "Usage: bach init VERSION"
    EXIT /B 1
  )
  jshell -R-Dbach-version=%2 https://git.io/bach-init
  EXIT /B %ERRORLEVEL%
)

IF EXIST ".bach\src\%~1.java" (
  REM PROGRAM=
  REM SHIFT 1
  java --module-path .bach\bin --add-modules ALL-MODULE-PATH ".bach\src\%~1.java" %2 %3 %4 %5 %6 %7 %8 %9
  EXIT /B %ERRORLEVEL%
)

IF EXIST "%~1" (
  java --module-path .bach\bin --add-modules ALL-MODULE-PATH %*
  EXIT /B %ERRORLEVEL%
)

java --module-path .bach\bin --module com.github.sormuras.bach %*
EXIT /B %ERRORLEVEL%
