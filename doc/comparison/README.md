# Build Tool Comparison

## Minimal

Single module project with sole `module-info.java` compilation unit:

```java
module de.sormuras.bach.doc.minimal {}
```

The goal of each build tool, is to generate 3 JAR files:

- Java module
- `-sources.jar`
- `-javadoc.jar`

All build tools are able to generate those 3 required JAR files.
See https://github.com/sormuras/bach/runs/375415188 for details and their build logs.

Here's a summary of the most interesting numbers.

| Tool           | Project Files | Raw Build Time | Total Time | Tool Files | Tool Size in MB |
|----------------| ------------- | -------------- | ---------- | ---------- | --------------- |
| Bach.java 2.0  |             1 |             2  |         10 |          1 |             0.1 |
| Maven 3.6.2    |             2 |            18  |         34 |       1400 |            33   |
| Gradle 6.0.1   |             7 |            43  |         44 |        447 |           330   |

> Note: the difference in the number of "Project Files" between Maven and Gradle stems from the
> fact that the Maven-based project does not include the Maven Wrapper assets, i.e. the project
> is build with the provided Maven installation of GitHub Actions. That is also a reason why the
> "Total Time" of the Gradle variant is roughly 10 seconds slower.

### Bach.java

```text
└───src
    └───de.sormuras.bach.doc.minimal
            module-info.java
```

1 file, the module compilation unit, weighing in 12,327 bytes before build.

`jshell https://bit.ly/bach-build`

10 seconds later (including 1.6 s build time), the directory contains 1,936,285 bytes.

### Maven

```text
│   pom.xml
│
└───src
    └───main
        └───java
                module-info.java
```

2 files, the module compilation unit and a [`pom.xml`](minimal/maven/pom.xml) file, weighing in 18,729 bytes before build.

`mvn verify`

34 seconds later (including 17.7 s build time), the directory contains 1,825,166 bytes.

Plus the Maven 3.6.2 binary installation with 68 files of 10,748,217 bytes. 

In addition, `~/.m2` was created. That directory contains 1309 files with 23,419,543 bytes.

### Gradle

```text
│   build.gradle.kts
│   gradlew
│   gradlew.bat
│   settings.gradle.kts
│
├───gradle
│   └───wrapper
│           gradle-wrapper.jar
│           gradle-wrapper.properties
│
└───src
    └───main
        └───java
                module-info.java
```

7 files weighing in 92,998 bytes before build.

`./gradlew build`

44 seconds later (43 s build time), the directory contains 2,056,634 bytes.

In addition, `~/.gradle` was created. That directory contains 447 files with 330,233,808 bytes.

Local `.gradle` directory containing hashes, caches, properties, etc, ...  was ignored.
