package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.sormuras.bach.api.SourceFolder;
import com.github.sormuras.bach.api.SourceFolders;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceFoldersTests {
  @Test
  void empty() {
    var folders = new SourceFolders(List.of());
    assertThrows(IllegalStateException.class, folders::first);
    assertEquals(0, folders.stream(0).count());
    assertEquals(0, folders.stream(0).count());
    assertEquals(".", folders.toModuleSpecificSourcePath());
  }

  @Test
  void one() {
    var one = new SourceFolder(Path.of("one"), 1);
    var folders = new SourceFolders(List.of(one));
    assertSame(one, folders.first());
    assertEquals(0, folders.stream(0).count());
    assertEquals(1, folders.stream(1).count());
    assertThrows(IllegalStateException.class, folders::toModuleSpecificSourcePath);
  }

  @Test
  void one(@TempDir Path temp) throws Exception {
    var one = new SourceFolder(temp, 1);
    Files.writeString(temp.resolve("module-info.java"), "module one {}");
    var folders = new SourceFolders(List.of(one));
    assertSame(one, folders.first());
    assertEquals(0, folders.stream(0).count());
    assertEquals(1, folders.stream(1).count());
    assertEquals(temp.toString(), folders.toModuleSpecificSourcePath());
  }
}
