/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JdkToolTests {

  @Test
  void javac() {
    var expectedLines =
        List.of(
            "javac",
            "--class-path",
            "  classes",
            "-g",
            "-deprecation",
            "-d",
            "  out",
            "-encoding",
            "  US-ASCII",
            "-Werror",
            "--patch-module",
            "  foo=bar",
            "--module-path",
            "  mods",
            "--module-source-path",
            "  src",
            "--add-modules",
            "  mod.A,ALL-MODULE-PATH,mod.B",
            "-parameters",
            "-verbose",
            ">> many .java files >>");
    var javac = new JdkTool.Javac();
    javac.generateAllDebuggingInformation = true;
    javac.deprecation = true;
    javac.destination = Paths.get("out");
    javac.encoding = StandardCharsets.US_ASCII;
    javac.failOnWarnings = true;
    javac.parameters = true;
    javac.verbose = true;
    javac.classPath = List.of(Paths.get("classes"));
    javac.classSourcePath = List.of(Paths.get("src/build"));
    javac.moduleSourcePath = List.of(Paths.get("src"));
    javac.modulePath = List.of(Paths.get("mods"));
    javac.addModules = List.of("mod.A", "ALL-MODULE-PATH", "mod.B");
    javac.patchModule = Map.of("foo", List.of(Paths.get("bar")));
    assertLinesMatch(expectedLines, dump(javac.toCommand()));
  }

  @Test
  void java() {
    var expectedLines =
        List.of(
            "java",
            "--dry-run",
            "-jar",
            "  application.jar",
            "--patch-module",
            "  com.greetings=xxx",
            "--module-path",
            "  mods",
            "--add-modules",
            "  mod.A,ALL-MODULE-PATH,mod.B",
            "--module",
            "  com.greetings/com.greetings.Main",
            "1",
            "2",
            "NEW");
    var java = new JdkTool.Java();
    java.dryRun = true;
    java.jar = Paths.get("application.jar");
    java.addModules = List.of("mod.A", "ALL-MODULE-PATH", "mod.B");
    java.patchModule = Map.of("com.greetings", List.of(Paths.get("xxx")));
    java.modulePath = List.of(Paths.get("mods"));
    java.module = "com.greetings/com.greetings.Main";
    java.args = List.of(1, "2", Thread.State.NEW);
    assertLinesMatch(expectedLines, dump(java.toCommand()));
  }

  @Test
  void runJavaWithVersion() {
    var bytes = new ByteArrayOutputStream(2000);
    var out = System.out;
    try {
      System.setOut(new PrintStream(bytes));
      var java = new JdkTool.Java();
      java.args = List.of("--version");
      java.run();
      assertTrue(bytes.toString().contains(Runtime.version().toString()));
    } finally {
      System.setOut(out);
    }
  }

  @Nested
  class Javadoc {

    @Test
    void basic() {
      var expectedLines = List.of("javadoc");
      var javadoc = new JdkTool.Javadoc();
      javadoc.quiet = false;
      javadoc.html5 = false;
      javadoc.keywords = false;
      javadoc.doclint = null;
      assertLinesMatch(expectedLines, dump(javadoc.toCommand()));
    }

    @Test
    void defaults() {
      var expectedLines = List.of("javadoc", "-quiet", "-html5", "-keywords", "-Xdoclint");
      assertLinesMatch(expectedLines, dump(new JdkTool.Javadoc().toCommand()));
    }

    @Test
    void customized() {
      var expectedLines =
          List.of(
              "javadoc",
              "-quiet",
              "-html5",
              "-keywords",
              "-link",
              "  one",
              "-link",
              "  two",
              "-linksource",
              "-Xdoclint:all,-missing",
              "--show-members",
              "  private",
              "--show-types",
              "  public");
      var javadoc = new JdkTool.Javadoc();
      javadoc.quiet = true;
      javadoc.html5 = true;
      javadoc.link = List.of("one", "two");
      javadoc.linksource = true;
      javadoc.keywords = true;
      javadoc.doclint = "all,-missing";
      javadoc.showTypes = JdkTool.Javadoc.Visibility.PUBLIC;
      javadoc.showMembers = JdkTool.Javadoc.Visibility.PRIVATE;
      assertLinesMatch(expectedLines, dump(javadoc.toCommand()));
    }

    @Test
    void suppressUnusedWarnings() {
      var command = new Command("suppressor");
      var javadoc = new JdkTool.Javadoc();
      javadoc.doclint(command);
      javadoc.showMembers(command);
      javadoc.showTypes(command);
    }
  }

  @Test
  void jar() {
    var expectedLines =
        List.of(
            "jar",
            "--list",
            "--file",
            "  fleet.jar",
            "--main-class",
            "  uss.Enterprise",
            "--module-version",
            "  1701",
            "--no-compress",
            "--verbose",
            "-C",
            "  classes",
            ".");
    var jar = new JdkTool.Jar();
    jar.mode = "--list";
    jar.file = Paths.get("fleet.jar");
    jar.mainClass = "uss.Enterprise";
    jar.moduleVersion = "1701";
    jar.noCompress = true;
    jar.verbose = true;
    jar.path = Paths.get("classes");
    assertLinesMatch(expectedLines, dump(jar.toCommand()));
  }

  @Test
  void jdeps() {
    var jdeps = new JdkTool.Jdeps();
    jdeps.classpath = List.of(Paths.get("classes"));
    jdeps.jdkInternals = true;
    jdeps.recursive = true;
    jdeps.profile = true;
    jdeps.apionly = true;
    jdeps.summary = true;
    jdeps.verbose = true;
    assertLinesMatch(
        List.of(
            "jdeps",
            "-classpath",
            "  classes",
            "-recursive",
            "--jdk-internals",
            "-profile",
            "-apionly",
            "-summary",
            "-verbose"),
        dump(jdeps.toCommand()));
  }

  @Test
  void jlink() {
    var jlink = new JdkTool.Jlink();
    jlink.modulePath = List.of(Paths.get("mods"));
    jlink.output = Paths.get("target", "image");
    assertLinesMatch(
        List.of(
            "jlink", "--module-path", "  mods", "--output", "  target" + File.separator + "image"),
        dump(jlink.toCommand()));
  }

  private List<String> dump(Command command) {
    var lines = new ArrayList<String>();
    assertSame(command, command.dump(lines::add));
    return lines;
  }
}
