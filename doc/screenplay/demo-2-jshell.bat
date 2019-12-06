@echo off
cls
echo.
echo BEGIN OF DEMO 2: build.jsh
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
echo Step 2: Create JShell-based build script: build.jsh
pause > nul
(
echo /open https://github.com/sormuras/bach/raw/master/BUILDING
echo run^("javac", "-d", Path.of^("target/classes"^) + "", "--module-source-path", "src", "--module", "demo"^)
echo run^("jar", "--create", "--file", Path.of^("target/demo.jar"^) + "", "-C", Path.of^("target/classes/demo"^) + "", "."^)
echo /exit
)>"build.jsh"
echo.
type build.jsh
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
jshell --show-version --execution local build.jsh
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
jar --describe-module --file target\demo.jar
pause > nul

cd ..
echo.
echo END OF DEMO
echo.
