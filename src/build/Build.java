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

  private void compile() {
    System.out.println("\n[compile]");
    bach.run("javac", "-d", targetBinMain, "src/bach/Bach.java");
  }

  private void test() {
    System.out.println("\n[test - compile] // TODO");
    // TODO bach.run("javac", "-d", targetBinTest, "src/test/BachTests.java", ...);

    System.out.println("\n[test - run] // TODO");
    // TODO Start JUnit Platform ConsoleLauncher
  }

  private void document() throws Exception {
    System.out.println("\n[document]");
    Files.createDirectories(targetJavadoc);
    bach.run(
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
        "jar",
        "--create",
        "--file",
        targetJars.resolve("bach.jar"),
        "--main-class",
        "Bach",
        "-C",
        target + "/main",
        ".");
    bach.run(
        "jar",
        "--create",
        "--file",
        targetJars.resolve("bach-sources.jar"),
        "-C",
        "src/bach",
        ".");
    bach.run(
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
    bach.run("jdeps", "-summary", "-recursive", targetJars.resolve("bach.jar"));

    System.out.println("\n[validate - java -jar bach.jar banner]");
    bach.run("java", "-jar", targetJars.resolve("bach.jar"), "tool", "javac", "--version");
  }

  private void zip() {
    System.out.println("[zip]");
    // TODO bach.treeDelete(Path.of("demo/scaffold/.bach"));
    // TODO bach.treeDelete(Path.of("demo/scaffold/bin"));
    bach.run(
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
