@echo off

setlocal

for %%i in ("%~dp0..") do set BACH_HOME=%%~fi
set BIN=%BACH_HOME%\bin
set SRC=%BACH_HOME%\src

echo ^| BAT = %~dpnx0
echo ^| BIN = %BIN%
echo ^| SRC = %SRC%

if "%JAVA_HOME%"=="" (
  echo JAVA_HOME not set, trying via PATH
  set JAVAC="javac.exe"
) else (
  set JAVAC="%JAVA_HOME%\bin\javac.exe"
)

@echo on
%JAVAC% --release 17 --module run.bach,run.duke --module-version 2023-ea+src --module-source-path %SRC%\*\main\java -d %BIN%
@echo off

endlocal

exit /B
