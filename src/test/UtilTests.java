import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UtilTests {

  @Test
  void newFails() {
    assertThrows(Error.class, Bach.Util::new);
  }

  @ParameterizedTest
  @ValueSource(strings = {"a.b", "a.b#c", "a.b?c", "https://host/path/../a.b#c?d=e&more=b.a"})
  void extractFileName(String uri) {
    assertEquals("a.b", Bach.Util.extractFileName(URI.create(uri)));
  }

  @DisabledIfSystemProperty(named = "bach.offline", matches = "true")
  @ParameterizedTest
  @ValueSource(strings = {"http", "https"})
  void downloadLicenseFromApacheOrg(String protocol, @TempDir Path temp) throws Exception {
    var log = new ArrayList<String>();
    var uri = URI.create(protocol + "://www.apache.org/licenses/LICENSE-2.0.txt");
    var first = Bach.Util.download(log::add, false, temp, uri);
    assertTrue(Files.readString(first).contains("Apache License"));
    var second = Bach.Util.download(log::add, false, temp, uri);
    assertEquals(first, second);
    Files.writeString(first, "Lorem ipsum...");
    assertFalse(Files.readString(first).contains("Apache License"));
    var third = Bach.Util.download(log::add, false, temp, uri);
    assertEquals(first, third);
    var forth = Bach.Util.download(log::add, true, temp, uri);
    assertEquals(first, forth);
    Files.delete(first);
    var e = assertThrows(Exception.class, () -> Bach.Util.download(log::add, true, temp, uri));
    assertEquals("Target is missing and being offline: " + first, e.getMessage());
    assertLinesMatch(
        List.of(
            // first
            "download(" + uri + ")",
            ">> TRANSFER >>",
            "Downloaded LICENSE-2.0.txt successfully.",
            // second
            "download(" + uri + ")",
            ">> TIMESTAMP COMPARISON >>",
            "Already downloaded LICENSE-2.0.txt previously.",
            // third
            "download(" + uri + ")",
            ">> TIMESTAMP COMPARISON >>",
            "Local target file differs from remote source -- replacing it...",
            ">> TRANSFER >>",
            "Downloaded LICENSE-2.0.txt successfully.",
            // forth
            "download(" + uri + ")",
            "Offline mode is active and target already exists.",
            // fifth
            "download(" + uri + ")"),
        log);
  }

  @Test
  void downloadRelativeUriThrows() {
    var log = new ArrayList<String>();
    var uri = URI.create("void");
    var base = Path.of(".");
    var e = assertThrows(Exception.class, () -> Bach.Util.download(log::add, true, base, uri));
    assertTrue(e.getMessage().contains("URI is not absolute"));
    assertLinesMatch(List.of("download(" + uri + ")"), log);
  }

  @Test
  void findDirectories() {
    var root = Path.of(".").toAbsolutePath().normalize();
    var dirs = Bach.Util.findDirectories(root);
    assertTrue(dirs.contains(root.resolve("demo")));
    assertTrue(dirs.contains(root.resolve("src")));
  }

  @Test
  void findDirectoryNames() {
    var root = Path.of(".").toAbsolutePath().normalize();
    var dirs = Bach.Util.findDirectoryNames(root);
    assertTrue(dirs.contains("demo"));
    assertTrue(dirs.contains("src"));
  }

  @Test
  void findDirectoriesReturnEmptyListWhenRootDoesNotExist() {
    var root = Path.of("does", "not", "exist");
    assertTrue(Bach.Util.findDirectories(root).isEmpty());
    assertTrue(Bach.Util.findDirectoryNames(root).isEmpty());
  }

  @Test
  void findDirectoriesFails() throws Exception {
    var root = Files.createTempDirectory("findDirectoriesFails-");
    Util.chmod(root, false, true, true);
    assertThrows(Error.class, () -> Bach.Util.findDirectories(root));
    assertThrows(Error.class, () -> Bach.Util.findDirectoryNames(root));
    Util.chmod(root, true, true, true);
  }

  @Test
  void findFiles() throws Exception {
    var root = Bach.USER_PATH;
    var files = Bach.Util.findFiles(List.of(root), __ -> true);
    assertTrue(files.contains(root.resolve("boot.jsh")));
    assertTrue(files.contains(root.resolve("build.jsh")));
    assertTrue(files.contains(root.resolve("src/bach/Bach.java")));
  }

  @Test
  void findFilesForNonExistentRootFails() {
    var root = Path.of("does", "not", "exist");
    assertThrows(Exception.class, () -> Bach.Util.findFiles(List.of(root), __ -> true));
  }

  @Test
  void findFilesForRegularFileRootReturnsThatFile() throws Exception {
    var root = Path.of("README.md");
    var files = Bach.Util.findFiles(List.of(root), __ -> true);
    assertEquals(List.of(root), files);
  }

  @Test
  void findFilesInHiddenDirectoryFails(@TempDir Path temp) throws Exception {
    var root = Files.createDirectory(temp.resolve("-w-"));
    Util.chmod(root, false, true, false);
    assertThrows(Exception.class, () -> Bach.Util.findFiles(List.of(temp), __ -> true));
    Util.chmod(root, true, true, true);
  }

  @Test
  void isJavaFile() {
    assertFalse(Bach.Util.isJavaFile(Path.of("")));
    assertFalse(Bach.Util.isJavaFile(Path.of("a/b")));
    assertTrue(Bach.Util.isJavaFile(Path.of("src/test/UtilTests.java")));
    assertFalse(Bach.Util.isJavaFile(Path.of("src/test-resources/Util.isJavaFile.java")));
  }

  @Test
  void joinPathsToString() {
    var abc = "a" + File.pathSeparator + "b" + File.pathSeparator + "c";
    assertEquals("", Bach.Util.join(List.of()));
    assertEquals("a", Bach.Util.join(List.of("a")));
    assertEquals(abc, Bach.Util.join(List.of("a", "b", "c")));

    assertEquals("", Bach.Util.join(""));
    assertEquals("a", Bach.Util.join("a"));
    assertEquals(abc, Bach.Util.join("a", "b", "c"));
  }

  @Test
  void unzip(@TempDir Path temp) throws Exception {
    var zip = Path.of("demo", "scaffold.zip");
    var one = Files.createDirectories(temp.resolve("one"));
    var home1 = Bach.Util.unzip(zip, one);
    assertEquals(one, home1);
    assertLinesMatch(
        Files.readAllLines(Path.of("src", "test-resources", "demo", "scaffold.clean.txt")),
        Util.treeWalk(home1));

    var two = Files.createDirectories(temp.resolve("two"));
    var zip2 = Files.copy(zip, two.resolve(zip.getFileName()));
    var home2 = Bach.Util.unzip(zip2);
    assertEquals(two, home2);
    Files.delete(zip2);
    assertLinesMatch(
        Files.readAllLines(Path.of("src", "test-resources", "demo", "scaffold.clean.txt")),
        Util.treeWalk(home2));
  }

  @Nested
  class Trees {

    @Test
    void tree() throws Exception {
      Path root = Files.createTempDirectory("tree-root-");
      assertTrue(Files.exists(root));
      assertEquals(1, Files.walk(root).count());
      assertTreeWalkMatches(root);

      Util.createFiles(root, 3);
      assertEquals(1 + 3, Files.walk(root).count());
      assertTreeWalkMatches(root, "file-0", "file-1", "file-2");

      Util.createFiles(Files.createDirectory(root.resolve("a")), 3);
      Util.createFiles(Files.createDirectory(root.resolve("b")), 3);
      Util.createFiles(Files.createDirectory(root.resolve("x")), 4);
      assertTrue(Files.exists(root));
      assertTreeWalkMatches(
          root,
          "a",
          "a/file-0",
          "a/file-1",
          "a/file-2",
          "b",
          "b/file-0",
          "b/file-1",
          "b/file-2",
          "file-0",
          "file-1",
          "file-2",
          "x",
          "x/file-0",
          "x/file-1",
          "x/file-2",
          "x/file-3");

      Bach.Util.treeDelete(root, path -> path.startsWith(root.resolve("b")));
      assertTreeWalkMatches(
          root,
          "a",
          "a/file-0",
          "a/file-1",
          "a/file-2",
          "file-0",
          "file-1",
          "file-2",
          "x",
          "x/file-0",
          "x/file-1",
          "x/file-2",
          "x/file-3");

      Bach.Util.treeDelete(root, path -> path.endsWith("file-0"));
      assertTreeWalkMatches(
          root,
          "a",
          "a/file-1",
          "a/file-2",
          "file-1",
          "file-2",
          "x",
          "x/file-1",
          "x/file-2",
          "x/file-3");

      Bach.Util.treeCopy(root.resolve("x"), root.resolve("a/b/c"));
      assertTreeWalkMatches(
          root,
          "a",
          "a/b",
          "a/b/c",
          "a/b/c/file-1",
          "a/b/c/file-2",
          "a/b/c/file-3",
          "a/file-1",
          "a/file-2",
          "file-1",
          "file-2",
          "x",
          "x/file-1",
          "x/file-2",
          "x/file-3");

      Bach.Util.treeCopy(root.resolve("x"), root.resolve("x/y"));
      assertTreeWalkMatches(
          root,
          "a",
          "a/b",
          "a/b/c",
          "a/b/c/file-1",
          "a/b/c/file-2",
          "a/b/c/file-3",
          "a/file-1",
          "a/file-2",
          "file-1",
          "file-2",
          "x",
          "x/file-1",
          "x/file-2",
          "x/file-3",
          "x/y",
          "x/y/file-1",
          "x/y/file-2",
          "x/y/file-3");

      Bach.Util.treeDelete(root);
      assertTrue(Files.notExists(root));
    }

    @Test
    void copyNonExistingDirectoryFails() {
      var root = Path.of("does not exist");
      assertThrows(IllegalArgumentException.class, () -> Bach.Util.treeCopy(root, Path.of(".")));
    }

    @Test
    void copyFailures(@TempDir Path temp) throws Exception {
      var regular = Util.createFiles(temp, 2).get(0);
      assertThrows(Throwable.class, () -> Bach.Util.treeCopy(regular, Path.of(".")));
      var directory = Files.createDirectory(temp.resolve("directory"));
      Util.createFiles(directory, 3);
      assertThrows(Throwable.class, () -> Bach.Util.treeCopy(directory, regular));
      Bach.Util.treeCopy(directory, directory);
      assertThrows(Throwable.class, () -> Bach.Util.treeCopy(temp, directory));
      var forbidden = Files.createDirectory(temp.resolve("forbidden"));
      try {
        Util.chmod(forbidden, false, false, true);
        assertThrows(Throwable.class, () -> Bach.Util.treeCopy(directory, forbidden));
      } finally {
        Util.chmod(forbidden, true, true, true);
      }
    }

    @Test
    void deleteEmptyDirectory() throws Exception {
      var empty = Files.createTempDirectory("deleteEmptyDirectory");
      assertTrue(Files.exists(empty));
      Bach.Util.treeDelete(empty);
      assertTrue(Files.notExists(empty));
    }

    @Test
    void deleteNonExistingPath() {
      var root = Path.of("does not exist");
      assertDoesNotThrow(() -> Bach.Util.treeDelete(root));
    }

    @Test
    void walkFailsForNonExistingPath() {
      var root = Path.of("does not exist");
      assertThrows(Error.class, () -> Util.treeWalk(root, System.out::println));
    }

    private void assertTreeWalkMatches(Path root, String... expected) {
      var actualLines = new ArrayList<String>();
      Util.treeWalk(root, actualLines::add);
      assertLinesMatch(List.of(expected), actualLines);
    }
  }
}
