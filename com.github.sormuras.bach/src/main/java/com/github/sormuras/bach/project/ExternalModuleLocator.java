package com.github.sormuras.bach.project;

import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

/** An external module locator tries to link a module name to a remote location. */
@FunctionalInterface
public interface ExternalModuleLocator {

  String locate(String module);

  default Optional<String> find(String module) {
    return Optional.ofNullable(locate(module));
  }

  default Map<String, String> findAll() {
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
    public Map<String, String> findAll() {
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
    public Map<String, String> findAll() {
      return map;
    }
  }

  class SormurasBachExternalModulesProperties implements ExternalModuleLocator {

    public static ExternalModuleLocator of(String library, String version, String... classifiers) {
      var url = new StringBuilder();
      url.append("https://github.com/sormuras/bach-external-modules/raw/main/properties");
      url.append('/').append(library);
      url.append('/').append(library).append('@').append(version);
      for (var classifier : classifiers) url.append('-').append(classifier);
      url.append("-modules.properties");
      return new SormurasBachExternalModulesProperties(url.toString());
    }

    private final String url;
    private volatile Properties properties;

    public SormurasBachExternalModulesProperties(String url) {
      this.url = url;
      this.properties = null;
    }

    @Override
    public String caption() {
      return "SormurasBachExternalModulesProperties -> " + url;
    }

    private void ensurePropertiesAreInitialized() {
      if (properties != null) return;
      try (var in = new URL(url).openStream()) {
        var text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        var temp = new Properties();
        temp.load(new StringReader(text));
        properties = temp; // last threads wins...
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }

    @Override
    public String locate(String module) {
      ensurePropertiesAreInitialized();
      return properties.getProperty(module);
    }

    @Override
    public Map<String, String> findAll() {
      ensurePropertiesAreInitialized();
      var map = new TreeMap<String, String>();
      properties.stringPropertyNames().forEach(key -> map.put(key, properties.getProperty(key)));
      return map;
    }
  }
}
