@ECHO OFF
CLS
ECHO.
ECHO Bootstrap...
ECHO.
java .bach\src\bootstrap.java
IF NOT ERRORLEVEL 0 ( EXIT /B %ERRORLEVEL% )

ECHO.
ECHO Format...
ECHO.
java --module-path .bach\bin --add-modules ALL-MODULE-PATH .bach\src\format.java
IF NOT ERRORLEVEL 0 ( EXIT /B %ERRORLEVEL% )

ECHO.
ECHO Build Bach with Bach...
ECHO.
java --module-path .bach\bin --add-modules ALL-MODULE-PATH .bach\src\build.java %*
EXIT /B %ERRORLEVEL%
