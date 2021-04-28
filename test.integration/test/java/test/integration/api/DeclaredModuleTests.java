package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.DeclaredModule;
import com.github.sormuras.bach.api.DeclaredModuleReference;
import com.github.sormuras.bach.api.SourceFolders;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeclaredModuleTests {
  @Test
  void empty(@TempDir Path temp) throws Exception {
    var info = Files.writeString(temp.resolve("module-info.java"), "module empty {}");
    var reference = DeclaredModuleReference.of(info);
    var sources = new SourceFolders(List.of());
    var resources = new SourceFolders(List.of());
    var module = new DeclaredModule(temp, reference, sources, resources);

    assertSame(temp, module.root());
    assertEquals("empty", module.name());
    assertEquals(info, module.reference().info());
    assertTrue(module.sources().list().isEmpty());
    assertTrue(module.resources().list().isEmpty());
    assertEquals(0, module.compareTo(new DeclaredModule(temp, reference, sources, resources)));
  }
}
