/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach;

import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

/*BODY*/
/** Project data. */
public /*STATIC*/ class Project {

  /** Project property enumeration. */
  public enum Property {
    NAME(
        "unnamed",
        "Name of the project. Determines some file names, like main documentation JAR file."),
    VERSION(
        "1.0.0-SNAPSHOT",
        "Version of the project. Passed to '--module-version' and other options."),
    MODULES("*", "List of modules to build. '*' means all in PATH_SRC."),
    PATH_BIN("bin", "Destination directory to store binary assets to."),
    PATH_LIB("lib", "Root directory of 3rd-party modules."),
    PATH_SRC("src", "This directory contains all Java module sources."),
    ;

    final String key;
    final String defaultValue;
    final String description;

    Property(String defaultValue, String description) {
      this.key = name().toLowerCase().replace('_', '.');
      this.defaultValue = defaultValue;
      this.description = description;
    }

    String get(Properties properties) {
      return get(properties, defaultValue);
    }

    String get(Properties properties, String defaultValue) {
      return properties.getProperty(key, defaultValue);
    }
  }

  public static Project of(Path home, Path work) {
    return new Project(home, work);
  }

  final Properties properties;
  final String name;
  final String version;

  final Path bin;
  final Path lib;
  final Path src;

  private Project(Path home, Path work) {
    this.properties = Run.newProperties(home);
    this.name = Property.NAME.get(properties, "" + home.toAbsolutePath().normalize().getFileName());
    this.version = Property.VERSION.get(properties);

    this.bin = work.resolve(path(Property.PATH_BIN));
    this.lib = home.resolve(path(Property.PATH_LIB));
    this.src = home.resolve(path(Property.PATH_SRC));
  }

  String get(Property property) {
    return property.get(properties);
  }

  String get(String key) {
    return properties.getProperty(key);
  }

  String get(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  List<String> modules(String realm) {
    return modules(realm, get(Property.MODULES), src);
  }

  static List<String> modules(String realm, String userDefinedModules, Path sourceDirectory) {
    if ("*".equals(userDefinedModules)) {
      // Find modules for "src/.../*/${realm}/java"
      if (Files.notExists(sourceDirectory)) {
        return List.of();
      }
      var modules = new ArrayList<String>();
      var descriptor = Path.of(realm, "java", "module-info.java");
      DirectoryStream.Filter<Path> filter =
          path -> Files.isDirectory(path) && Files.exists(path.resolve(descriptor));
      try (var stream = Files.newDirectoryStream(sourceDirectory, filter)) {
        stream.forEach(directory -> modules.add(directory.getFileName().toString()));
      } catch (Exception e) {
        throw new Error("Scanning directory for modules failed: " + e);
      }
      return modules;
    }
    var modules = userDefinedModules.split(",");
    for (int i = 0; i < modules.length; i++) {
      modules[i] = modules[i].strip();
    }
    return List.of(modules);
  }

  List<Path> modulePath(String realm, String phase, String... requiredRealms) {
    var result = new ArrayList<Path>();
    if ("runtime".equals(phase)) {
      result.add(bin.resolve(realm).resolve("modules"));
    }
    var candidates = List.of(realm, realm + "-" + phase + "-junit", realm + "-" + phase + "-only");
    for (var candidate : candidates) {
      result.add(lib.resolve(candidate));
    }
    for (var required : requiredRealms) {
      if (realm.equals(required)) {
        throw new IllegalArgumentException("Cyclic realm dependency detected: " + realm);
      }
      bin.resolve(required).resolve("modules");
      result.addAll(modulePath(required, phase));
    }
    result.removeIf(Files::notExists);
    return result;
  }

  Path path(Property property) {
    var value = get(property);
    try {
      return Path.of(URI.create(value));
    } catch (IllegalArgumentException e) {
      // fall-through
    }
    return Path.of(value);
  }

  @Override
  public String toString() {
    return name + ' ' + version;
  }

  void toStrings(Consumer<String> consumer) {
    var skips = Set.of("name", "version");
    consumer.accept("name = " + name);
    consumer.accept("version = " + version);
    for (var property : Property.values()) {
      var key = property.key;
      if (skips.contains(key)) {
        continue;
      }
      consumer.accept(key + " = " + get(property));
    }
  }
}
