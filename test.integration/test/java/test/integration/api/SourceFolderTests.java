package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.sormuras.bach.api.SourceFolder;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SourceFolderTests {
  @Test
  void empty() {
    var empty = new SourceFolder(Path.of(""), 0);
    assertEquals("", empty.path().toString());
    assertEquals(0, empty.release());
    assertFalse(empty.isTargeted());
    assertFalse(empty.isModuleInfoJavaPresent());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"0", "a", "b0", "c-0", "d.0", "e/0"})
  void parseReleaseNumber0(String string) {
    assertEquals(0, SourceFolder.parseReleaseNumber(string));
  }

  @ParameterizedTest
  @ValueSource(strings = {"9", "9 9", "b9", "c-9", "d.9", "e/9"})
  void parseReleaseNumber9(String string) {
    assertEquals(9, SourceFolder.parseReleaseNumber(string));
  }

  @ParameterizedTest
  @ValueSource(strings = {"99", "9 99", "b99", "c-99", "d.99", "e/99"})
  void parseReleaseNumber99(String string) {
    assertEquals(99, SourceFolder.parseReleaseNumber(string));
  }
}
