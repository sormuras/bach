import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Stream;

class externals {

  public static void main(String... args) throws Exception {
    var externals = new externals();
    var paths = args.length == 0 ? findProperties() : Stream.of(args).map(Path::of).toList();
    for (var path : paths) externals.validateProperties(path);
  }

  static List<Path> findProperties() throws Exception {
    var syntaxAndPattern = "glob:.bach/external-**.properties";
    System.out.println(syntaxAndPattern);
    var directory = Path.of("");
    var matcher = directory.getFileSystem().getPathMatcher(syntaxAndPattern);
    try (var stream = Files.find(directory, 9, (path, attributes) -> matcher.matches(path))) {
      return stream.toList();
    }
  }

  final HttpClient http = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();

  void validateProperties(Path path) throws Exception {
    if (Files.notExists(path)) throw new IllegalArgumentException("no such file: " + path);
    System.out.println();
    System.out.println(path);
    var properties = new Properties();
    properties.load(Files.newBufferedReader(path));
    for (var key : new TreeSet<>(properties.stringPropertyNames())) {
      var value = properties.getProperty(key);
      validateProperty(key, value);
    }
  }

  void validateProperty(String key, String value) throws Exception {
    if (key.startsWith("@") || value.startsWith("string:")) {
      print(value.length(), key, value);
      return;
    }
    if (value.startsWith("http")) {
      var uri = URI.create(value);
      var request = HttpRequest.newBuilder(uri).method("HEAD", BodyPublishers.noBody()).build();
      var response = http.send(request, BodyHandlers.discarding());
      if (response.statusCode() == 200) {
        var size = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        print(size, key, value);
      } else {
        response
            .headers()
            .map()
            .forEach((header, entry) -> System.out.printf("%s -> %s%n", header, entry));
      }
      return;
    }
    System.out.printf("Unknown property protocol %s=%s%n", key, value);
  }

  void print(long size, String key, String value) {
    var line = value.replace('\n', '\\').replace('\r', '\\');
    var snippet = line.length() < 100 ? line : line.substring(0, 95) + "[...]";
    System.out.printf("%,11d %s %s%n", size, key, snippet);
  }
}
