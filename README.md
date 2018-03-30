# bach - Java Shell Builder
 
[![jdk11](https://img.shields.io/badge/jdk-11-blue.svg)](http://jdk.java.net/11/)
[![travis](https://travis-ci.org/sormuras/bach.svg?branch=master)](https://travis-ci.org/sormuras/bach)
[![experimental](https://img.shields.io/badge/api-experimental-yellow.svg)](https://jitpack.io/com/github/sormuras/bach/master-SNAPSHOT/javadoc/)

Use Java source in [jshell] to build your modular project.

> No need to be a maven to be able to use a build tool - [forax/pro](https://github.com/forax/pro)

## simple usage

Download a copy of [Bach.java] to your project's root directory and put the following [jshell] script in file called `build.jsh`.
Launch the build with `jshell build.jsh`.

```javascript
/open Bach.java

new Bach().run("java", "--version")

/exit
```

## make it executable

Want to call just `./build` to launch the build?

Add the following pseudo-shebang as the first line to `build.jsh`:

```bash
//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?
```

Don't forget to mark your build script executable, i.e. `chmod u+x build.jsh`.
See **bootstrap on-the-fly** section below for an example.

## bootstrap on-the-fly
 
Copy and paste the source of [bootstrap.jsh] to automatically download that latest revisions of [Bach.java] and [Bach.jsh].

```javascript
//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/*
 * Download "Bach.java" and "Bach.jsh" from github to local "target" directory.
 */
var target = Files.createDirectories(Paths.get("target"))
var remote = new URL("https://github.com/sormuras/bach/raw/master/src/bach/")
for (var name : Set.of("Bach.java", "Bach.jsh")) {
  var script = target.resolve(name);
  // if (Files.exists(script)) continue; // uncomment to preserve existing files
  try (var stream = new URL(remote, name).openStream()) {
    Files.copy(stream, script, StandardCopyOption.REPLACE_EXISTING);
  }
}

/*
 * Source "Bach.java" and "Bach.jsh" into this jshell session.
 */
/open target/Bach.java
/open target/Bach.jsh

/*
 * Use it!
 */
java("--version")

/exit
```

When [/open URI](https://bugs.openjdk.java.net/browse/JDK-8199912) is implemented in [jshell] 11 or later the bootstrap
commands for loading [Bach.java] boil down to:

`/open https://raw.githubusercontent.com/sormuras/bach/master/src/bach//Bach.java`.

## be free - have fun
[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[jshell]: https://docs.oracle.com/javase/10/tools/jshell.htm
[Bach.java]: https://github.com/sormuras/bach/blob/master/src/bach/Bach.java
[Bach.jsh]: https://github.com/sormuras/bach/blob/master/src/bach/Bach.jsh
[bootstrap.jsh]: https://github.com/sormuras/bach/blob/master/demo/00-bootstrap/bootstrap.jsh
