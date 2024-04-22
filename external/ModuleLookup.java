/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.external;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import run.bach.internal.PathSupport;

/** Connects zero or more module names to their remote locations. */
@FunctionalInterface
public interface ModuleLookup {
  String lookup(String name, OperatingSystem os);

  default String lookup(String name) {
    return lookup(name, OperatingSystem.CURRENT);
  }

  default String description() {
    return getClass().getSimpleName();
  }

  static ModuleLookup ofProperties(Path file) {
    var properties = PathSupport.properties(file);
    var annotation =
        Optional.ofNullable(properties.remove("@description"))
            .map(Object::toString)
            .orElse(file.getFileName().toString().replace(".properties", ""));
    var modules = new TreeSet<String>();
    for (var name : properties.stringPropertyNames()) {
      if (name.startsWith("@")) {
        properties.remove(name);
        continue;
      }
      var index = name.indexOf('|');
      modules.add(index <= 0 ? name : name.substring(0, index));
    }
    var uri = file.toUri();
    var description = "%s [%d/%d] %s".formatted(annotation, modules.size(), properties.size(), uri);
    return new PropertiesLookup(description, properties);
  }

  record PropertiesLookup(String description, Properties properties) implements ModuleLookup {
    @Override
    public String lookup(String module, OperatingSystem os) {
      var moduleAndOperatingSystem = module + '|' + os.name();
      {
        var location = properties.getProperty(moduleAndOperatingSystem + '-' + os.architecture());
        if (location != null) return location;
      }
      {
        var location = properties.getProperty(moduleAndOperatingSystem);
        if (location != null) return location;
      }
      return properties.getProperty(module);
    }
  }
}
