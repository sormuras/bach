package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Bach;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

/** https://github.com/sormuras/modules */
public class SormurasModulesProperties {

  private final Bach bach;
  private final Map<String, String> variants;
  private Map<String, String> moduleMaven;
  private Map<String, String> moduleVersion;

  public SormurasModulesProperties(Bach bach, Map<String, String> variants) {
    this.bach = bach;
    this.variants = variants;
  }

  public Optional<String> search(String module) {
    if (moduleMaven == null && moduleVersion == null)
      try {
        moduleMaven = load("module-maven.properties");
        moduleVersion = load("module-version.properties");
      } catch (Exception e) {
        throw new RuntimeException("Load module properties failed", e);
      }
    if (moduleMaven == null) throw new IllegalStateException("module-maven map is null");
    if (moduleVersion == null) throw new IllegalStateException("module-version map is null");

    var maven = moduleMaven.get(module);
    if (maven == null) return Optional.empty();
    var indexOfColon = maven.indexOf(':');
    if (indexOfColon < 0) throw new AssertionError("Expected group:artifact, but got: " + maven);
    var version = variants.getOrDefault(module, moduleVersion.get(module));
    if (version == null) return Optional.empty();
    var joiner =
        new Maven.Joiner()
            .group(maven.substring(0, indexOfColon))
            .artifact(maven.substring(indexOfColon + 1))
            .version(version);
    return Optional.of(joiner.toString());
  }

  private static final String ROOT = "https://github.com/sormuras/modules";

  private Map<String, String> load(String properties) throws Exception {
    var root = Path.of(System.getProperty("user.home", ""));
    var cache = Files.createDirectories(root.resolve(".bach/modules"));
    var source = URI.create(String.join("/", ROOT, "raw/master", properties));
    var target = cache.resolve(properties);
    var path = bach.httpCopy(source, target);
    return map(load(new Properties(), path));
  }

  /** Load all strings from the specified file into the passed properties instance. */
  private Properties load(Properties properties, Path path) {
    if (Files.isRegularFile(path)) {
      try (var reader = Files.newBufferedReader(path)) {
        properties.load(reader);
      } catch (Exception e) {
        throw new RuntimeException("Load properties failed: " + path, e);
      }
    }
    return properties;
  }

  /** Convert all {@link String}-based properties into a {@code Map<String, String>}. */
  private static Map<String, String> map(Properties properties) {
    var map = new TreeMap<String, String>();
    for (var name : properties.stringPropertyNames()) {
      map.put(name, properties.getProperty(name));
    }
    return map;
  }
}
