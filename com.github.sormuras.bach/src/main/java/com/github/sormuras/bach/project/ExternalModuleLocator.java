package com.github.sormuras.bach.project;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.TreeMap;

/** An external module locator links module names to their remote locations. */
@FunctionalInterface
public interface ExternalModuleLocator {

  String locate(String module);

  default Map<String, String> locations() {
    return Map.of();
  }

  default String caption() {
    return getClass().getSimpleName();
  }

  record SingleExternalModuleLocator(String module, String uri) implements ExternalModuleLocator {
    @Override
    public String caption() {
      return "SingleExternalModuleLocator: " + module + " -> " + uri;
    }

    @Override
    public String locate(String module) {
      return module().equals(module) ? uri : null;
    }

    @Override
    public Map<String, String> locations() {
      return Map.of(module, uri);
    }
  }

  record MultiExternalModuleLocator(Map<String, String> map) implements ExternalModuleLocator {
    @Override
    public String caption() {
      return "MultiExternalModuleLocator for %d module%s"
          .formatted(map.size(), map.size() == 1 ? "" : "s");
    }

    @Override
    public String locate(String module) {
      return map.get(module);
    }

    @Override
    public Map<String, String> locations() {
      return map;
    }
  }

  record PropertiesBundleModuleLocator(List<Properties> bundle) implements ExternalModuleLocator {

    public static ExternalModuleLocator of(Path base) {
      if (Files.notExists(base)) throw new IllegalArgumentException("Base must exist: " + base);
      var filename = base.getFileName().toString();
      if (!filename.endsWith(".properties")) throw new IllegalArgumentException(".properties");
      var name = filename.substring(0, filename.length() - 11);

      var suffixes = List.of(computeOsName(), computeOsArch());
      var bundle = new ArrayList<Properties>();
      for (int i = 0; i < suffixes.size(); i++) {
        var joiner = new StringJoiner("_", "_", "");
        for (int j = 0; j < suffixes.size() - i; j++) joiner.add(suffixes.get(j));
        var file = base.resolveSibling(name + joiner + ".properties");
        if (Files.notExists(file)) continue;
        bundle.add(loadProperties(file));
      }
      bundle.add(loadProperties(base));
      return new PropertiesBundleModuleLocator(List.copyOf(bundle));
    }

    @Override
    public String locate(String module) {
      for (var properties : bundle) {
        var location = properties.getProperty(module);
        if (location != null) return location;
      }
      return null;
    }

    @Override
    public Map<String, String> locations() {
      var map = new TreeMap<String, String>();
      for (var properties : bundle) {
        for (var module : properties.stringPropertyNames()) {
          if (map.containsKey(module)) continue;
          map.put(module, properties.getProperty(module));
        }
      }
      return map;
    }

    static String computeOsName() {
      var os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
      if (os.contains("win")) return "windows";
      if (os.contains("mac")) return "macos";
      if (os.contains("lin")) return "linux";
      return os;
    }

    static String computeOsArch() {
      return System.getProperty("os.arch").toLowerCase(Locale.ROOT);
    }

    static Properties loadProperties(Path file) {
      var properties = new Properties();
      try {
        properties.load(new StringReader(Files.readString(file)));
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
      return properties;
    }
  }
}
