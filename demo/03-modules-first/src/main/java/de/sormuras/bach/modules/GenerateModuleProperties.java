package de.sormuras.bach.modules;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class GenerateModuleProperties {

  private final URI base;
  private final URI offset;
  private final Map<String, String> map;

  GenerateModuleProperties(Map<String, String> map, URI offset) {
    this(map, URI.create("http://central.maven.org/maven2/"), offset);
  }

  GenerateModuleProperties(Map<String, String> map, URI base, URI offset) {
    this.base = base;
    this.offset = offset;
    this.map = map;
  }

  public void run() {
    scan("");
  }

  private void scan(String fragment) {
    var uri = base.resolve(offset).resolve(fragment);
    System.out.println("\nscan(" + fragment + ") // " + uri);
    var optionalSource = read(uri);
    if (!optionalSource.isPresent()) {
      return;
    }
    var source = optionalSource.get();
    if (source.contains("maven-metadata.xml")) {
      if (scanMetadata(uri)) {
        return;
      }
    }
    scanGroup(fragment, source);
  }

  private void scanGroup(String fragment, String source) {
    for (var line : source.split("\\R")) {
      if (line.startsWith("<a href=") && line.contains("title")) {
        var name = substring(line, ">", "</a>");
        if (name.endsWith("/")) {
          scan(fragment + name);
        }
      }
    }
  }

  private boolean scanMetadata(URI uri) {
    var meta = read(uri.resolve("maven-metadata.xml")).orElseThrow(Error::new);
    if (meta.contains("<versioning>")) {
      var updated = substring(meta, "<lastUpdated>", "<");
      if (updated.length() != 14) {
        System.out.println("  [!] unexpected <lastUpdated> format: " + updated);
        return true;
      }
      if (updated.compareTo("20170101000000") < 0) {
        System.out.println("  [!] too old: " + updated);
        return true;
      }
      var group = substring(meta, "<groupId>", "<");
      var artifact = substring(meta, "<artifactId>", "<");
      var version = substring(meta, "<release>", "<"); // TODO Consider 'latest'?
      try {
        // TODO load .pom and assume "jar" packaging
        var jar = load(uri.resolve(version + "/" + artifact + "-" + version + ".jar"));
        if (Files.exists(jar) && Files.size(jar) > 0) {
          scanJar(jar, group, artifact);
        }
        Files.deleteIfExists(jar);
      } catch (Exception e) {
        throw new Error("Scanning maven-metadata.xml failed!", e);
      }
      return true;
    }
    return false;
  }

  private void scanJar(Path path, String group, String artifact) {
    var jar = ToolProvider.findFirst("jar").orElseThrow(Error::new);
    var out = new StringWriter(8000);
    jar.run(
        new PrintWriter(out),
        new PrintWriter(System.err),
        "--describe-module",
        "--file",
        path.toString());
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
        System.out.println("\n\t" + module + " = " + value + "\n");
        if (old == null) {
          return;
        }
        if (old.equals(value)) {
          System.err.println("  [!] already mapped " + module + " to " + value);
        }
        System.err.println("  [!] non-unique module name found: " + module);
        return;
      }
      if (line.trim().isEmpty()) {
        empty = true;
      }
    }
    System.out.println("  [!] not a modular jar: " + path);
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

  static Path load(URI uri) throws Exception {
    var url = uri.toURL();
    var parts = url.getFile().split("/");
    var temp = Files.createTempFile("", "-" + parts[parts.length - 1]);
    try (var sourceStream = url.openStream();
        var targetStream = Files.newOutputStream(temp)) {
      sourceStream.transferTo(targetStream);
    } catch (FileNotFoundException e) {
      System.out.println("  [!] file not found: " + url);
      Files.deleteIfExists(temp);
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
    var map = new TreeMap<String, String>();
    // org/apache/commons/
    // org/kordamp/
    // org/junit/
    // org/springframework/
    new GenerateModuleProperties(map, URI.create("org/apache/commons/")).run();
    var lines =
        map.entrySet()
            .stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.toList());
    Files.write(
        Paths.get("target/module-maven-apache-commons.properties"),
        lines,
        CREATE,
        TRUNCATE_EXISTING);
  }
}
