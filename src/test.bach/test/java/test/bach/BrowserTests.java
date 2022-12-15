package test.bach;

import com.sun.net.httpserver.HttpServer;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.spi.ToolProvider;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.bach.Browser;

@Registered
@Enabled
public record BrowserTests(Browser browser, HttpServer server) implements ToolProvider {
  public static void main(String... args) throws Exception {
    provider().run(System.out, System.err, args);
  }

  public static BrowserTests provider() throws Exception {
    var browser = new Browser();
    var server = HttpServer.create(new InetSocketAddress("", 0), 0);
    return new BrowserTests(browser, server);
  }

  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      server.start();
      testRead();
      testLoad();
      testHead();
    } finally {
      server.stop(0);
    }
    return 0;
  }

  URI uri() {
    var address = server.getAddress();
    return URI.create("http://%s:%d".formatted(address.getHostString(), address.getPort()));
  }

  void testHead() {
    var response = browser.head(uri());
    if (response.statusCode() == 404) return;
    System.err.println("Unexpected response: " + response);
  }

  void testLoad() {
    var target = Path.of("load.file");
    try {
      var response = browser.load(uri(), target);
      System.err.println("Unexpected response: " + response);
    } catch (RuntimeException exception) {
      assert ("(GET " + uri() + ") 404").equals(exception.toString()) : exception;
    }
  }

  void testRead() {
    var response = browser.read(uri());
    if (response.equals("<h1>404 Not Found</h1>No context found for request")) return;
    System.err.println("Unexpected response: " + response);
  }
}
