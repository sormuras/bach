package test.integration.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.project.SourceFolder;
import com.github.sormuras.bach.project.SourceFolders;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SourceFoldersTests {

  @Test
  void empty() {
    var sourceFolders = new SourceFolders(List.of());
    assertTrue(sourceFolders.list().isEmpty());
    assertThrows(ArrayIndexOutOfBoundsException.class, sourceFolders::first);
    assertThrows(IllegalStateException.class, sourceFolders::toModuleSpecificSourcePath);
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
              SourceFolder.of(main.resolve("java")),
              SourceFolder.of(main.resolve("resources")),
              SourceFolder.of(main.resolve("java-11")),
              SourceFolder.of(main.resolve("resources-13")),
              SourceFolder.of(main.resolve("java-15")));
      check(new SourceFolders(list));
    }

    @Test
    void factory() {
      check(SourceFolders.of(main, ""));
    }
  }

  @Test
  void foldersOfTestProjectSingleRelease8() {
    var main = Path.of("test.projects", "SingleRelease-8", "foo", "main");
    var base = main.resolve("java");
    var list =
        List.of(
            SourceFolder.of(base),
            SourceFolder.of(main.resolve("java-module")),
            SourceFolder.of(main.resolve("resources")));
    var sourceFolders = new SourceFolders(list);
    assertEquals(3, sourceFolders.list().size());
    assertSame(base, sourceFolders.first().path());
    assertEquals(
        base + File.pathSeparator + main.resolve("java-module"),
        sourceFolders.toModuleSpecificSourcePath());
  }
}
