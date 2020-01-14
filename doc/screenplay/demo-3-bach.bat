@echo off
cls
echo.
echo BEGIN OF DEMO 3: Bach.java
pause > nul

if exist demo rmdir /q/s demo
mkdir demo
cd demo
mkdir src\demo

echo ______
echo Step 1: Declare demo module: src\demo\module-info.java
pause > nul
(
echo module demo {}
)>"src\demo\module-info.java"
echo.
type src\demo\module-info.java
pause > nul

echo ______
echo Step 2: NOOP -- no local build file is needed
pause > nul

echo ______
echo Step 3: Show tree of source files
pause > nul
echo.
tree /f . | findstr /v Volume | findstr /v :
pause > nul

echo ______
echo Step 4: Build
pause > nul
@echo on
jshell --show-version --execution local https://bit.ly/bach-build
@echo off
pause > nul

echo ______
echo Step 5: Show tree of source and binary files
pause > nul
echo.
tree /f . | findstr /v Volume | findstr /v :
pause > nul

echo ______
echo Step 6: Describe demo module
pause > nul
echo.
jar --describe-module --file .bach\out\main\modules\demo-0.jar
pause > nul

cd ..
echo.
echo END OF DEMO
echo.
