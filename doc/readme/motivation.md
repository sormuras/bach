## Motivation

From Project Jigsaw's Module System Quick-Start Guide to Bach.java - a Java Shell Build tool.

### Quick-Start Guide

Single module


```text
└───com.greetings
    │   module-info.java
    └───com
        └───greetings
                Main.java
```

Call `javac` + `jar`, and `jlink`.

- [Build Jigsaw Quick-Start](BuildJigsawQuickStart.java)

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
│   │       └───com
│   │           └───greetings
│   │                   Main.class
│   ├───image
│   │   │   release
│   │   ├───bin
|   |   [...]
│   │   └───lib
│   └───modules
│           com.greetings.jar
[...]
```

### Quick-Start Guide World

Two modules in main realm, one module in test.

```text
├───com.greetings
│   └───main
│       │   module-info.java
│       └───com
│           └───greetings
│                   Main.java
├───org.astro
│   └───main
│       │   module-info.java
│       └───org
│           └───astro
│                   World.java
└───test.modules
    └───test
        │   module-info.java
        └───test
            └───modules
                    TestProvider.java
```

Call `javac` + `jar`, `javadoc`, and `jlink`.

- [Build Jigsaw Quick-Start World](BuildJigsawQuickStartWorld.java)

```text
[main]
javac    --module=com.greetings,org.astro ...
jar      --file com.greetings.jar ...
jar      --file org.astro.jar
javadoc  --module=com.greetings,org.astro ...
jlink    --add-modules=com.greetings,org.astro ...

[test]
javac    --module=test.modules ...
jar      --file test.modules.jar ...
test(test.modules) 
```

```
├───build
│   ├───api
│   │   ├───com.greetings
│   │   ├───org.astro
│   │   │   └───org
│   │   │       └───astro
[...]
│   ├───classes
│   │   ├───main
│   │   │   ├───com.greetings
[...]
│   │   │   └───org.astro
[...]
│   │   └───test
│   │       └───test.modules
[...]
│   ├───image
[...]
│   └───modules
│       ├───main
│       └───test
[...]
```
