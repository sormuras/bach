## Motivation

From Project Jigsaw's Module System Quick-Start Guide to Bach.java - a Java Shell Build tool.

### Quick-Start Guide

Single module, using `javac` + `jar`, and `jlink`.

- [Build Jigsaw Quick-Start Guide](BuildJigsawQuickStart.java)

```text
javac --module com.greetings ...
  jar --file com.greetings.jar ...
jlink --add-modules com.greetings ...
```

```
├───build
│   ├───classes
│   │   └───com.greetings
│   │       │   module-info.class
│   │       │
│   │       └───com
│   │           └───greetings
│   │                   Main.class
│   ├───image
│   │   │   release
│   │   ├───bin
|   |   ...
│   │   └───lib
│   └───modules
│           com.greetings.jar
│
└───com.greetings
    │   module-info.java
    │
    └───com
        └───greetings
                Main.java
```

### Quick-Start Guide World

Two main modules, one test module. Using `javac` + `jar`, `javadoc`, and `jlink`.

