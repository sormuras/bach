# bach - Java Shell Builder
 
[![experimental](https://img.shields.io/badge/api-experimental-yellow.svg)](http://github.com/badges/stability-badges)
[![jdk9](https://img.shields.io/badge/jdk-9-blue.svg)](https://docs.oracle.com/javase/9/index.html)
[![travis](https://travis-ci.org/sormuras/bach.svg?branch=master)](https://travis-ci.org/sormuras/bach)

Use Java source in JShell to build your modular project.

> No need to be a maven to be able to use a build tool - [forax/pro](https://github.com/forax/pro)

## simple usage

```javascript
/open bach/Bach.java

Bach bach = new Bach()
bach.call("java", "-version")
{
bach.command("javac")
    .addAll("-d", "target/test")
    .markDumpLimit(1)
    .addAllJavaFiles(Paths.get("bach"))
    .addAllJavaFiles(Paths.get("test"))
    .execute();
}
bach.call("java", "-ea", "-cp", "target/test", "BachTests")

bach.format(false, Paths.get("test"), "--skip-sorting-imports")

/exit
```



## be free - have fun
[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[demo]:      https://github.com/sormuras/bach/tree/master/demo
[Bach.java]: https://github.com/sormuras/bach/blob/master/bach/Bach.java
[build.jsh]:  https://github.com/sormuras/bach/blob/master/build.jsh
