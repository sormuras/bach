package test.base.resource;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class WebServer implements ResourceSupplier<HttpServer>, HttpHandler {

  public record Asset(byte[] bytes, String contentType) {

    public static Asset of(byte... bytes) {
      return new Asset(bytes, "application/octet-stream");
    }

    public static Asset ofText(String text) {
      return new Asset(text.getBytes(StandardCharsets.UTF_8), "text/plain");
    }
  }

  private final Map<String, Asset> assets;
  private final HttpServer httpServer;

  public WebServer() {
    this(0);
  }

  public WebServer(int port) {
    this.assets = createAssets();
    this.httpServer = createAndStartHttpServerOnPort(port);
  }

  protected Map<String, Asset> createAssets() {
    return Map.of();
  }

  protected HttpServer createAndStartHttpServerOnPort(int port) {
    try {
      var localhost = new InetSocketAddress("localhost", port);
      var server = HttpServer.create(localhost, 0);
      server.createContext("/", this);
      server.start();
      return server;
    } catch (IOException e) {
      throw new UncheckedIOException("Create HttpServer failed!", e);
    }
  }

  @Override
  public void close() {
    httpServer.stop(0);
  }

  @Override
  public HttpServer get() {
    return httpServer;
  }

  public URI uri(String... path) {
    var address = httpServer.getAddress();
    if (path.length == 0)
      return URI.create("http://%s:%s".formatted(address.getHostName(), address.getPort()));
    try {
      var join = String.join("/", path);
      var raw = new URI(null, null, join, null).getRawPath();
      return URI.create("http://%s:%s/%s".formatted(address.getHostName(), address.getPort(), raw));
    } catch (URISyntaxException exception) {
      throw new RuntimeException(exception);
    }
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    var uri = exchange.getRequestURI();
    var path = uri.getPath();
    var asset = assets.get(path);
    if (asset == null) {
      var response = "File not found: " + uri.getPath();
      exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
      exchange.sendResponseHeaders(404, response.length());
      try (var stream = exchange.getResponseBody()) {
        stream.write(response.getBytes());
      }
      return;
    }

    exchange.getResponseHeaders().set("Content-Type", asset.contentType);
    if ("HEAD".equals(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(200, -1);
      return;
    }
    exchange.sendResponseHeaders(200, asset.bytes.length);
    try (var stream = exchange.getResponseBody()) {
      stream.write(asset.bytes);
    }
  }
}
