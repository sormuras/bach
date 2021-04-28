package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.api.ModulePaths;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModulePathsTests {
  @Test
  void empty() {
    var tweaks = new ModulePaths(List.of());
    assertTrue(tweaks.list().isEmpty());
    assertTrue(tweaks.pruned().isEmpty());
  }

  @Test
  void withOneEntryThatExists() {
    var tweaks = new ModulePaths(List.of(Path.of("")));
    assertEquals(1, tweaks.list().size());
    assertEquals(1, tweaks.pruned().size());
  }

  @Test
  void withNonExistingEntries() {
    var tweaks = new ModulePaths(List.of(Path.of("1"), Path.of("2"), Path.of("3")));
    assertEquals(3, tweaks.list().size());
    assertTrue(tweaks.pruned().isEmpty());
  }

}
