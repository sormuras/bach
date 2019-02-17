import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectTests {

  private final CollectingLogger logger = new CollectingLogger("*");
  private final Bach bach = new Bach(logger, Path.of("."), List.of());

  @Test
  void findDirectories() {
    var root = Path.of(".").toAbsolutePath().normalize();
    var dirs = bach.project.findDirectories(root);
    assertTrue(dirs.contains(root.resolve("demo")));
    assertTrue(dirs.contains(root.resolve("src")));
  }

  @Test
  void findDirectoryNames() {
    var root = Path.of(".").toAbsolutePath().normalize();
    var dirs = bach.project.findDirectoryNames(root);
    assertTrue(dirs.contains("demo"));
    assertTrue(dirs.contains("src"));
  }

  @Test
  void findDirectoriesReturnEmptyListWhenRootDoesNotExist() {
    var root = Path.of("does", "not", "exist");
    assertTrue(bach.project.findDirectories(root).isEmpty());
    assertTrue(bach.project.findDirectoryNames(root).isEmpty());
  }

  @Test
  void findDirectoriesFails(@TempDir Path temp) throws Exception {
    var root = Files.createDirectory(temp.resolve("findDirectoriesFails-"));
    Util.chmod(root, false, true, true);
    assertThrows(Error.class, () -> bach.project.findDirectories(root));
    assertThrows(Error.class, () -> bach.project.findDirectoryNames(root));
    // bach.run(new Bach.Action.TreeDelete(root));
  }
}
