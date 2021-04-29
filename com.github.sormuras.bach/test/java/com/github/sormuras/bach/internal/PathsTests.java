package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PathsTests {

  @Test
  void findNameOrElse() {
    var path = Path.of("a/b/c/d/module-info.java");
    assertEquals(Path.of("a"), Paths.findNameOrElse(path, "a", null));
    assertEquals(Path.of("a/b"), Paths.findNameOrElse(path, "b", null));
    assertEquals(Path.of("a/b/c"), Paths.findNameOrElse(path, "c", null));
    assertEquals(Path.of("a/b/c/d"), Paths.findNameOrElse(path, "d", null));
    assertNull(Paths.findNameOrElse(path, "null", null));
  }

  @Test
  void countName() {
    var path = Path.of("a/b/c/d/module-info.java");
    assertEquals(1, Paths.countName(path, "a"));
    assertEquals(1, Paths.countName(path, "b"));
    assertEquals(1, Paths.countName(path, "c"));
    assertEquals(1, Paths.countName(path, "d"));
    assertEquals(1, Paths.countName(path, "module-info.java"));
    assertEquals(0, Paths.countName(path, "e"));
  }

  @Nested
  class DeleteTests {
    @Test
    void deleteEmptyDirectory() {
      var empty = Paths.createTempDirectory("deleteEmptyDirectory");
      assertTrue(Files.exists(empty));
      var deleted = Paths.deleteDirectories(empty);
      assertSame(empty, deleted);
      assertTrue(Files.notExists(deleted));
    }

    @Test
    void deleteNonExistingPath() {
      var root = Path.of("does not exist");
      assertDoesNotThrow(() -> Paths.deleteDirectories(root));
    }
  }

  @Nested
  class FindTests {
    @Test
    void findModuleInfoJavaFilesOfBach() {
      assertLinesMatch(
          """
          .bach/bach.info/module-info.java
          com.github.sormuras.bach/main/java/module-info.java
          com.github.sormuras.bach/test/java-module/module-info.java
          test.base/test/java/module-info.java
          test.integration/test/java/module-info.java
          >> Demo projects starting with a capital letter... >>
          test.projects/test/java/module-info.java
          """
              .lines(),
          Paths.findModuleInfoJavaFiles(Path.of(""), 4).stream()
              .map(Path::toString)
              .map(line -> line.replace('\\', '/')));
    }
  }

  @Nested
  class IsTests {
    @Test
    void isJarFile(@TempDir Path temp) throws Exception {
      var fileNameEndsWithJar = Files.writeString(temp.resolve("file.jar"), "test");
      assertTrue(Paths.isJarFile(fileNameEndsWithJar));
      var directoryNameEndsWithJar = Paths.createDirectories(temp.resolve("dir.jar"));
      assertFalse(Paths.isJarFile(directoryNameEndsWithJar));
    }

    @Test
    void isJavaFile(@TempDir Path temp) throws Exception {
      var fileNameEndsWithJava = Files.writeString(temp.resolve("file.java"), "test");
      assertTrue(Paths.isJavaFile(fileNameEndsWithJava));
      var directoryNameEndsWithJava = Paths.createDirectories(temp.resolve("dir.java"));
      assertFalse(Paths.isJavaFile(directoryNameEndsWithJava));
    }
  }

  @Nested
  class ListTests {

    @Test
    void listingOfBaseDirectory() {
      var actual = Paths.list(Path.of(""), Files::isRegularFile);
      assertLinesMatch(
          List.of(".gitignore", ">> MORE FILES >>", "README.md", ">> more files >>"),
          actual.stream().map(Path::toString).collect(Collectors.toList()));
    }

    @Test
    void listingOfTempDirectory(@TempDir Path temp) {
      var sub = Paths.createDirectories(temp.resolve("sub"));
      assertEquals(List.of(sub), Paths.list(temp, __ -> true));
    }

    @Test
    void listEmptyDirectoryYieldsAnEmptyListOfPaths(@TempDir Path temp) {
      assertEquals(List.of(), Paths.list(temp, __ -> true));
      assertEquals(List.of(), Paths.list(temp, Files::isRegularFile));
      assertEquals(List.of(), Paths.list(temp, Files::isDirectory));
    }
  }
}
