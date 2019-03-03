import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@BachExtension
class TreeTests {

  private final Bach bach;

  TreeTests(Bach bach) {
    this.bach = bach;
  }

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

    bach.treeDelete(root, path -> path.startsWith(root.resolve("b")));
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

    bach.treeDelete(root, path -> path.endsWith("file-0"));
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

    bach.treeCopy(root.resolve("x"), root.resolve("a/b/c"));
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

    bach.treeCopy(root.resolve("x"), root.resolve("x/y"));
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

    bach.treeDelete(root);
    assertTrue(Files.notExists(root));
  }

  @Test
  void copyNonExistingDirectoryFails() {
    var root = Path.of("does not exist");
    assertThrows(IllegalArgumentException.class, () -> bach.treeCopy(root, Path.of(".")));
  }

  @Test
  void copyAndItsPreconditions(@TempDir Path temp) throws Exception {
    var regular = Util.createFiles(temp, 2).get(0);
    assertThrows(Throwable.class, () -> bach.treeCopy(regular, Path.of(".")));
    var directory = Files.createDirectory(temp.resolve("directory"));
    Util.createFiles(directory, 3);
    assertThrows(Throwable.class, () -> bach.treeCopy(directory, regular));
    bach.treeCopy(directory, directory);
    assertThrows(Throwable.class, () -> bach.treeCopy(temp, directory));
    var forbidden = Files.createDirectory(temp.resolve("forbidden"));
    try {
      Util.chmod(forbidden, false, false, true);
      assertThrows(Throwable.class, () -> bach.treeCopy(directory, forbidden));
    } finally {
      Util.chmod(forbidden, true, true, true);
    }
  }

  @Test
  void deleteEmptyDirectory() throws Exception {
    var empty = Files.createTempDirectory("deleteEmptyDirectory");
    assertTrue(Files.exists(empty));
    bach.treeDelete(empty);
    assertTrue(Files.notExists(empty));
  }

  @Test
  void deleteNonExistingPath() {
    var root = Path.of("does not exist");
    assertDoesNotThrow(() -> bach.treeDelete(root));
  }

  @Test
  void walkFailsForNonExistingPath() {
    var root = Path.of("does not exist");
    assertThrows(Error.class, () -> bach.treeWalk(root, System.out::println));
  }

  private void assertTreeWalkMatches(Path root, String... expected) {
    var actualLines = new ArrayList<String>();
    bach.treeWalk(root, actualLines::add);
    assertLinesMatch(List.of(expected), actualLines);
  }
}
