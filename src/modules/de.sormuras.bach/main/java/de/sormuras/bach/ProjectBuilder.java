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

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/*BODY*/
/** Build project. */
public /*STATIC*/ class ProjectBuilder {

  /** Supported properties. */
  public enum Property {
    /** Name of the project. */
    NAME("project"),

    /** Version of the project, consumable by {@link Version#parse(String)}. */
    VERSION("0"),

    /** Directory that contains all modules. */
    SRC_PATH("src");

    public final String key;
    public final String defaultValue;

    Property(String defaultValue) {
      this.key = name().replace('_', '-').toLowerCase();
      this.defaultValue = defaultValue;
    }
  }

  /** Create default project scanning the passed base directory. */
  public static Project build(Path base) {
    if (!Files.isDirectory(base)) {
      throw new IllegalArgumentException("Expected a directory but got: " + base);
    }
    return new Scanner(base).project();
  }

  static class Scanner {

    private final Path base;
    private final Properties properties;

    Scanner(Path base) {
      this.base = base;
      this.properties = Util.load(new Properties(), base.resolve(".bach").resolve(".properties"));
    }

    String get(Property property) {
      return get(property, property.defaultValue);
    }

    String get(Property property, String defaultValue) {
      return System.getProperty(property.key, properties.getProperty(property.key, defaultValue));
    }

    Project.ModuleInfo info(Path path) {
      for (var directory : List.of("java", "module")) {
        var info = path.resolve(directory).resolve("module-info.java");
        if (Util.isModuleInfo(info)) {
          return Project.ModuleInfo.of(info);
        }
      }
      throw new IllegalArgumentException("Couldn't find module-info.java file in: " + path);
    }

    List<Project.ModuleUnit> units(Path src, String realm) {
      var units = new ArrayList<Project.ModuleUnit>();
      for (var module : Util.list(src, Files::isDirectory)) {
        var path = module.resolve(realm);
        if (Files.notExists(path)) {
          continue;
        }
        // jigsaw
        if (Files.isDirectory(path.resolve("java"))) {
          var info = info(path);
          var sources = List.of(Project.Source.of(path.resolve("java")));
          var resources = Util.findExistingDirectories(List.of(path.resolve("resources")));
          var mavenPom = path.resolve("maven").resolve("pom.xml");
          units.add(new Project.ModuleUnit(info, sources, resources, mavenPom));
          continue;
        }
        // multi-release
        if (!Util.list(path, "java-*").isEmpty()) {
          Project.ModuleInfo info = null;
          var sources = new ArrayList<Project.Source>();
          for (int feature = 7; feature <= Runtime.version().feature(); feature++) {
            var sourced = path.resolve("java-" + feature);
            if (Files.notExists(sourced)) {
              continue;
            }
            sources.add(Project.Source.of(sourced, feature));
            var infoPath = sourced.resolve("module-info.java");
            if (info == null && Util.isModuleInfo(infoPath)) { // select first
              info = Project.ModuleInfo.of(infoPath);
            }
          }
          var resources = Util.findExistingDirectories(List.of(path.resolve("resources")));
          var mavenPom = path.resolve("maven").resolve("pom.xml");
          units.add(new Project.ModuleUnit(info, sources, resources, mavenPom));
          continue;
        }
        throw new IllegalStateException("Failed to scan module: " + module);
      }
      return units;
    }

    Project.Realm realm(String name, Project.Realm... realms) {
      var units = units(base.resolve(get(Property.SRC_PATH)), name);
      return Project.Realm.of(name, units, realms);
    }

    Project project() {
      var main = realm("main");
      var test = realm("test", main);
      return new Project(
          base,
          base.resolve("bin"),
          get(Property.NAME, Util.findFileName(base).orElse(Property.NAME.defaultValue)),
          Version.parse(get(Property.VERSION)),
          new Project.Library(base.resolve("lib")),
          List.of(main, test));
    }
  }
}
