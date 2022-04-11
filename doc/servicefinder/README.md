# ServiceFinder

## Summary

Extend the ServiceLoader and ToolProvider SPI by an API working with both programmatically.

## Goals

- Introduce generic interface `ServiceFinder<SERVICE>` to `java.util.spi` package.
- Add a `ToolFinder` API implementation that `extends ServiceFinder<ToolProvider>`.

## Non-Goals

_TBD._

## Motivation

The ServiceLoader SPI was introduced in Java 6.
The Module System introduced in Java 9 leverages the ServiceLoader SPI via `uses` and `provides` directives.

There's no API that enables users to work with both programmatically in an elegant way.
Let's introduce such an API: let `ToolFinder` be to `ToolProvider` what `ModuleFinder` is to `ModuleReference`.

The Java launcher supports `java --list-modules` to list observable modules.

The Java launcher should also support `java --list-services SERVICE-TYPE` to list loadable services of the specified service type.
The Java launcher should also support `java --list-tools` as short-cut for `java --list-services java.util.spi.ToolProvider`.

## Description

_TBD._

### Functionality

```java
class ListSystemTools {
    public static void main(String... args) {
        ToolFinder.ofSystem().findAll().stream()
            .sorted(Comparator.comparing(ToolProvider::name))
            .forEach(System.out::println);
    }
}
```

```java
class ComposeToolFinders {
    public static void main(String... args) {
        var finder = ToolFinder.compose(
            ToolFinder.of(new CustomToolProvider(), new MoreToolProvider()),
            ToolFinder.of(ModuleFinder.of(Path.of("mods"))),
            ToolFinder.ofSystem(),
            ToolFinder.ofNativeTools(Path.of(System.getProperty("java.home"), "bin")));
        // use tool finder
    }
}
```

## Alternatives

Do nothing.
Leave it to users to design and implement an API to work with the ServiceLoader and ToolProvider SPI.

Focus on extending support for `ToolProviders`.
Only implement the `ToolFinder` API; do not extend a generic `ServerFinder<SERVICE>` SPI.

Introduce an entire JDK tooling related module: `jdk.tool`
Module `jdk.tool` could host the API to work with the ServiceLoader and ToolProvider SPI.
It may also provide more tools that help users in a platform-agnostic manner.
Like `tree` for rendering directory and file hierarchies,
Or `files --create-directories PATH` and `files --delete-directories PATH` as wrappers for common `Files`-related functionality.

## Testing

_TBD._

## Dependences

- `ToolProvider` SPI in `java.base/java.util.spi`
- `ModuleFinder` API in `java.base/java.lang.module`