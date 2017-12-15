package de.sormuras.bach.modules;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

public class GenerateModuleProperties {

  private final System.Logger log = System.getLogger(getClass().getName());
  private final URI base;
  private final Map<String, String> map;

  GenerateModuleProperties(URI base, Map<String, String> map) {
    this.base = base;
    this.map = map;
  }

  public void run() {
    scan("");
  }

  private void scan(String fragment) {
    var uri = base.resolve(fragment);
    System.out.println(uri);
    read(uri)
        .ifPresent(
            source -> {
              if (source.contains("maven-metadata.xml") && !fragment.isEmpty()) {
                scanMetadata(uri);
                return;
              }
              scanDirectory(fragment, source);
            });
  }

  private void scanDirectory(String fragment, String source) {
    for (var line : source.split("\\R")) {
      if (line.startsWith("<a href=") && line.contains("title")) {
        var name = substring(line, ">", "</a>");
        if (!name.endsWith("/")) {
          name = name + "/";
        }
        scan(fragment + name);
      }
    }
  }

  private void scanMetadata(URI uri) {
    var meta = read(uri.resolve("maven-metadata.xml")).orElseThrow(Error::new);
    if (meta.contains("<lastUpdated>2017")) { // TODO better date check "2017 or higher"
      var group = substring(meta, "<groupId>", "<");
      var artifact = substring(meta, "<artifactId>", "<");
      var version = substring(meta, "<latest>", "<");
      var jar = ToolProvider.findFirst("jar").orElseThrow(Error::new);
      var out = new StringWriter(8000);
      try {
        var tmp = load(uri.resolve(version + "/" + artifact + "-" + version + ".jar").toURL());
        jar.run(
            new PrintWriter(out),
            new PrintWriter(System.err),
            "--describe-module",
            "--file",
            tmp.toString());
        var lines = out.toString().split("\\R");
        var empty = false;
        for (var line : lines) {
          if (empty) {
            var module = line.trim().split("\\s+")[0];
            if (module.contains("@")) {
              module = line.substring(0, line.indexOf('@'));
            }
            var value = group + ":" + artifact;
            var old = map.put(module, value);
            System.out.println(module + " = " + value);
            if (old == null) {
              break;
            }
            if (old.equals(value)) {
              log.log(System.Logger.Level.WARNING, "Already mapped {0} to {1}?!", module, value);
            }
            log.log(System.Logger.Level.WARNING, "Non-unique module name found: {0}", module);
            break;
          }
          if (line.trim().isEmpty()) {
            empty = true;
          }
        }
        Files.delete(tmp);
      } catch (FileNotFoundException e) {
        // ignore
      } catch (Exception e) {
        throw new Error("Scanning maven-metadata failed!", e);
      }
    }
  }

  static Optional<String> read(URI uri) {
    try (var input = uri.toURL().openStream();
        var output = new ByteArrayOutputStream()) {
      input.transferTo(output);
      return Optional.of(output.toString("UTF-8"));
    } catch (Exception exception) {
      System.err.println(exception.toString());
      return Optional.empty();
    }
  }

  static Path load(URL url) throws Exception {
    var parts = url.getFile().split("/");
    var temp = Files.createTempFile("000-", "-" + parts[parts.length - 1]);
    try (var sourceStream = url.openStream();
        var targetStream = Files.newOutputStream(temp)) {
      sourceStream.transferTo(targetStream);
    }
    return temp;
  }

  static String substring(String string, String beginTag, String endTag) {
    var initialIndex = string.indexOf(beginTag);
    if (initialIndex < 0) {
      throw new NoSuchElementException("no '" + beginTag + "' in: " + string);
    }
    var beginIndex = initialIndex + beginTag.length();
    var endIndex = string.indexOf(endTag, beginIndex);
    return string.substring(beginIndex, endIndex).trim();
  }

  public static void main(String... args) throws IOException {
    Map<String, String> map = new TreeMap<>();
    // new
    // GenerateModuleProperties(URI.create("http://central.maven.org/maven2/org/apache/commons/"),
    // map).run();
    // new GenerateModuleProperties(URI.create("http://central.maven.org/maven2/org/kordamp/"),
    // map).run();
    // new GenerateModuleProperties(URI.create("http://central.maven.org/maven2/org/junit/"),
    // map).run();
    // new
    // GenerateModuleProperties(URI.create("http://central.maven.org/maven2/org/springframework/"),
    // map).run();
    new GenerateModuleProperties(URI.create("http://central.maven.org/maven2/org/joda/"), map)
        .run();
    var lines =
        map.entrySet()
            .stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.toList());
    Files.write(Paths.get("module-version-joda.properties"), lines, CREATE, TRUNCATE_EXISTING);
  }
}
