package test.integration.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.project.SourceFolder;
import com.github.sormuras.bach.project.SourceFolders;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SourceFoldersTests {

  private final static Bach BACH = new Bach(Options.of());

  private static SourceFolder newSourceFolder(Path path) {
    return BACH.computeProjectSourceFolder(path);
  }

  private static SourceFolders newSourceFolders(Path path, String prefix) {
    return BACH.computeProjectSourceFolders(path, prefix);
  }

  @Test
  void empty() {
    var sourceFolders = new SourceFolders(List.of());
    assertTrue(sourceFolders.list().isEmpty());
    assertThrows(ArrayIndexOutOfBoundsException.class, sourceFolders::first);
    assertEquals(".", sourceFolders.toModuleSpecificSourcePath());
  }

  @Nested
  class TestProjectMultiRelease9 {

    final Path main = Path.of("test.projects", "MultiRelease-9", "foo", "main");

    void check(SourceFolders sourceFolders) {
      assertEquals(
          List.of("java", "resources", "java-11", "resources-13", "java-15"),
          sourceFolders.list().stream().map(f -> f.path().getFileName().toString()).toList());
      assertEquals(main.resolve("java"), sourceFolders.first().path());
      assertEquals(main.resolve("java").toString(), sourceFolders.toModuleSpecificSourcePath());
    }

    @Test
    void crafted() {
      var list =
          List.of(
              newSourceFolder(main.resolve("java")),
              newSourceFolder(main.resolve("resources")),
              newSourceFolder(main.resolve("java-11")),
              newSourceFolder(main.resolve("resources-13")),
              newSourceFolder(main.resolve("java-15")));
      check(new SourceFolders(list));
    }

    @Test
    void factory() {
      check(newSourceFolders(main, ""));
    }
  }

  @Test
  void foldersOfTestProjectSingleRelease8() {
    var main = Path.of("test.projects", "SingleRelease-8", "foo", "main");
    var base = main.resolve("java");
    var list =
        List.of(
            newSourceFolder(base),
            newSourceFolder(main.resolve("java-module")),
            newSourceFolder(main.resolve("resources")));
    var sourceFolders = new SourceFolders(list);
    assertEquals(3, sourceFolders.list().size());
    assertSame(base, sourceFolders.first().path());
    assertEquals(
        base + File.pathSeparator + main.resolve("java-module"),
        sourceFolders.toModuleSpecificSourcePath());
  }
}
