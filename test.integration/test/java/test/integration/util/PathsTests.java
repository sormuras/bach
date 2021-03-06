package test.integration.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.util.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PathsTests {

  @Nested
  class DeleteTests {
    @Test
    void deleteEmptyDirectory() throws Exception {
      var empty = Files.createTempDirectory("deleteEmptyDirectory");
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
  class ListTests {

    @Test
    void listingOfBaseDirectory() {
      var actual = Paths.list(Path.of(""), Files::isRegularFile);
      assertLinesMatch(
          List.of(".gitignore", ">> MORE FILES >>", "README.md", ">> more files >>"),
          actual.stream().map(Path::toString).collect(Collectors.toList()));
    }

    @Test
    void listEmptyDirectoryYieldsAnEmptyListOfPaths(@TempDir Path temp) {
      assertEquals(List.of(), Paths.list(temp, __ -> true));
      assertEquals(List.of(), Paths.list(temp, Files::isRegularFile));
      assertEquals(List.of(), Paths.list(temp, Files::isDirectory));
    }
  }
}
