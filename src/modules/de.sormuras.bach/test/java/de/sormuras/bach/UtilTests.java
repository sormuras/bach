package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UtilTests {
  @Test
  void listEmptyDirectoryYieldsAnEmptyListOfPaths(@TempDir Path temp) {
    assertEquals(List.of(), Util.list(temp));
    assertEquals(List.of(), Util.list(temp, Files::isRegularFile));
    assertEquals(List.of(), Util.list(temp, Files::isDirectory));
  }
}
