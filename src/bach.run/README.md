# Bach's `src/bach.run` folder

This folder contains Java source files (`.java`) and Java Shell scripts (`.jshell`) helping to install Bach's binaries.

## Install default version of Bach

```shell
mkdir example && cd example
jshell
/open https://install.bach.run
```

- https://install.bach.run forwards to the _"install default version of Bach into `.bach` directory of the current working directory"_ [Java Shell](install.jshell) script.

## Path forwarding

- https://src.bach.run forwards to https://raw.githubusercontent.com/sormuras/bach/main/src/bach.run - resulting in `404: Not Found`
- https://src.bach.run/install.jshell forwards to https://raw.githubusercontent.com/sormuras/bach/main/src/bach.run/install.jshell
- https://src.bach.run/BachInstaller.java forwards to https://raw.githubusercontent.com/sormuras/bach/main/src/bach.run/BachInstaller.java
