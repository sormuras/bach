package test.integration.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.base.resource.ResourceManager;
import test.base.resource.ResourceManager.Singleton;
import test.base.resource.TempDir;
import test.base.resource.WebServer;
import test.integration.Auxiliary;

@ExtendWith(ResourceManager.class)
class HttpTraitTests {

  @Test
  void read(@Singleton(VolatileServer.class) WebServer server) {
    var bach = Auxiliary.newEmptyBach();
    var actual = bach.httpRead(server.uri("index.html").toString());
    assertLinesMatch("""
        Hello World!
        """.lines(), actual.lines());
    assertLinesMatch(
        """
        Read http.+
        New HttpClient.Builder with 9s connect timeout and NORMAL redirect policy
        """
            .lines(),
        bach.logbook().lines());
  }

  @Test
  void load1(@TempDir Path temp, @Singleton(VolatileServer.class) WebServer server)
      throws Exception {
    var bach = Auxiliary.newEmptyBach();
    var file = temp.resolve("target");
    bach.httpLoad(server.uri("123.bytes").toString(), file);
    assertLinesMatch(
        """
        Load .+target from http.+
        New HttpClient.Builder with 9s connect timeout and NORMAL redirect policy
        """
            .lines(),
        bach.logbook().lines());
    var actual = Files.readAllBytes(file);
    assertArrayEquals(new byte[] {1, 2, 3}, actual);
  }

  @Test
  void load3(@TempDir Path temp, @Singleton(VolatileServer.class) WebServer server)
      throws Exception {
    var bach = Auxiliary.newEmptyBach();
    var file123 = temp.resolve("file123");
    var file456 = temp.resolve("file456");
    var file789 = temp.resolve("file789");
    bach.httpLoad(
        Map.of(
            server.uri("123.bytes").toString(), file123,
            server.uri("456.bytes").toString(), file456,
            server.uri("789.bytes").toString(), file789));
    assertLinesMatch(
        """
        Load 3 files
        Load .+file... from http.+
        New HttpClient.Builder with 9s connect timeout and NORMAL redirect policy
        Load .+file... from http.+
        Load .+file... from http.+
        """
            .lines(),
        bach.logbook().lines());
    assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(file123));
    assertArrayEquals(new byte[] {4, 5, 6}, Files.readAllBytes(file456));
    assertArrayEquals(new byte[] {7, 8, 9}, Files.readAllBytes(file789));
  }

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
