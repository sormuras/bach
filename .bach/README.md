# Directory `.bach`

The `.bach` directory contains all Bach-related assets.

## Directory `.bach/bin`

The `.bach/bin` directory contains scripts and modules used to build modular Java projects.

### Scripts

- `bach`: Launch script for Linux and MacOS
- `bach.bat`: Launch script for Windows

### Modules

- `com.github.sormuras.bach@VERSION.jar`: 🎼 Bach, the Java Shell Builder

## Directory `.bach/src`

The `.bach/src` directory contains sources of the configuration module for this project.

## Directory `.bach/external-modules`

The `.bach/external-modules` directory contains all external modules required to build this project.
An external module is a module that neither is declared by the current project, nor a module that is provided by the Java Runtime.

## Directory `.bach/external-tools`

The `.bach/external-tools` directory contains all external tools required to build this project.

## Directory `.bach/workspace`

The `.bach/workspace` directory is created by Bach to store intermediat and final assets.
Usually, Bach stores reusable modules of library projects in `.bach/workspace/modules`.
Custom runtime images of application projects are stored in `.bach/workspace/image`.
