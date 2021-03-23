# Changelog

All notable changes to [Bach](https://github.com/sormuras/bach) will be documented in this `CHANGELOG.md` file.

The format is based on [Keep a Changelog](https://keepachangelog.com).

This project adheres to [JEP 223: New Version-String Scheme](https://openjdk.java.net/jeps/223).

## Version [17-ea]

### Fixed

- Compilation of targeted sources in multi-module projects fixed. [#209]
- Creation of test JAR files as computed by the "test code space builder" fixed. [#213]
- Missing external modules computation now correctly includes `requires` directives of in-module test modules. [#213]

### Added

- Support and auto-detect launcher option values of `jlink`. [#208]
- Run each test module via their `ToolProvider` services named `test` in addition to launching `junit`. [#215]

## Version [17-ea-1] released 2021-03-17

Initial Early-Access release.

[17-ea]: https://github.com/sormuras/bach/releases/tag/17-ea

[17-ea-1]: https://github.com/sormuras/bach/compare/17-ea-1...main

[#208]: https://github.com/sormuras/bach/issues/208

[#209]: https://github.com/sormuras/bach/issues/209

[#213]: https://github.com/sormuras/bach/issues/213

[#215]: https://github.com/sormuras/bach/issues/215
