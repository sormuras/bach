REM Call this batch from the base directory to bootstrap Bach and boot it into a JShell session
rmdir /Q/S .bach\workspace
del  .bach\cache\*.jar
java .bach\build\build\Bootstrap.java
IF %ERRORLEVEL% == 0 (cls && jshell boot)
