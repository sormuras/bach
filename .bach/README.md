# Directory `.bach`

The `.bach` directory contains all Bach-related assets.

## Directory `.bach/bin`

The `.bach/bin` directory contains scripts and modules used to build modular Java projects.

### Scripts

- `bach`: Launch script for Linux and MacOS
- `bach.bat`: Launch script for Windows
- `boot.java`: Bach's boot API and JShell overlay used in `boot.jsh`
- `boot.jsh`: Bach's boot script usable as a JShell load-file
- `bootstrap.java`: Bach's platform-agnostic build program used by GitHub Actions workflows
- `init.java`: Bach's init program used by launcher scripts and in `init.jsh`
- `init.jsh`: Bach's init script usable as a JShell load-file

### Modules

- `com.github.sormuras.bach@VERSION.jar`: 🎼 Bach, the Java Shell Builder

## Directory `.bach/bach.info`

The `.bach/bach.info` directory contains sources of the configuration module for this project.
If this directory does not exist, Bach is running in "zero-configuration" mode.

## Directory `.bach/external-modules`

The `.bach/external-modules` directory contains all external modules required to build this project.
An external module is a module that neither is declared by the current project, nor a module that is provided by the Java Runtime.

## Directory `.bach/external-tools`

The `.bach/external-tools` directory contains all external tools required to build this project.

## Directory `.bach/workspace`

The `.bach/workspace` directory is created by Bach to store intermediat and final assets.
Usually, Bach stores reusable modules of library projects in `.bach/workspace/modules`.
Custom runtime images of application projects are stored in `.bach/workspace/image`.
