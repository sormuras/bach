package test.integration.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.project.SourceFolder;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SourceFolderTests {

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

  @Test
  void directoryWithModuleInfoFile() {
    var sourceFolder = SourceFolder.of(Path.of(".bach/bach.info"));
    assertEquals(0, sourceFolder.release());
    assertFalse(sourceFolder.isTargeted());
    assertTrue(sourceFolder.isModuleInfoJavaPresent());
  }

  @Test
  void directoriesOfTestProjectMultiRelease9() {
    var main = Path.of("test.projects", "MultiRelease-9", "foo", "main");
    var base = SourceFolder.of(main.resolve("java"));
    assertEquals(0, base.release());
    assertTrue(base.isModuleInfoJavaPresent());
    assertEquals(11, SourceFolder.of(main.resolve("java-11")).release());
    assertEquals(15, SourceFolder.of(main.resolve("java-15")).release());
    assertEquals(0, SourceFolder.of(main.resolve("resources")).release());
    assertEquals(13, SourceFolder.of(main.resolve("resources-13")).release());
  }

  @Test
  void pathWithoutTrailingNumberReturnsReleaseZero() {
    assertEquals(0, SourceFolder.of(Path.of("foo/bar")).release());
  }

  @ParameterizedTest
  @ValueSource(strings = {"9", "bar9", "foo/9", "foo/bar9", "bar-9", "bar-1+9", "bar+1-9", "bar1.9"})
  void nonExistingPathWithTrailingNumber9(String path) {
    assertEquals(9, SourceFolder.of(Path.of(path)).release());
  }

  @Test
  void nonExistingPathWithTrailingNumber99() {
    assertEquals(99, SourceFolder.of(Path.of("foo/bar99")).release());
  }

}
