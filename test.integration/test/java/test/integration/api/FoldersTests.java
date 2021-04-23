package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.Folders;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FoldersTests {

  @Test
  void canonical() {
    var folders = Folders.of("./WILL-BE-REMOVED-BY-NORMALIZATION/..");
    assertEquals("", folders.root().toString());
    assertEquals(Path.of(".bach/external-modules"), folders.externals());
    assertEquals(Path.of(".bach/external-modules/file"), folders.externals("file"));
    assertEquals(Path.of(".bach/workspace"), folders.workspace());
    assertEquals(Path.of(".bach/workspace/modules"), folders.modules(CodeSpace.MAIN));
    assertEquals(Path.of(".bach/workspace/modules-test"), folders.modules(CodeSpace.TEST));
  }
}
