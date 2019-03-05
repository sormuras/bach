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

import java.nio.file.Files;
import java.nio.file.Path;

/** OS-agnostic build program. */
class Build {

  /** Main entry-point throwing runtime exception on error. */
  public static void main(String... args) throws Exception {
    System.out.println("\nBuilding Bach.java " + Bach.VERSION + "...");
    var build = new Build();
    build.clean();
    build.format();
    build.compile();
    build.test();
    build.document();
    build.jar();
    build.validate();
    build.zip();
  }

  private final Bach bach = new Bach(true, Path.of(""));
  private final Path target = Path.of("target", "build");
  private final Path targetBinMain = target.resolve("bin/main");
  private final Path targetJavadoc = target.resolve("javadoc");
  private final Path targetJars = target.resolve("jars");

  private void clean() {
    System.out.println("\n[clean] // TODO");
  }

  private void format() {
    System.out.println("\n[format] // TODO");
  }

  private void compile() {
    System.out.println("\n[compile]");
    bach.run(0, "javac", "-d", targetBinMain, "src/bach/Bach.java");
  }

  private void test() {
    System.out.println("\n[test - compile] // TODO");
    // TODO bach.run(0, "javac", "-d", targetBinTest, "src/test/BachTests.java", ...);

    System.out.println("\n[test - run] // TODO");
    // TODO Start JUnit Platform ConsoleLauncher
  }

  private void document() throws Exception {
    System.out.println("\n[document]");
    Files.createDirectories(targetJavadoc);
    bach.run(
        0,
        "javadoc",
        "-d",
        targetJavadoc,
        "-package",
        "-quiet",
        "-keywords",
        "-html5",
        "-linksource",
        "-Xdoclint:all,-missing",
        "-link",
        "https://docs.oracle.com/en/java/javase/11/docs/api/",
        "src/bach/Bach.java");
  }

  private void jar() throws Exception {
    System.out.println("\n[jar]");
    Files.createDirectories(targetJars);
    bach.run(
        0,
        "jar",
        "--create",
        "--file",
        targetJars.resolve("bach.jar"),
        "--main-class",
        "Bach",
        "-C",
        targetBinMain,
        ".");
    bach.run(
        0,
        "jar",
        "--create",
        "--file",
        targetJars.resolve("bach-sources.jar"),
        "-C",
        "src/bach",
        ".");
    bach.run(
        0,
        "jar",
        "--create",
        "--file",
        targetJars.resolve("bach-javadoc.jar"),
        "-C",
        targetJavadoc,
        ".");
    // TODO bach.treeWalk(targetJars, System.out::println);
  }

  private void validate() {
    System.out.println("\n[validate - jdeps]");
    bach.run(0, "jdeps", "-summary", "-recursive", targetJars.resolve("bach.jar"));

    System.out.println("\n[validate - java -jar bach.jar banner]");
    bach.run(0, "java", "-jar", targetJars.resolve("bach.jar"), "tool", "javac", "--version");
  }

  private void zip() throws Exception {
    System.out.println("\n[zip]");
    Bach.Util.treeDelete(Path.of("demo/scaffold/.bach"));
    Bach.Util.treeDelete(Path.of("demo/scaffold/bin"));
    bach.run(
        0,
        "jar",
        "--update",
        "--file",
        "demo/scaffold.zip",
        "--no-manifest",
        "-C",
        "demo/scaffold",
        ".");
  }
}
