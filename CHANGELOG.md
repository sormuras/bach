# Changelog
All notable changes to [Bach.java](https://github.com/sormuras/bach) will be documented in this `CHANGELOG.md` file.

The format is based on [Keep a Changelog](https://keepachangelog.com),
and this project adheres to [JEP 223: New Version-String Scheme](https://openjdk.java.net/jeps/223).

## Version [11-ea] unreleased

### Added
- ☕ **Java**, pristine Java

    > JDK 11+ required.
    Write custom build programs in Java (no XML, YML, Z...).
    Java modules declarations define dependencies.
    Missing 3<sup>rd</sup>-party modules are resolved recursively.

- 🚀 **Zero-installation** build mode

    > `jshell https://sormuras.de/bach-build` ↔ [bach-build.jsh](src/bach/bach-build.jsh)

- 📚 **API** documentation

    > Calls `javadoc` with the right arguments.
    Find generated HTML pages in `bach/workspace/api/`.

- 💾 Custom runtime **image**

    > Calls `jlink` with the right arguments.
    Find binary assets in `bach/workspace/image/`.

- 📋 Structured build **summary** with history
    > Stores latest summary in `.bach/workspace/summary.md`.
    Find a history of summary file in `.bach/workspace/summaries/`.

### TODO

- ✔ Automated Checks: `ToolProvider` + JUnit Platform
- More main modes: build, clean, info...
- 🚧 Scaffold sample projects via shell script `bach-boot.jsh`
- 🧩 Locator looking up module-to-maven mappings from https://github.com/sormuras/modules

## Version [2.1] released 2020-02-08

- https://github.com/sormuras/bach/releases/tag/2.1
- https://fosdem.org/2020/schedule/event/bach

## Version [1.9.10] released 2019-10-23

- https://github.com/sormuras/bach/releases/tag/1.9.10

[11-ea]: https://github.com/sormuras/bach/commits/master
[2.1]: https://github.com/sormuras/bach/compare/2.0...2.1
[1.9.10]: https://github.com/sormuras/bach/compare/1.9.1...1.9.10