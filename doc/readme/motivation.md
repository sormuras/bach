## Motivation

From Project Jigsaw's Module System Quick-Start Guide to Bach.java - a Java Shell Build tool.

### Quick-Start Guide

Single module, using `javac` + `jar`, and `jlink`.

[MotivationJigsawQuickStartGuide](MotivationJigsawQuickStartGuide.java)
```text
javac    --module=com.greetings --module-source-path=doc\project\JigsawQuickStart -d doc\project\JigsawQuickStart\build\classes
jar      --create --file=doc\project\JigsawQuickStart\build\modules\com.greetings.jar -C doc\project\JigsawQuickStart\build\classes\com.greetings .
jlink    --output=doc\project\JigsawQuickStart\build\image --module-path=doc\project\JigsawQuickStart\build\modules --add-modules=com.greetings --launcher=greet=com.greetings/com.greetings.Main
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

