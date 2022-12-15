package run.bach.external;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.zip.ZipInputStream;

public record Walker(Map<Info, List<String>> map) {

  public static Walker of(Path path) {
    return new Walker(mapDirectoryTree(path));
  }

  public static Walker of(HttpClient client, Repository repository) {
    try {
      var request = HttpRequest.newBuilder(URI.create(repository.zip())).build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() >= 400) throw new RuntimeException(response.toString());
      return new Walker(mapZipInputStream(response.body()));
    } catch (Exception exception) {
      throw (exception instanceof RuntimeException re) ? re : new RuntimeException(exception);
    }
  }

  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    for (var info : Info.values()) {
      var list = map.get(info);
      if (list == null) {
        // joiner.add(info + " not present");
        continue;
      }
      var size = list.size();
      joiner.add(info.folder());
      list.stream()
          .map(line -> line.substring(line.lastIndexOf('/') + 1))
          .map(line -> line.replace(info.extension(), ""))
          .sorted()
          .forEach(name -> joiner.add("  " + name));
      joiner.add("    %d element%s".formatted(size, size == 1 ? "" : "s"));
    }
    return joiner.toString().indent(indent).stripTrailing();
  }

  static Map<Info, List<String>> mapDirectoryTree(Path start) {
    if (!Files.isDirectory(start)) return Map.of();
    var map = new TreeMap<Info, List<String>>();
    var matcher = start.getFileSystem().getPathMatcher("glob:**.properties");
    try (var stream = Files.find(start, 99, (path, attributes) -> matcher.matches(path))) {
      with_next_path:
      for (var path : stream.toList()) {
        var candidate = path.toUri().toString();
        for (var info : Info.values()) {
          if (candidate.endsWith(info.extension())) {
            map.computeIfAbsent(info, key -> new ArrayList<>()).add(candidate);
            continue with_next_path;
          }
        }
      }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    return map;
  }

  static Map<Info, List<String>> mapZipInputStream(InputStream stream) {
    var map = new TreeMap<Info, List<String>>();
    try (var zip = new ZipInputStream(stream)) {
      with_next_entry:
      while (true) {
        var entry = zip.getNextEntry();
        if (entry == null) break;
        var name = entry.getName();
        if (!name.endsWith(".properties")) continue;
        for (var index : Info.values()) {
          if (name.endsWith(index.extension())) {
            map.computeIfAbsent(index, key -> new ArrayList<>()).add(name);
            continue with_next_entry;
          }
        }
      }
      return map;
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
