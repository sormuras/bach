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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
  private final Path targetBinTest = target.resolve("bin/test");
  private final Path targetJavadoc = target.resolve("javadoc");
  private final Path targetJars = target.resolve("jars");

  private void clean() throws Exception {
    System.out.println("\n[clean]");

    Bach.Util.treeDelete(targetBinMain);
    Bach.Util.treeDelete(targetBinTest);
    Bach.Util.treeDelete(targetJavadoc);
    Bach.Util.treeDelete(targetJars);
  }

  private void format() throws Exception {
    System.out.println("\n[format]");

    var roots =
        List.of(
            Path.of("demo"),
            Path.of("src", "bach"),
            Path.of("src", "build"),
            Path.of("src", "test"));
    Bach.Tool.format(bach, Boolean.getBoolean("bach.format.replace"), roots);
  }

  private void compile() {
    System.out.println("\n[compile]");
    bach.run(0, "javac", "-d", targetBinMain, "src/bach/Bach.java");
    Util.treeWalk(targetBinMain);
  }

  private void test() throws Exception {
    System.out.println("\n[test - download]");
    var uri = URI.create(bach.get(Bach.Property.TOOL_URI_JUNIT));
    var junit = bach.download(target, uri);

    System.out.println("\n[test - compile]");
    var javac = new ArrayList<>();
    javac.add("-d");
    javac.add(targetBinTest);
    javac.add("--class-path");
    javac.add(Bach.Util.join(targetBinMain, junit));
    javac.addAll(Bach.Util.findJavaFiles(Path.of("src", "test")));
    bach.run(0, "javac", javac.toArray(Object[]::new));
    Util.treeCopy(Path.of("src/test-resources"), targetBinTest);
    Util.treeWalk(targetBinTest);

    System.out.println("\n[test - run]");
    var launcher = new ArrayList<>();
    launcher.add("-ea");
    launcher.add("-Djunit.jupiter.execution.parallel.enabled=true");
    launcher.add("-Djunit.jupiter.execution.parallel.mode.default=concurrent");
    launcher.add("--class-path");
    launcher.add(Bach.Util.join(targetBinTest, targetBinMain, junit));
    launcher.add("org.junit.platform.console.ConsoleLauncher");
    launcher.add("--scan-class-path");
    bach.run(0, "java", launcher.toArray(Object[]::new));
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

    System.out.println("\nArtifacts in " + targetJars);
    Util.treeWalk(targetJars, System.out::println);
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

  static class Util {
    /** Copy all files and directories from source to target directory. */
    static void treeCopy(Path source, Path target) throws Exception {
      treeCopy(source, target, __ -> true);
    }

    /** Copy selected files and directories from source to target directory. */
    static void treeCopy(Path source, Path target, Predicate<Path> filter) throws Exception {
      // debug("treeCopy(source:`%s`, target:`%s`)%n", source, target);
      if (!Files.exists(source)) {
        throw new IllegalArgumentException("source must exist: " + source);
      }
      if (!Files.isDirectory(source)) {
        throw new IllegalArgumentException("source must be a directory: " + source);
      }
      if (Files.exists(target)) {
        if (!Files.isDirectory(target)) {
          throw new IllegalArgumentException("target must be a directory: " + target);
        }
        if (target.equals(source)) {
          return;
        }
        if (target.startsWith(source)) {
          // copy "a/" to "a/b/"...
          throw new IllegalArgumentException("target must not a child of source");
        }
      }
      try (var stream = Files.walk(source).sorted()) {
        int counter = 0;
        var paths = stream.collect(Collectors.toList());
        for (var path : paths) {
          var destination = target.resolve(source.relativize(path));
          if (Files.isDirectory(path)) {
            Files.createDirectories(destination);
            continue;
          }
          if (filter.test(path)) {
            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
            counter++;
          }
        }
        System.out.printf("Copied %d file(s) of %d elements.%n", counter, paths.size());
      }
    }

    /** Walk directory tree structure. */
    static void treeWalk(Path root) {
      treeWalk(root, System.out::println);
    }

    /** Walk directory tree structure. */
    static void treeWalk(Path root, Consumer<String> out) {
      try (var stream = Files.walk(root)) {
        stream
            .map(root::relativize)
            .map(path -> path.toString().replace('\\', '/'))
            .sorted()
            .filter(Predicate.not(String::isEmpty))
            .forEach(out);
      } catch (Exception e) {
        throw new Error("Walking tree failed: " + root, e);
      }
    }
  }
}
