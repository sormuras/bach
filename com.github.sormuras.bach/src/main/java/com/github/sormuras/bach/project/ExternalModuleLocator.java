package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.OperatingSystem;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** An external module locator links module names to their remote locations. */
@FunctionalInterface
public interface ExternalModuleLocator {

  String locate(String module);

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
  }

  record PropertiesBasedModuleLocator(Properties properties) implements ExternalModuleLocator {

    public static ExternalModuleLocator of(Path base) {
      if (Files.notExists(base)) throw new IllegalArgumentException("Base must exist: " + base);
      return new PropertiesBasedModuleLocator(loadProperties(base));
    }

    @Override
    public String locate(String module) {
      var os = OperatingSystem.SYSTEM;
      var key = module + '|' + os.name();
      {
        var location = properties.getProperty(key + '-' + os.architecture());
        if (location != null) return location;
      }
      {
        var location = properties.getProperty(key);
        if (location != null) return location;
      }
      return properties.getProperty(module);
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
