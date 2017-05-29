# bach - Java Shell Builder
 
[![jdk9](https://img.shields.io/badge/jdk-9-blue.svg)](https://shields.io)
[![travis](https://travis-ci.org/sormuras/bach.svg?branch=master)](https://travis-ci.org/sormuras/bach)

Use Java source in JShell to build your modular project.

> No need to be a maven to be able to use a build tool - [forax/pro](https://github.com/forax/pro)

# how to use bach

Fast-forward to the :sparkles:**full-featured build mode**:sparkles: to get the
most out of `bach`, including IDE step-by-step build debug support! The
preceding run modes show the hassle-free usage of JShell as a build platform.

The sources under the [demo] directory are based on [jigsaw/quick-start](http://openjdk.java.net/projects/jigsaw/quick-start).

## raw mode
Copy and paste contents of [Bach.java] into an arbitrary file that resides in
your project's root directory, like `./Bach.java`. Scroll down to the end of
the file and uncomment the example build command block - after editing it to
your project's needs. At the command line use `jshell` to launch the build:

    jshell Bach.java


## auto-download mode

Copy and paste contents of [build.jsh] to your project's root directory and edit
the example build command block. Launch the download and build process with:

    jshell build.jsh


## full-featured build mode

In your project root directory, create a `build.jsh` file and also create a
directory `build`. Drop [Bach.java] into the `build` directory and create a
`Build.java` sibling. The tree could look like:

    build/
      Bach.java
      Build.java
    deps/
      com.astro.lib.jar
    source/
      main/java/
        org.foo.bar/
          module-info.java
      test/...
    build.jsh

The content of `build.jsh` is simply:

```javascript
//$JAVA_HOME/bin/jshell --show-version $0 $@; exit $?
/open build/Bach.java
/open build/Build.java
Build.main()
/exit
```

The actual build block is found in `build/Build.java`'s `main` method:

```java
// default package
public class Build {
  public static void main(String... args) throws Exception {
    Bach.builder()
        .name("bar")
        .log(Level.FINE)
        .override(Folder.SOURCE, Paths.get("source"))
        .override(Folder.TARGET, Paths.get("target/bach/foo"))
        .peek(builder -> System.out.printf("%n%s%n%n", builder.name))
      .build()
        .format()
        .compile()
        .run("org.foo.bar", "org.foo.bar.Main");
  }
}
```

With `jshell build.jsh` you still launch the build from the command line as
described above. That retains the ability to run the build on any system with
an installed JDK.

Now include the `build` directory a source folder in your IDE. It should show
the executable 

**:arrow_forward: Run 'Build.main()'**

marker. And also the 

**:bug: Debug 'Build.main()'**

one.


## be free - have fun
[![jsb](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Bachsiegel.svg/220px-Bachsiegel.svg.png)](https://wikipedia.org/wiki/Johann_Sebastian_Bach)

[demo]:      https://github.com/sormuras/bach/tree/master/demo
[Bach.java]: https://github.com/sormuras/bach/blob/master/bach/Bach.java
[build.jsh]:  https://github.com/sormuras/bach/blob/master/build.jsh
