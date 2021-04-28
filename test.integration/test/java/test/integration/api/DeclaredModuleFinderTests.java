package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.DeclaredModule;
import com.github.sormuras.bach.api.DeclaredModuleFinder;
import com.github.sormuras.bach.api.DeclaredModuleReference;
import com.github.sormuras.bach.api.SourceFolders;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeclaredModuleFinderTests {
  @Test
  void empty() {
    var finder = new DeclaredModuleFinder(Map.of());

    assertTrue(finder.find("one").isEmpty());
    assertEquals(0, finder.findAll().size());
    assertTrue(finder.isEmpty());
    assertEquals(0, finder.size());
    assertEquals("", finder.toNames(","));
    assertThrows(IllegalStateException.class, () -> finder.toModuleSourcePaths(true));
    assertThrows(IllegalStateException.class, () -> finder.toModuleSourcePaths(false));
    assertEquals(Map.of(), finder.toModulePatches(new DeclaredModuleFinder(Map.of())));
    assertEquals(Map.of(), new DeclaredModuleFinder(Map.of()).toModulePatches(finder));
  }

  @Test
  void one(@TempDir Path temp) throws Exception {
    var info = Files.writeString(temp.resolve("module-info.java"), "module one {}");
    var reference = DeclaredModuleReference.of(info);
    var sources = new SourceFolders(List.of());
    var resources = new SourceFolders(List.of());
    var module = new DeclaredModule(temp, reference, sources, resources);
    var finder = new DeclaredModuleFinder(Map.of(module.name(), module));

    assertTrue(finder.find("one").isPresent());
    assertEquals(1, finder.findAll().size());
    assertFalse(finder.isEmpty());
    assertEquals(1, finder.size());
    assertEquals("one", finder.toNames(","));
    assertLinesMatch(List.of("one=."), finder.toModuleSourcePaths(true));
    assertLinesMatch(List.of("one=."), finder.toModuleSourcePaths(false));
    assertEquals(Map.of(), finder.toModulePatches(new DeclaredModuleFinder(Map.of())));
    assertEquals(Map.of(), new DeclaredModuleFinder(Map.of()).toModulePatches(finder));
  }
}
