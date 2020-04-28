## Motivation

From Project Jigsaw's Module System Quick-Start Guide to Bach.java - a Java Shell Build tool.

### Quick-Start Guide

Single module project setup.

```text
└───com.greetings
    │   module-info.java          --> | module com.greetings {}
    └───com
        └───greetings
                Main.java
```

Call `javac` + `jar` and `jlink`.

- [Build Jigsaw Quick-Start](BuildJigsawQuickStart.java)

```text
javac --module com.greetings ...
  jar --file com.greetings.jar ...
jlink --add-modules com.greetings ...
```

```
├───build
│   ├───classes                       | `javac`'s output directory
│   │   └───com.greetings
│   │       │   module-info.class
│   │       └───com
│   │           └───greetings
│   │                   Main.class
|   |
│   ├───image                         | `jlink`'s output directory
│   │   │   release                   | Describes this image: `MODULES="java.base com.greetings"`
│   │   ├───bin                       | Contains `greet[.bat]` launcher
|   |   :                             |
│   │   └───lib                       | Contains linked `modules` file
|   |
│   └───modules                       | `jar`'s output directory
│           com.greetings.jar         | Modular JAR file
|
└───com.greetings
    │   module-info.java
    └───com
        └───greetings
                Main.java
```

### Quick-Start Guide World

Three distinct module names.
Two modules in main realm, two modules in test realm.

```text
├───com.greetings
│   └───main
│       │   module-info.java      --> | module com.greetings {
│       └───com                       |   requires org.astro;
│           └───greetings             | }
│                   Main.java
│
├───org.astro
│   ├───main
│   │   │   module-info.java      --> | module org.astro {   → ─┐ copy module name
│   │   └───org                       |   exports org.astro; → ─│─────────┐ 
│   │       └───astro                 | }                       │         │ copy relevant
│   │               World.java                                  │         │ directives from
│   └───test                                                 ┌──┴────┐    │ main module
│       │   module-info.java      --> | open /*test*/ module org.astro {  │
│       └───org                       |   exports org.astro;    ├─────────┘
│           └───astro                 |   provides ...ToolProvider with org.astro.TestProvider;
│                   TestProvider.java | }
│
└───test.modules
    └───test
        │   module-info.java      --> | open /*test*/ module test.modules {
        └───test                      |   requires com.greetings;
            └───modules               |   requires org.astro;
                    TestProvider.java |   provides ...ToolProvider with test.modules.TestProvider;
                                      | }
```

Call `javac` + `jar`, `javadoc`, and `jlink`.

- [Build Jigsaw Quick-Start World](BuildJigsawQuickStartWorld.java)

```text
[main]
javac    --module=com.greetings,org.astro ...
jar      --file main/com.greetings.jar ...
jar      --file main/org.astro.jar
javadoc  --module=com.greetings,org.astro ...
jlink    --add-modules=com.greetings,org.astro ...

[test]
javac    --module=test.modules,org.astro ...
jar      --file test/test.modules.jar ...
jar      --file test/org.astro.jar ...
test(test.modules)
test(org.astro)
```

```
├───build
│   ├───api
│   │       index.html
|   |
│   ├───classes
│   │   ├───main
│   │   │   ├───com.greetings
│   │   │   └───org.astro
│   │   └───test
│   │       ├───org.astro
│   │       └───test.modules
|   |
│   ├───image
|   |
│   └───modules
│       ├───main
│       │       com.greetings.jar
│       │       org.astro.jar
│       └───test
│               org.astro.jar
│               test.modules.jar
|
├───com.greetings
├───org.astro
└───test.modules
```
