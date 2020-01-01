# Story of Bach.java

> Hello.
My name is Christian Stein.
Your build tools are overkill.
Prepare to yet another one.

## Intro

I'll briefly refer to the "as-is" state of build tools
and my motivation to write a lightweight Java build tool for modular Java projects.

### Status Quo

There are many build tools available in the wild.

> Who uses shell scripts, Make, Ant, Maven, ...?

The JDK does not provide a build tool out of the box.

> Why is there no `jbuild`?

The JDK provides foundation tools like `javac`, `jar`, and soon `jpackage`.

> What's a minimal **Java** "build tool"?

- Shuffling the [JDK Foundation Tools] in the right order.
- Passing the right arguments to those calls.
- Based on a modular project model.

> Why didn't build tools embrace Java modules as their basic building blocks of the project models in 2017?
> Why are there still build tools in 2020 that actively deny supporting Java modules out of the box?

I guess, we must be glad that `package`s were already introduced in the early days of Java.
Otherwise, the unnamed package would be our sole container of types.

### Enter Bach.java

Ranging from [JDK Foundation Tools], over shell scripts and Apache Ant to multi-language, multi-purpose build tools...

![jdk-and-build-tools-with-bach](img/jdk-and-build-tools-with-bach.svg)

...`Bach.java`'s target is close to platform-specific shell scripts.
For small to mid-size projects it offers build support in platform-agnostic manner.

- Bach.java Demo
- Bach.java's project model

[JDK Foundation Tools]: https://docs.oracle.com/en/java/javase/11/tools/main-tools-create-and-build-applications.html
