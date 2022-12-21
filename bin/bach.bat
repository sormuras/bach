@echo off

setlocal

for %%i in ("%~dp0..") do set BACH_HOME=%%~fi
set BIN=%BACH_HOME%\bin

if "%BACH_VERBOSE%"=="true" (
  echo ^| BAT = %~dpnx0
  echo ^| BIN = %BIN%
)

if "%JAVA_HOME%"=="" (
  echo JAVA_HOME not set, trying via PATH
  set JAVA="java.exe"
) else (
  set JAVA="%JAVA_HOME%\bin\java.exe"
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
