package test.integration.settings;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.base.resource.ResourceManager;
import test.base.resource.ResourceManager.Singleton;
import test.base.resource.TempDir;
import test.base.resource.WebServer;
import test.integration.VolatileServer;

@ExtendWith(ResourceManager.class)
class BrowserTests {

  @Nested
  class FilesTests {

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

  @Test
  void read(@Singleton(VolatileServer.class) WebServer server) {
    var bach = Bach.of(Project.newProject("Test", "99"));
    var actual = bach.browser().read(server.uri("index.html").toString());
    assertLinesMatch("""
        Hello World!
        """.lines(), actual.lines());
    assertLinesMatch("""
        New HttpClient created with 10s connect timeout and redirect policy of: NORMAL
        Read http://.+/index.html
        """.lines(), bach.settings().logbook().lines());
  }

  @Test
  void load1(@TempDir Path temp, @Singleton(VolatileServer.class) WebServer server)
      throws Exception {
    var bach = Bach.of(Project.newProject("Test", "99"));
    var file = temp.resolve("target");
    bach.browser().load(server.uri("123.bytes").toString(), file);
    var actual = Files.readAllBytes(file);
    assertArrayEquals(new byte[] {1, 2, 3}, actual);
    assertLinesMatch("""
        New HttpClient created with 10s connect timeout and redirect policy of: NORMAL
        Load .+target from http://.+123.bytes
        """.lines(), bach.settings().logbook().lines());
  }

  @Test
  void load3(@TempDir Path temp, @Singleton(VolatileServer.class) WebServer server)
      throws Exception {
    var bach = Bach.of(Project.newProject("Test", "99"));
    var file123 = temp.resolve("file123");
    var file456 = temp.resolve("file456");
    var file789 = temp.resolve("file789");
    var map =
        Map.of(
            server.uri("123.bytes").toString(), file123,
            server.uri("456.bytes").toString(), file456,
            server.uri("789.bytes").toString(), file789);
    bach.browser().load(map);
    assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(file123));
    assertArrayEquals(new byte[] {4, 5, 6}, Files.readAllBytes(file456));
    assertArrayEquals(new byte[] {7, 8, 9}, Files.readAllBytes(file789));
    assertLinesMatch("""
        New HttpClient created with 10s connect timeout and redirect policy of: NORMAL
        Load 3 files
        Load .+file... from http://.+....bytes
        Load .+file... from http://.+....bytes
        Load .+file... from http://.+....bytes
        """.lines(), bach.settings().logbook().lines());
  }
}
