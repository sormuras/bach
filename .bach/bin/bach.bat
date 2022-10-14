@echo off

if "%JAVA_HOME%"=="" (
  echo JAVA_HOME not set, trying via PATH
  set JAVA="java"
) else (
  set JAVA="%JAVA_HOME%"\bin\java
)

set BACH=%~dp0bach.java

%JAVA% %BACH% %*

exit /B
