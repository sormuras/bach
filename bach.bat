@ECHO OFF

IF "%~1" == "boot" (
  REM SHIFT 1
  jshell src\Bach.java bach.jshell %2 %3 %4 %5 %6 %7 %8 %9
  EXIT /B %ERRORLEVEL%
)

java src\Bach.java %*
EXIT /B %ERRORLEVEL%
