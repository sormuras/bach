package test.integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.base.resource.ResourceManager;
import test.base.resource.ResourceManager.Singleton;
import test.base.resource.TempDir;
import test.base.resource.WebServer;

@ExtendWith(ResourceManager.class)
class VolatileServerTests {
  @Test
  void copy(@TempDir Path temp, @Singleton(VolatileServer.class) WebServer server)
      throws Exception {
    var uri = server.uri();
    var root = temp.resolve("root.txt");
    try (var stream = uri.toURL().openStream()) {
      Files.copy(stream, root);
    }
    var actual = Files.readString(root);
    assertLinesMatch("""
        <root>
        """.lines(), actual.lines());
  }

  @Test
  void bytes(@TempDir Path temp, @Singleton(VolatileServer.class) WebServer server)
      throws Exception {
    var uri = server.uri("123.bytes");
    var root = temp.resolve("123.bytes");
    try (var stream = uri.toURL().openStream()) {
      Files.copy(stream, root);
    }
    var actual = Files.readAllBytes(root);
    assertArrayEquals(new byte[] {1, 2, 3}, actual);
  }
}
