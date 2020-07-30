/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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

package test.base.jdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.Tree;

class JavacTests {

  @BeforeAll
  static void findJavacAndEmitVersion() {
    var javac = ToolProvider.findFirst("javac").orElseThrow();
    var discard = new PrintWriter(Writer.nullWriter());
    javac.run(discard, discard, "--version");
  }

  @Test
  void testTimestampsOfRemovedClasses(@TempDir Path temp) {
    var javac = ToolProvider.findFirst("javac").orElseThrow();
    // var javap = ToolProvider.findFirst("javap").orElseThrow();

    try {
      Files.createDirectories(temp.resolve("src/a/a"));
      Files.writeString(temp.resolve("src/a/module-info.java"), "module a {}");
      Files.writeString(temp.resolve("src/a/a/A.java"), "package a; class A {}");
      // Tree.walk(temp.resolve("src"), __ -> true, System.out::println);
      javac.run(
          System.out,
          System.err,
          // "-verbose",
          "-d",
          temp.resolve("bin").toString(),
          "-g",
          "--module",
          "a",
          "--module-source-path",
          temp.resolve("src").toString());
      // Tree.walk(temp.resolve("bin"), __ -> true, System.out::println);
      // javap.run(System.out, System.err, temp.resolve("bin/a/a/A.class").toString());

      Files.delete(temp.resolve("src/a/a/A.java"));
      // Files.writeString(temp.resolve("src/a/a/D.java"), "package a; class D {}");
      // Tree.walk(temp.resolve("src"), __ -> true, System.out::println);
      javac.run(
          System.out,
          System.err,
          "-verbose",
          "-d",
          temp.resolve("bin").toString(),
          "--module",
          "a",
          "--module-source-path",
          temp.resolve("src").toString());
      // Tree.walk(temp.resolve("bin"), __ -> true, System.out::println);
    } catch (Throwable throwable) {
      Tree.walk(temp, __ -> true, System.err::println);
      throw new AssertionError(throwable);
    }
  }

  @Test
  void testSeparateJavaAndModuleDirectories(@TempDir Path temp) {
    var root = Path.of("src", "test.base", "test", "resources", "jdk");
    var base = root.resolve("SeparateJavaAndModuleDirectories");
    var javac = ToolProvider.findFirst("javac").orElseThrow();
    var moduleSourcePath =
        String.join(
            File.pathSeparator,
            String.join(File.separator, base.toString(), "*", "java"),
            String.join(File.separator, base.toString(), "*", "module"));
    var args =
        List.of("-d", temp, "--module", "foo", "--module-source-path", moduleSourcePath).stream()
            .map(Object::toString)
            .toArray(String[]::new);
    var code = javac.run(System.out, System.err, args);
    assertEquals(0, code, String.join(System.lineSeparator(), args));
    try {
      // Strings.walk(temp, System.out::println);
      assertTrue(Files.exists(temp.resolve("foo/foo/Foo.class")));
      assertTrue(Files.exists(temp.resolve("foo/module-info.class")));
    } catch (Throwable throwable) {
      Tree.walk(temp, __ -> true, System.err::println);
      throw throwable;
    }
  }

  @Test
  void testMultiRelease(@TempDir Path temp) {
    var root = Path.of("src", "test.base", "test", "resources", "jdk");
    var base = root.resolve("MultiRelease");
    var javac = ToolProvider.findFirst("javac").orElseThrow();
    // initial "compile all" run
    var classes = temp.resolve("classes");
    try {
      var moduleSourcePath =
          String.join(
              File.pathSeparator,
              String.join(File.separator, base.toString(), "*", "java"),
              String.join(File.separator, base.toString(), "*", "java-8"),
              String.join(File.separator, base.toString(), "*", "java-9"));
      var args =
          List.of("-d", classes, "--module", "a,b,c", "--module-source-path", moduleSourcePath)
              .stream()
              .map(Object::toString)
              .toArray(String[]::new);
      var code = javac.run(System.out, System.err, args);
      assertEquals(0, code, String.join(System.lineSeparator(), args));
      assertTrue(Files.exists(classes.resolve("a/module-info.class")));
      assertTrue(Files.exists(classes.resolve("a/a/A.class")));
      assertTrue(Files.exists(classes.resolve("b/module-info.class")));
      assertTrue(Files.exists(classes.resolve("b/b/B.class")));
      assertTrue(Files.exists(classes.resolve("c/module-info.class")));
      assertTrue(Files.exists(classes.resolve("c/c/C.class")));
    } catch (AssertionError e) {
      Tree.walk(classes, __ -> true, System.err::println);
      throw e;
    }
    // for each multi-release module...
    var release = temp.resolve("release");
    try {
      for (int feature = 7; feature <= Runtime.version().feature(); feature++) {
        var javaN = "java-" + feature;
        var source = base.resolve("b").resolve(javaN);
        if (Files.notExists(source)) continue;
        if (feature < 9) {
          var fixture =
              List.of(
                  "--release",
                  feature,
                  "-d",
                  release.resolve(javaN).resolve("b"),
                  "--source-path",
                  source,
                  "--class-path",
                  classes);
          var withSourceFiles = new ArrayList<Object>(fixture);
          try (var stream = Files.walk(source)) {
            stream
                .filter(path -> String.valueOf(path.getFileName()).endsWith(".java"))
                .sorted()
                .forEach(withSourceFiles::add);
          } catch (Exception e) {
            throw new AssertionError(e);
          }
          var args = withSourceFiles.stream().map(Object::toString).toArray(String[]::new);
          var code = javac.run(System.out, System.err, args);
          assertEquals(0, code, String.join(System.lineSeparator(), args));
          continue;
        }
        var moduleSourcePath =
            String.join(
                File.pathSeparator,
                String.join(File.separator, base.toString(), "*", javaN),
                String.join(File.separator, base.toString(), "*", "java-9"));
        var args =
            List.of(
                    "--release",
                    feature,
                    "-d",
                    release.resolve(javaN),
                    "--module",
                    "b",
                    "--module-source-path",
                    moduleSourcePath,
                    "--module-path",
                    classes)
                .stream()
                .map(Object::toString)
                .toArray(String[]::new);
        var code = javac.run(System.out, System.err, args);
        assertEquals(0, code, String.join(System.lineSeparator(), args));
      }
    } catch (AssertionError e) {
      Tree.walk(release, __ -> true, System.err::println);
      throw e;
    }
    // Strings.walk(temp, System.out::println);
    assertEquals(8, Classes.feature(release.resolve("java-8/b/b/B.class")));
    assertEquals(9, Classes.feature(release.resolve("java-9/b/module-info.class")));
    assertEquals(11, Classes.feature(release.resolve("java-11/b/b/B.class")));
  }
}
