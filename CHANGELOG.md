# Changelog
All notable changes to [Bach.java](https://github.com/sormuras/bach) will be documented in this `CHANGELOG.md` file.

The format is based on [Keep a Changelog](https://keepachangelog.com),
and this project adheres to [JEP 223: New Version-String Scheme](https://openjdk.java.net/jeps/223).

## Version [11-ea] unreleased

_In progress..._

## Version [11.2] released 2020-06-10

- https://github.com/sormuras/bach/releases/tag/11.2
- https://repo.maven.apache.org/maven2/de/sormuras/bach/de.sormuras.bach/11.2.0.3/

### Breaking Changes
- Replace generated `Bach.java` with module `de.sormuras.bach` [#126]
- Use `@` character as version separator in API documetation JAR file
### New Features And Enhancements
- Add **JLWGL 3.2.3** module mappings https://search.maven.org/search?q=g:org.lwjgl
- Deploy snapshot builds to GitHub Packages https://github.com/sormuras/bach/packages
- Deploy releases to Bintray and publish them to Maven Central https://search.maven.org/search?q=g:de.sormuras.bach
### Bug Fixes
- Fix parsing of `last-modifier` HTTP header when resolving modules

## Version [11.1] released 2020-06-01

https://github.com/sormuras/bach/releases/tag/11.1

- ☕ **Java**, pristine Java
- 🚀 **Zero-installation** build mode
- 📚 **API** documentation
- 💾 Custom runtime **image**
- ✔ Automated Checks: **Test** program and JUnit Platform support
- 🗄 **Multi-Release** modular JAR file support
- 🧩 **3<sup>rd</sup>-party modules** in plain sight
- 📋 Structured build **summary** with history

## Version [2.1] released 2020-02-08

- https://github.com/sormuras/bach/releases/tag/2.1
- https://fosdem.org/2020/schedule/event/bach

## Version [1.9.10] released 2019-10-23

- https://github.com/sormuras/bach/releases/tag/1.9.10

[11-ea]: https://github.com/sormuras/bach/compare/11.2...master
[11.2]: https://github.com/sormuras/bach/compare/11.1...11.2
[11.1]: https://github.com/sormuras/bach/commits/11.1
[2.1]: https://github.com/sormuras/bach/compare/2.0...2.1
[1.9.10]: https://github.com/sormuras/bach/compare/1.9.1...1.9.10
[#126]: https://github.com/sormuras/bach/issues/126
