package test.integration.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.Spaces;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.base.resource.ResourceManager;
import test.base.resource.ResourceManager.Singleton;
import test.base.resource.TempDir;
import test.base.resource.WebServer;

@ExtendWith(ResourceManager.class)
class HttpTraitTests {

  static class TestServer extends WebServer {

    @Override
    protected Map<String, Asset> createAssets() {
      return Map.of(
          "/", Asset.ofText("<root>"),
          "/index.html", Asset.ofText("Hello World!"),
          "/123.bytes", Asset.of(new byte[] {1, 2, 3}),
          "/456.bytes", Asset.of(new byte[] {4, 5, 6}),
          "/789.bytes", Asset.of(new byte[] {7, 8, 9})
      );
    }
  }

  static Bach newBach() {
    var logbook = Logbook.ofErrorPrinter();
    var factory = new Factory();
    var options = Options.ofDefaultValues();
    var folders = Folders.of("");
    var spaces = new Spaces(new CodeSpaceMain(), new CodeSpaceTest());
    var project = new Project("explicit", folders, spaces);
    return new Bach(logbook, options, factory, project);
  }

  @Test
  void read(@Singleton(TestServer.class) WebServer server) {
    var bach = newBach();
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
  void load1(@TempDir Path temp, @Singleton(TestServer.class) WebServer server) throws Exception {
    var bach = newBach();
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
  void load3(@TempDir Path temp, @Singleton(TestServer.class) WebServer server) throws Exception {
    var bach = newBach();
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
  void copy(@TempDir Path temp, @Singleton(TestServer.class) WebServer server) throws Exception {
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
  void bytes(@TempDir Path temp, @Singleton(TestServer.class) WebServer server) throws Exception {
    var uri = server.uri("123.bytes");
    var root = temp.resolve("123.bytes");
    try (var stream = uri.toURL().openStream()) {
      Files.copy(stream, root);
    }
    var actual = Files.readAllBytes(root);
    assertArrayEquals(new byte[] {1, 2, 3}, actual);
  }
}
