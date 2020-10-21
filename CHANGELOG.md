# Changelog
All notable changes to [Bach](https://github.com/sormuras/bach) will be documented in this `CHANGELOG.md` file.

The format is based on [Keep a Changelog](https://keepachangelog.com),
and this project adheres to [JEP 223: New Version-String Scheme](https://openjdk.java.net/jeps/223).

## Version [Unreleased]

Nothing notable, yet.

### Added

- Main action `help` printing a help screen.
- Main action `clean` deleting `.bach/workspace` directory.

### Changed

- JShell-based `boot` script now also downloads an initial version of module `com.github.sormuras.bach`

## Version [15-ea+3] - 2020-10-20

Launch programs modified.

### Added

- Java-based launch script `bach.java`

### Changed

- Bash-based launch script `bach`
- DOS-based launch script `bach.bat`

### Removed

- JShell-based scripts `boot.jsh` and `pull.jsh`

## Version [15-ea+2] - 2020-10-15

Build and launch custom build program.

### Added

- Bach's main program - only delegating to custom build programs for the time being
- JShell-based scripts `boot.jsh` and `pull.jsh`

### Changed

- API documentation
- Directory for build-related modules is now called `.bach/cache`

## Version [15-ea+1] - 2020-10-12

Initial pre-release of Bach 15.

### Added

- Module `com.github.sormuras.bach` exporting its only package with the same name
- Package `com.github.sormuras.bach` with generic tool-related classes

[Unreleased]: https://github.com/sormuras/bach/compare/15-ea+3...HEAD
[15-ea+3]: https://github.com/sormuras/bach/releases/tag/15-ea+3
[15-ea+2]: https://github.com/sormuras/bach/releases/tag/15-ea+2
[15-ea+1]: https://github.com/sormuras/bach/releases/tag/15-ea+1
