# Project Processing Code

https://openjdk.java.net/groups/compiler/processing-code.html

## Running ShowDoclet

Running `ShowDoclet` is **not** implemented, yet.

```shell
jdk/bin/javadoc \
    -docletpath doclet-classes \
    -doclet showcode.ShowDoclet \
    -sourcepath src \
    showcode
```

## Running ShowProcessor

```java
  provides javax.annotation.processing.Processor with showcode.ShowProcessor;
```

```java
  @Tweak(tool = "javac", option = "--processor-module-path", value = ".bach/workspace/modules"),
```

## Running ShowPlugin

```java
  provides com.sun.source.util.Plugin with showcode.ShowPlugin;
```

```java
  @Tweak(tool = "javac", option = "--processor-module-path", value = ".bach/workspace/modules"),
  @Tweak(tool = "javac", option = "-Xplugin:showPlugin"),
```
