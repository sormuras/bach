package test.integration.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.project.SourceFolder;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SourceFolderTests {

  @Nested
  class Canonical {

    @Test
    void emptyPathAndZeroAsRelease() {
      var sourceFolder = new SourceFolder(Path.of(""), 0);
      assertEquals("", sourceFolder.path().toString());
      assertEquals(0, sourceFolder.release());
      assertFalse(sourceFolder.isTargeted());
      assertFalse(sourceFolder.isModuleInfoJavaPresent());
    }

    @Test
    void emptyPathAndOneAsRelease() {
      var sourceFolder = new SourceFolder(Path.of(""), 1);
      assertEquals("", sourceFolder.path().toString());
      assertEquals(1, sourceFolder.release());
      assertTrue(sourceFolder.isTargeted());
      assertFalse(sourceFolder.isModuleInfoJavaPresent());
    }
  }

  private final static Bach BACH = new Bach(Options.of());

  private static SourceFolder newSourceFolder(Path path) {
    return BACH.computeProjectSourceFolder(path);
  }

  @Test
  void directoryWithModuleInfoFile() {
    var sourceFolder = newSourceFolder(Path.of(".bach/bach.info"));
    assertEquals(0, sourceFolder.release());
    assertFalse(sourceFolder.isTargeted());
    assertTrue(sourceFolder.isModuleInfoJavaPresent());
  }

  @Test
  void directoriesOfTestProjectMultiRelease9() {
    var main = Path.of("test.projects", "MultiRelease-9", "foo", "main");
    var base = newSourceFolder(main.resolve("java"));
    assertEquals(0, base.release());
    assertTrue(base.isModuleInfoJavaPresent());
    assertEquals(11, newSourceFolder(main.resolve("java-11")).release());
    assertEquals(15, newSourceFolder(main.resolve("java-15")).release());
    assertEquals(0, newSourceFolder(main.resolve("resources")).release());
    assertEquals(13, newSourceFolder(main.resolve("resources-13")).release());
  }

  @Test
  void pathWithoutTrailingNumberReturnsReleaseZero() {
    assertEquals(0, newSourceFolder(Path.of("foo/bar")).release());
  }

  @ParameterizedTest
  @ValueSource(strings = {"9", "a9", "a/9", "a/b9", "a-9", "a-1+9", "a+1-9", "a1.9"})
  void nonExistingPathWithTrailingNumber9(String path) {
    assertEquals(9, newSourceFolder(Path.of(path)).release());
  }

  @Test
  void nonExistingPathWithTrailingNumber99() {
    assertEquals(99, newSourceFolder(Path.of("foo/bar99")).release());
  }
}
