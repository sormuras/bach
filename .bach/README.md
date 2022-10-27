# Directory `.bach`

The `.bach` directory contains all Bach-related assets.

## Directory `.bach/bin`

The `.bach/bin` directory contains files used to bootstrap, initialize, reset, and start Bach.

- `bach`: Start script for Linux and macOS
- `bach.bat`: Start script for Windows
- `bach.java`: Single-file source-code Java program used to bootstrap, initialize, reset, and start Bach

### Modules

- `com.github.sormuras.bach@VERSION.jar`: 🎼 Bach, the Java Shell Builder

## Directory `.bach/src`

The `.bach/src` directory contains sources of the build programs for this project.

## Directory `.bach/external-modules`

The `.bach/external-modules` directory contains all external modules required to build this project.
An external module is a module that neither is declared by this project, nor a module that is provided by the Java Runtime.

## Directory `.bach/external-tool-layers`

The `.bach/external-layers` directory contains external modular tool providers helping to build this project.
Each set of external modular tool providers has its own subdirectory.
The names of the tools are defined by the `java.util.spi.ToolProvider` SPI implementations.

## Directory `.bach/external-tool-programs`

The `.bach/external-tool-programs` directory contains external tool programs helping to build this project.
Each external tool program has its own subdirectory.
The name of the subdirectory defines the name of the tool.
An external tool program is either:

- a single-file source-code program as described by (JEP 330)[https://openjdk.java.net/jeps/330],
- or a single executable JAR file,
- or a custom packaged Java program that is launched by a `java.launch` configuration file.

## Directory `.bach/out`

The `.bach/out` directory is created by Bach to store intermediate and final assets.
By default, Bach stores reusable modules of library projects in `.bach/out/modules`.
Custom runtime images of application projects are stored in `.bach/out/images/<name>`.
