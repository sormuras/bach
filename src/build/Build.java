/*
 * Bach - Java Shell Builder
 * Copyright (C) 2018 Christian Stein
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

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
class Build {

  public static void main(String... args) {
    try {
      new Build().build();
    } catch (Throwable throwable) {
      System.err.println("build failed due to: " + throwable);
      throwable.printStackTrace();
      System.exit(1);
    }
  }

  Path TOOLS = Paths.get(".bach", "tools");
  Path MAVEN = Paths.get(".bach", "resolved");
  Path SOURCE_BACH = Paths.get("src", "bach");
  Path SOURCE_TEST = Paths.get("src", "test");
  Path TARGET = Paths.get("target", "build");
  Path TARGET_MAIN = TARGET.resolve("classes/main");
  Path TARGET_TEST = TARGET.resolve("classes/test");
  Path JAVADOC = TARGET.resolve("javadoc");
  Path ARTIFACTS = TARGET.resolve("artifacts");
  Path BACH_JAVA = SOURCE_BACH.resolve("Bach.java");

  String BARTHOLDY = "0.2.1";
  String JUNIT_JUPITER = "5.3.1";
  String JUNIT_PLATFORM = "1.3.1";
  String OPENTEST4J = "1.1.0";
  String API_GUARDIAN = "1.0.0";

  final Bach bach = new Bach();

  void build() throws Exception {
    includes();
    format();
    clean();
    compile();
    test();
    javadoc();
    jar();
    jdeps();
  }

  void includes() throws Exception {
    // var uri = URI.create("https://github.com/sormuras/bartholdy/archive/master.zip");
    var uri = URI.create("https://github.com/sormuras/bartholdy/archive/v" + BARTHOLDY + ".zip");
    var zip = download(uri, TARGET.resolve("downloads"), "bartholdy-" + BARTHOLDY + ".zip");
    var builder = new ArrayList<String>();
    builder.add("import static java.lang.System.Logger.Level.DEBUG;");
    builder.add("import static java.util.Objects.requireNonNull;");
    builder.add("");
    builder.add("import java.io.*;");
    builder.add("import java.lang.annotation.*;");
    builder.add("import java.lang.reflect.*;");
    builder.add("import java.net.*;");
    builder.add("import java.nio.channels.*;");
    builder.add("import java.nio.file.*;");
    builder.add("import java.time.*;");
    builder.add("import java.util.*;");
    builder.add("import java.util.concurrent.*;");
    builder.add("import java.util.function.*;");
    builder.add("import java.util.spi.*;");
    builder.add("import java.util.stream.*;");
    builder.add("");
    builder.add("interface Bach2 {");
    builder.add("");
    builder.add("interface Shadow {");
    try (var zipFileSystem = FileSystems.newFileSystem(zip, null)) {
      try (var stream =
          Files.find(
              zipFileSystem.getPath("/"),
              9,
              (path, attributes) ->
                  attributes.isRegularFile()
                      && path.toString().contains("src/main/java")
                      && !path.getFileName().toString().contains("-")
                      && path.toString().endsWith(".java"))) {
        var files = stream.sorted(Path::compareTo).collect(Collectors.toList());
        var insideMultilineComment = false;
        for (var file : files) {
          if (file.toString().contains("bartholdy/tool")) {
            if (!file.getFileName().toString().endsWith("AbstractTool.java")
                && !file.getFileName().toString().endsWith("Java.java")) {
              System.out.printf("[includes] [-] %s%n", file);
              continue;
            }
          }
          if (file.toString().contains("bartholdy/util")) {
            System.out.printf("[includes] [-] %s%n", file);
            continue;
          }
          System.out.printf("[includes] [+] %s%n", file);
          var lines = Files.readAllLines(file);
          // builder.add("// " + file);
          for (var line : lines) {
            var trim = line.trim();
            if (insideMultilineComment) {
              if (trim.endsWith("*/")) {
                insideMultilineComment = false;
              }
              continue;
            }
            if (line.startsWith("package ")) {
              continue;
            }
            if (line.startsWith("import ")) {
              continue;
            }
            if (trim.startsWith("//")) {
              continue;
            }
            if (trim.startsWith("/*")) {
              if (trim.endsWith("*/")) {
                continue;
              }
              insideMultilineComment = true;
              continue;
            }
            if (trim.startsWith("public class ")
                || trim.startsWith("public interface ")
                || trim.startsWith("public abstract class ")
                || trim.startsWith("public final class ")) {
              line = line.replace("public ", "");
            }
            builder.add(line);
          }
        }
      }
    }
    builder.add("}");
    builder.add("}");
    Files.write(Path.of("src", "bach2", "Bach2.java"), builder);
  }

  Path maven(String group, String artifact, String version) throws Exception {
    return maven(group, artifact, version, "");
  }

  Path maven(String group, String artifact, String version, String classifier) throws Exception {
    if (!classifier.isEmpty() && !classifier.startsWith("-")) {
      classifier = "-" + classifier;
    }
    var repo = "https://repo1.maven.org/maven2";
    var file = artifact + "-" + version + classifier + ".jar";
    var uri = URI.create(String.join("/", repo, group.replace('.', '/'), artifact, version, file));
    return download(uri, MAVEN, file);
  }

  /** Download the resource from URI to the target directory using the provided file name. */
  Path download(URI uri, Path directory, String fileName) throws Exception {
    return bach.util.download(uri, directory, fileName);
  }

  void format() throws Exception {
    System.out.printf("%n[format]%n%n");
    /*
    String repo = "https://jitpack.io";
    String user = "com/github/google";
    String name = "google-java-format";
    String version = "master-SNAPSHOT";
    String file = name + "-" + version + "-all-deps.jar";
    URI uri = URI.create(String.join("/", repo, user, name, name, version, file));
    */
    var version = "1.6";
    var base = "https://github.com/google/";
    var name = "google-java-format";
    var file = name + "-" + version + "-all-deps.jar";
    var uri = URI.create(base + name + "/releases/download/" + name + "-" + version + "/" + file);
    var jar = download(uri, TOOLS.resolve(name), file);
    var java = new JdkTool.Java();
    java.jar = jar;
    java.toCommand(bach).add("--version").run();
    // format
    var format = java.toCommand(bach);
    if (Boolean.getBoolean("bach.format.replace")) {
      format.add("--replace");
    } else {
      format.add("--dry-run");
      format.add("--set-exit-if-changed");
    }
    format.mark(5);
    format.addAllJavaFiles(List.of(Paths.get("src"), Paths.get("demo")));
    format.run();
  }

  void clean() {
    System.out.printf("%n[clean]%n%n");
    if (Files.exists(TARGET)) {
      bach.util.removeTree(TARGET);
    }
    System.out.println("deleted " + TARGET);
  }

  void compile() throws Exception {
    System.out.printf("%n[compile]%n%n");

    // main
    var javac = new JdkTool.Javac();
    javac.generateAllDebuggingInformation = true;
    javac.destination = TARGET_MAIN;
    javac.classSourcePath = List.of(SOURCE_BACH);
    bach.run(javac);

    // test
    javac.destination = TARGET_TEST;
    javac.classSourcePath = List.of(SOURCE_TEST);
    javac.classPath =
        List.of(
            TARGET_MAIN,
            maven("org.junit.jupiter", "junit-jupiter-api", JUNIT_JUPITER),
            maven("org.junit.jupiter", "junit-jupiter-params", JUNIT_JUPITER),
            maven("org.junit.platform", "junit-platform-commons", JUNIT_PLATFORM),
            maven("org.apiguardian", "apiguardian-api", API_GUARDIAN),
            maven("org.opentest4j", "opentest4j", OPENTEST4J));
    bach.run(javac);
    bach.util.copyTree(Paths.get("src/test-resources"), TARGET_TEST);
  }

  void javadoc() throws Exception {
    System.out.printf("%n[javadoc]%n%n");

    Files.createDirectories(JAVADOC);
    var javadoc = new JdkTool.Javadoc();
    javadoc.destination = JAVADOC;
    javadoc.doclint = "all,-missing";
    javadoc.link = List.of("https://docs.oracle.com/en/java/javase/11/docs/api/");
    javadoc.linksource = true;
    javadoc.showTypes = JdkTool.Javadoc.Visibility.PACKAGE;
    javadoc.showMembers = JdkTool.Javadoc.Visibility.PACKAGE;
    javadoc.toCommand(bach).add(BACH_JAVA).run();
  }

  void jar() throws Exception {
    System.out.printf("%n[jar]%n%n");

    Files.createDirectories(ARTIFACTS);
    jar("bach.jar", TARGET_MAIN);
    jar("bach-sources.jar", SOURCE_BACH);
    jar("bach-javadoc.jar", JAVADOC);
  }

  void jar(String artifact, Path path) {
    var jar = new JdkTool.Jar();
    jar.file = ARTIFACTS.resolve(artifact);
    jar.path = path;
    bach.run(jar);
  }

  void jdeps() {
    System.out.printf("%n[jdeps]%n%n");

    var jdeps = new JdkTool.Jdeps();
    jdeps.summary = true;
    jdeps.recursive = true;
    jdeps.toCommand(bach).add(ARTIFACTS.resolve("bach.jar")).run();
  }

  void test() throws Exception {
    System.out.printf("%n[test]%n%n");

    var name = "junit-platform-console-standalone";
    var repo = "https://repo1.maven.org/maven2";
    var user = "org/junit/platform";
    var file = name + "-" + JUNIT_PLATFORM + ".jar";
    var uri = URI.create(String.join("/", repo, user, name, JUNIT_PLATFORM, file));
    var jar = download(uri, TOOLS.resolve(name), file);

    var java = bach.command("java");
    java.add("-ea");
    java.add("-Dbach.offline=" + System.getProperty("bach.offline", "false"));
    java.add("-Djunit.jupiter.execution.parallel.enabled=true");
    java.add("-jar").add(jar);
    java.add("--class-path").add(TARGET_TEST);
    java.add("--class-path").add(TARGET_MAIN);
    java.add("--scan-classpath");
    java.run();
  }
}
