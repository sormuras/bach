@echo off

setlocal

for %%i in ("%~dp0..") do set BACH_HOME=%%~fi
set BIN=%BACH_HOME%\bin
set SRC=%BACH_HOME%\src

if "%BACH_VERBOSE%"=="true" (
  echo ^| BAT = %~dpnx0
  echo ^| BIN = %BIN%
  if exist %SRC% echo ^| SRC = %SRC%
)

if "%JAVA_HOME%"=="" (
  echo JAVA_HOME not set, trying via PATH
  set JAVA="java.exe"
  set JAVAC="javac.exe"
) else (
  set JAVA="%JAVA_HOME%\bin\java.exe"
  set JAVAC="%JAVA_HOME%\bin\javac.exe"
)

if exist "%SRC%\run.bach\main\java\module-info.java" (
  if "%BACH_VERBOSE%"=="true" (
    echo ^| BOOT
  )
  %JAVAC% --release 17 --module run.bach,run.duke --module-version 2022-ea+src --module-source-path %SRC%\*\main\java -d %BIN%
)

set BACH=-ea --module-path %BIN% --add-modules ALL-DEFAULT,ALL-MODULE-PATH --module run.bach/run.bach.Main

if "%BACH_VERBOSE%"=="true" (
  echo ^| JAVA = %JAVA%
  echo ^| BACH = %BACH%
  echo ^| ARGS = "%*"
)

%JAVA% %BACH% %*

endlocal

exit /B
