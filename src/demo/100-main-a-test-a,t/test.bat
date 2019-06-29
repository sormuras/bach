@echo off

java -ea --class-path ..\..\..\target\build\junit-platform-console-standalone-1.5.0-RC2.jar;..\..\..\lib\test-runtime-only\de.sormuras.mainrunner.engine-2.0.5.jar --module-path bin\test\modules;bin\main\modules --add-modules a,t org.junit.platform.console.ConsoleLauncher --scan-modules
rem java -ea --show-module-resolution --class-path ..\..\..\target\build\junit-platform-console-standalone-1.5.0-RC2.jar;..\..\..\lib\test-runtime-only\de.sormuras.mainrunner.engine-2.0.5.jar --module-path bin\test\modules;bin\main\modules --add-modules t org.junit.platform.console.ConsoleLauncher --scan-modules

rem java.lang.module.FindException: Module org.junit.platform.engine not found, required by de.sormuras.mainrunner.engine
rem java --class-path ..\..\..\target\build\junit-platform-console-standalone-1.5.0-RC2.jar --module-path bin\test\modules;bin\main\modules;..\..\..\lib\test-runtime-only --add-modules de.sormuras.mainrunner.engine,t org.junit.platform.console.ConsoleLauncher --scan-modules
