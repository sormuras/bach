/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// default package

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class Make {

  public static void main(String... args) throws Exception {
    var bach = new Bach();
    bach.log.info("Make!");

    var SOURCE_BACH = Path.of("src", "bach");
    var SOURCE_TEST = Path.of("src", "test");
    var SOURCE_TEST_RESOURCES = Path.of("src", "test-resources");
    var TARGET = Path.of("target", "build");
    var TARGET_MAIN = TARGET.resolve("classes/main");
    var TARGET_TEST = TARGET.resolve("classes/test");
    var JAVADOC = TARGET.resolve("javadoc");
    var ARTIFACTS = TARGET.resolve("artifacts");
    var BACH_JAVA = SOURCE_BACH.resolve("Bach.java");

    bach.log.info("Format");
    var format = new Tool.GoogleJavaFormat(true, List.of(Path.of("src"), Path.of("demo")));
    format.run(bach);

    bach.log.info("Clean");
    if (Files.exists(TARGET)) Util.removeTree(TARGET);

    bach.log.info("Compile Bach.java");
    var main = new Command("javac");
    main.add("-g");
    main.add("-d").add(TARGET_MAIN);
    main.add("--source-path").add(SOURCE_BACH);
    main.mark(10);
    main.addAllJavaFiles(List.of(SOURCE_BACH));
    main.run(bach);

    bach.log.info("Compile tests");
    var test = new Command("javac");
    test.add("-g");
    test.add("-d").add(TARGET_TEST);
    test.add("--source-path").add(SOURCE_TEST);
    test.add("--class-path").add(List.of(TARGET_MAIN, Tool.JUnit.install(bach)));
    test.mark(10);
    test.addAllJavaFiles(List.of(SOURCE_TEST));
    test.run(bach);

    bach.log.info("Launch JUnit Platform Console");
    new Tool.JUnit(
            List.of(
                "--class-path",
                TARGET_TEST,
                "--class-path",
                SOURCE_TEST_RESOURCES,
                "--class-path",
                TARGET_MAIN,
                "--scan-class-path"))
        .run(bach);

    bach.log.info("Generate javadoc");
    Files.createDirectories(JAVADOC);
    var javadoc = new Command("javadoc");
    javadoc.add("-d").add(JAVADOC);
    javadoc.add("-package");
    javadoc.add("-quiet");
    javadoc.add("-keywords");
    javadoc.add("-html5");
    javadoc.add("-linksource");
    javadoc.add("-Xdoclint:all,-missing");
    javadoc.add("-link").add("https://docs.oracle.com/en/java/javase/11/docs/api/");
    javadoc.add(BACH_JAVA);
    javadoc.run(bach);

    bach.log.info("Package");
    Files.createDirectories(ARTIFACTS);
    new Command("jar")
        .add("--create")
        .add("--file")
        .add(ARTIFACTS.resolve("bach.jar"))
        .add("-C")
        .add(TARGET_MAIN)
        .add(".")
        .run(bach);
    new Command("jar")
        .add("--create")
        .add("--file")
        .add(ARTIFACTS.resolve("bach-sources.jar"))
        .add("-C")
        .add(SOURCE_BACH)
        .add(".")
        .run(bach);
    new Command("jar")
        .add("--create")
        .add("--file")
        .add(ARTIFACTS.resolve("bach-javadoc.jar"))
        .add("-C")
        .add(JAVADOC)
        .add(".")
        .run(bach);

    bach.log.info("JDeps");
    new Command("jdeps")
        .add("-summary")
        .add("-recursive")
        .add(ARTIFACTS.resolve("bach.jar"))
        .run(bach);
  }
}
