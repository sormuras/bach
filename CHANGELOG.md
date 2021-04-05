# Changelog

All notable changes to [Bach](https://github.com/sormuras/bach) will be documented in this `CHANGELOG.md` file.

The format is based on [Keep a Changelog](https://keepachangelog.com).

This project adheres to [JEP 223: New Version-String Scheme](https://openjdk.java.net/jeps/223).

## Version [17-ea]

_In progress..._

### Added

- Support of `OnTestsFailed` and `OnTestsSuccessful` events added. [#227]

### Changed

- Tentative final API polishing and renaming pass applied to `ProjectInfo` annotation. [#205] Including:
    - Moved all nested annotation types directly into `ProjectInfo`'s body.
    - Introduced `options = @Options` element and annotation.
    - Introduced `main = @MainSpace` element and annotation.
    - Introduced `test = @TestSpace` element and annotation.
    - Introduced `libraries = @Libraries` element and annotation.
- Generator methods of the `Command` interface are now called `with()` and `withAll()` instead of `.add()`
  and `addAll()`. This also affects all implementations, like `Javac`, `Javadoc`, and others. [#205]
- Enum `CodeStyle` renamed from `JavaStyle`. [#205]
- Records `LocalModule(s)` renamed from `ModuleDeclaration(s)`. The new names fit better into the scheme of system
  modules, external modules, and local modules. [#205]

### Fixed

- Download of [Google Java Formatter](https://github.com/google/google-java-format) tool fixed by upgrading it to
  version `1.10.0`. [#223]

## Version [17-ea-2] released 2021-03-31

Bug fixes and minor features.

### Fixed

- Compilation of targeted sources in multi-module projects fixed. [#209]
- Creation of test JAR files as computed by the "test code space builder" fixed. [#213]
- Missing external modules computation now correctly includes `requires` directives of in-module test modules. [#213]
- Prevent `FindException` when switching JDK version between Bach runs. [#216]
- All system modules are added by default when running tools and tests. [#217]
- Prevent potential `NoClassDefFoundError` when running `bach clean build`. [#218]
- Test resources are now packaged into test modules. [#219]

### Added

- Support and auto-detect launcher option values of `jlink`. [#208]
- Add [FXGL](https://almasb.github.io/FXGL) module lookup support [#210]
- Run each test module via their `ToolProvider` services named `test` in addition to launching `junit`. [#215]
- Enable assertions in test space's class loaders by default [#220]

## Version [17-ea-1] released 2021-03-17

Initial Early-Access release.

[17-ea]: https://github.com/sormuras/bach/compare/17-ea-2...17-ea

[17-ea-2]: https://github.com/sormuras/bach/compare/17-ea-1...17-ea-2

[17-ea-1]: https://github.com/sormuras/bach/releases/tag/17-ea-1

[#205]: https://github.com/sormuras/bach/issues/205

[#208]: https://github.com/sormuras/bach/issues/208

[#209]: https://github.com/sormuras/bach/issues/209

[#210]: https://github.com/sormuras/bach/issues/210

[#213]: https://github.com/sormuras/bach/issues/213

[#215]: https://github.com/sormuras/bach/issues/215

[#216]: https://github.com/sormuras/bach/issues/216

[#217]: https://github.com/sormuras/bach/issues/217

[#218]: https://github.com/sormuras/bach/issues/218

[#219]: https://github.com/sormuras/bach/issues/219

[#220]: https://github.com/sormuras/bach/issues/220

[#223]: https://github.com/sormuras/bach/issues/223

[#227]: https://github.com/sormuras/bach/issues/227
