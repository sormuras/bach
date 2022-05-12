@ECHO OFF

IF "%~1" == "init" (
  IF "%~2" == "" (
    ECHO "Usage: bach init VERSION"
    EXIT /B 1
  )
  jshell -R-Dbach-version=%2 https://git.io/bach-init
  EXIT /B
)

IF NOT EXIST ".bach\bin\*.jar" (
  IF NOT EXIST ".bach\bach-init.version" (
    ECHO "Usage: bach init VERSION"
    EXIT /B 1
  )
  SET /p VERSION=<.bach\bach-init.version
  jshell -R-Dbach-version=%VERSION% https://git.io/bach-init
)

IF "%~1" == "boot" (
  REM SHIFT 1
  jshell --module-path .bach\bin --add-modules ALL-MODULE-PATH,ALL-SYSTEM .bach\bin\bach.jshell %2 %3 %4 %5 %6 %7 %8 %9
  EXIT /B
)

SET JAVA_LAUNCHER_ARGUMENTS=--module-path=.bach\bin --add-modules=ALL-MODULE-PATH --add-modules=ALL-DEFAULT

IF EXIST ".bach\src\%~1.java" (
  REM PROGRAM=
  REM SHIFT 1
  java %JAVA_LAUNCHER_ARGUMENTS% ".bach\src\%~1.java" %2 %3 %4 %5 %6 %7 %8 %9
  EXIT /B
)

IF EXIST "%~1" (
  java %JAVA_LAUNCHER_ARGUMENTS% %*
  EXIT /B
)

java %JAVA_LAUNCHER_ARGUMENTS% --module com.github.sormuras.bach %*
EXIT /B
