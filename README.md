# bach - Java Shell Builder
 
[![jdk9](https://img.shields.io/badge/jdk-9-blue.svg)](https://docs.oracle.com/javase/9/index.html)
[![travis](https://travis-ci.org/sormuras/bach.svg?branch=master)](https://travis-ci.org/sormuras/bach)
[![experimental](https://img.shields.io/badge/api-experimental-yellow.svg)](https://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/javadoc/)

Use Java source in JShell to build your modular project.

> No need to be a maven to be able to use a build tool - [forax/pro](https://github.com/forax/pro)

## simple usage

Download a copy of [Bach.java] to your project's root directory and put the
following script in file called `build.jsh`. Edit the source and target paths
to your needs and launch the build with `jshell build.jsh`.

```javascript
/open Bach.java

Bach bach = new Bach.Builder().build()
bach.call("java", "--version")
bach.call("javac", "-d", "target/classes", ...)
bach.call("java", "-ea", "-cp", "target/classes", "BachTests")

/exit
```

## bootstrap on-the-fly

You may want to mark the build script executable and use [bootstrap.jsh] to
automatically download that latest [Bach.java].

```javascript
//$JAVA_HOME/bin/jshell --show-version $0 $@; exit $?

//
// download "Bach.java"
//
Path target = Paths.get("target")
Path source = target.resolve("Bach.java")
if (Files.notExists(source)) {
  Files.createDirectories(target);
  URL url = new URL("https://raw.githubusercontent.com/sormuras/bach/master/src/main/java/Bach.java");
  try (InputStream in = url.openStream()) {
    Files.copy(in, source, StandardCopyOption.REPLACE_EXISTING);
  }
  System.out.printf("created %s [%s]%n", source, url);
}

//
// source "Bach.java" into this jshell environment and build
//
/open target/Bach.java

new Bach.Builder().build().call("java", "--version")

/exit
```

## be free - have fun
[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[Bach.java]: https://github.com/sormuras/bach/blob/master/src/main/java/Bach.java
[bootstrap.jsh]: https://github.com/sormuras/bach/blob/master/bootstrap.jsh
