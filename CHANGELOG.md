# Changelog

All notable changes to [Bach](https://github.com/sormuras/bach) will be documented in this `CHANGELOG.md` file.

The format is based on [Keep a Changelog](https://keepachangelog.com).

This project adheres to [JEP 223: New Version-String Scheme](https://openjdk.java.net/jeps/223).

## Version [17-ea]

### Fixed

- Compilation of targeted sources in multi-module projects fixed. [#209]
- Creation of test JAR files as computed by the "test code space builder" fixed. [#213]
- Missing external modules computation now correctly includes `requires` directives of in-module test modules. [#213]
- Prevent `FindException` when switching JDK version between Bach runs. [#216]
- All system modules are added by default when running tools and tests. [#217]
- Test resources are now packaged into test modules. [#219]

### Added

- Support and auto-detect launcher option values of `jlink`. [#208]
- Add [FXGL](https://almasb.github.io/FXGL) module lookup support [#210]
- Run each test module via their `ToolProvider` services named `test` in addition to launching `junit`. [#215]
- Enable assertions in test space's class loaders by default [#220]

## Version [17-ea-1] released 2021-03-17

Initial Early-Access release.

[17-ea]: https://github.com/sormuras/bach/releases/tag/17-ea

[17-ea-1]: https://github.com/sormuras/bach/compare/17-ea-1...main

[#208]: https://github.com/sormuras/bach/issues/208

[#209]: https://github.com/sormuras/bach/issues/209

[#210]: https://github.com/sormuras/bach/issues/210

[#213]: https://github.com/sormuras/bach/issues/213

[#215]: https://github.com/sormuras/bach/issues/215

[#216]: https://github.com/sormuras/bach/issues/216

[#217]: https://github.com/sormuras/bach/issues/217

[#219]: https://github.com/sormuras/bach/issues/219

[#220]: https://github.com/sormuras/bach/issues/220
