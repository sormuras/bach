package run.bach;

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
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.zip.ZipInputStream;

/** A repository for external files mapping external assets. */
public interface ExternalAssetsRepository {

  GitHub DEFAULT = new GitHub("sormuras", "bach-info", "HEAD");

  static ExternalAssetsRepository of(String slug) {
    if (slug == null || slug.isEmpty()) return DEFAULT;
    var scanner = new Scanner(slug);
    scanner.useDelimiter("/");
    var host = scanner.next(); // "github.com"
    if (host.equals("github.com")) {
      var user = scanner.hasNext() ? scanner.next() : DEFAULT.user();
      var repo = scanner.hasNext() ? scanner.next() : DEFAULT.repo();
      var hash = scanner.hasNext() ? String.join("/", scanner.tokens().toList()) : DEFAULT.hash();
      return new GitHub(user, repo, hash);
    }
    throw new RuntimeException("Repository slug not supported: " + slug);
  }

  static Walker walk(Path path) {
    return new Walker(Walker.mapDirectoryTree(path));
  }

  static Walker walk(HttpClient client, ExternalAssetsRepository repository) {
    try {
      var request = HttpRequest.newBuilder(URI.create(repository.zip())).build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() >= 400) throw new RuntimeException(response.toString());
      return new Walker(Walker.mapZipInputStream(response.body()));
    } catch (Exception exception) {
      throw (exception instanceof RuntimeException re) ? re : new RuntimeException(exception);
    }
  }

  String home();

  String source(Info info, String name);

  String zip();

  record GitHub(String user, String repo, String hash) implements ExternalAssetsRepository {
    @Override
    public String home() {
      var joiner = new StringJoiner("/", "https://github.com/", "");
      joiner.add(user).add(repo);
      if (!hash.equals(DEFAULT.hash)) joiner.add("tree").add(hash);
      return joiner.toString();
    }

    @Override
    public String source(Info info, String name) {
      var joiner = new StringJoiner("/", "https://github.com/", "");
      joiner.add(user).add(repo).add("raw").add(hash);
      joiner.add(".bach").add(info.folder()).add(name + info.extension());
      return joiner.toString();
    }

    @Override
    public String zip() {
      var joiner = new StringJoiner("/", "https://github.com/", ".zip");
      return joiner.add(user).add(repo).add("archive").add(hash).toString();
    }
  }

  enum Info {
    /** A module-uri index mapping Java module names to their remote modular JAR file locations. */
    EXTERNAL_MODULES_LOCATOR("external-modules", ".modules-locator.properties"),

    /** An asset-uri index mapping local file paths to their remote resource locations. */
    EXTERNAL_TOOL_DIRECTORY("external-tools", ".tool-directory.properties");

    private final String folder;
    private final String extension;

    Info(String folder, String extension) {
      this.folder = folder;
      this.extension = extension;
    }

    public String folder() {
      return folder;
    }

    public String extension() {
      return extension;
    }

    public String name(String path) {
      return path.substring(path.lastIndexOf('/') + 1).replace(extension, "");
    }
  }

  record Walker(Map<Info, List<String>> map) {
    public String toString(int indent) {
      var joiner = new StringJoiner("\n");
      for (var index : Info.values()) {
        var list = map.get(index);
        if (list == null) {
          joiner.add(index + " not present");
          continue;
        }
        var size = list.size();
        joiner.add(index.folder());
        list.stream()
            .map(line -> line.substring(line.lastIndexOf('/') + 1))
            .map(line -> line.replace(index.extension(), ""))
            .sorted()
            .forEach(name -> joiner.add("  " + name));
        joiner.add("    %d element%s".formatted(size, size == 1 ? "" : "s"));
      }
      return joiner.toString().indent(indent).stripTrailing();
    }

    private static Map<Info, List<String>> mapDirectoryTree(Path start) {
      if (!Files.isDirectory(start)) return Map.of();
      var map = new TreeMap<Info, List<String>>();
      var matcher = start.getFileSystem().getPathMatcher("glob:**.properties");
      try (var stream = Files.find(start, 99, (path, attributes) -> matcher.matches(path))) {
        with_next_path:
        for (var path : stream.toList()) {
          var candidate = path.toUri().toString();
          for (var index : Info.values()) {
            if (candidate.endsWith(index.extension())) {
              map.computeIfAbsent(index, key -> new ArrayList<>()).add(candidate);
              continue with_next_path;
            }
          }
        }
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
      return map;
    }

    private static Map<Info, List<String>> mapZipInputStream(InputStream stream) {
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
}
