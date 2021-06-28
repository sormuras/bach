package test.integration.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import com.github.sormuras.bach.settings.Folders;
import org.junit.jupiter.api.Test;

class FoldersTests {

  @Test
  void canonical() {
    var folders = Folders.of("./WILL-BE-REMOVED-BY-NORMALIZATION/..");
    assertEquals(Path.of(""), folders.root());
    assertEquals(Path.of("a/b"), folders.root("a", "b"));
    assertEquals(Path.of(".bach/external-modules"), folders.externalModules());
    assertEquals(Path.of(".bach/external-modules/a/b"), folders.externalModules("a", "b"));
    assertEquals(Path.of(".bach/external-tools"), folders.externalTools());
    assertEquals(Path.of(".bach/external-tools/a/b"), folders.externalTools("a", "b"));
    assertEquals(Path.of(".bach/workspace"), folders.workspace());
    assertEquals(Path.of(".bach/workspace/a/b"), folders.workspace("a", "b"));
  }
}
