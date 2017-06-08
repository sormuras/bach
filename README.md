# bach - Java Shell Builder
 
[![jdk9](https://img.shields.io/badge/jdk-9-blue.svg)](https://docs.oracle.com/javase/9/index.html)
[![travis](https://travis-ci.org/sormuras/bach.svg?branch=master)](https://travis-ci.org/sormuras/bach)
[![experimental](https://img.shields.io/badge/api-experimental-yellow.svg)](http://github.com/badges/stability-badges)

Use Java source in JShell to build your modular project.

> No need to be a maven to be able to use a build tool - [forax/pro](https://github.com/forax/pro)

## simple usage

Download a copy of [Bach.java] to your project's root directory and put the
following script in file called `build.jsh`. Edit the source and target paths
to your needs and launch the build with `jshell build.jsh`

```javascript
/open Bach.java

Bach bach = new Bach()
bach.call("java", "-version")
{
bach.command("javac")
    .addAll("-d", "target/test")
    .addAllJavaFiles(Paths.get("bach"))
    .addAllJavaFiles(Paths.get("test"))
    .execute();
}
bach.call("java", "-ea", "-cp", "target/test", "BachTests")

/exit
```

## bootstrap on-the-fly

You may want to mark `build.jsh` as executable and use the following bootstrap
lines to automatically download that latest [Bach.java] -- and use it via
`./build.jsh`

```javascript
//$JAVA_HOME/bin/jshell --show-version $0 $@; exit $?

Path bachJava = Paths.get("target/Bach.java")
if (Files.notExists(bachJava)) {
  URL bachURL = new URL("https://raw.githubusercontent.com/sormuras/bach/master/bach/Bach.java");
  Files.createDirectories(bachJava.getParent());
  try (InputStream in = bachURL.openStream()) {
    Files.copy(in, bachJava, StandardCopyOption.REPLACE_EXISTING);
  }
  System.out.printf("created %s [url=%s]%n", bachJava, bachURL);
}
/open target/Bach.java

// place your build commands here
Bach bach = new Bach()
bach.call("java", "-version")
```

## be free - have fun
[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[demo]:      https://github.com/sormuras/bach/tree/master/demo
[Bach.java]: https://github.com/sormuras/bach/blob/master/bach/Bach.java
[build.jsh]:  https://github.com/sormuras/bach/blob/master/build.jsh
