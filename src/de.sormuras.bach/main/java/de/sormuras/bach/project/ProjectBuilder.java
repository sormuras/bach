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

package de.sormuras.bach.project;

import de.sormuras.bach.Log;
import de.sormuras.bach.util.Modules;
import de.sormuras.bach.util.Paths;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;

public class ProjectBuilder {

  /** Name of the project declaration "source unit". */
  private static final String PROPERTIES = "project-info.java.properties";

  /** Project property enumeration. */
  private enum Property {
    NAME("project"),
    VERSION("0"),

    REALM_MAIN_JAVAC_ARGS(
        String.join("|", "-encoding", "UTF-8", "-parameters", "-W" + "error", "-X" + "lint")),
    REALM_TEST_JAVAC_ARGS(
        String.join(
            "|", "-encoding", "UTF-8", "-parameters", "-W" + "error", "-X" + "lint:-preview")),

    DEPLOYMENT_REPOSITORY_ID(null),
    DEPLOYMENT_URL(null);

    final String key;
    final String defaultValue;

    Property(String defaultValue) {
      this.key = name().toLowerCase().replace('_', '.');
      this.defaultValue = defaultValue;
    }

    public String get(Properties properties) {
      return get(properties, defaultValue);
    }

    public String get(Properties properties, String defaultValue) {
      return properties.getProperty(key, defaultValue);
    }

    public List<String> list(Properties properties, String regex) {
      var value = get(properties);
      if (value.isBlank()) return List.of();
      return Arrays.stream(value.split(regex)).map(String::strip).collect(Collectors.toList());
    }
  }

  private final Log log;

  public ProjectBuilder(Log log) {
    this.log = log;
  }

  /** Create project instance auto-configured by scanning the given base directory. */
  public Project auto(Path base) {
    return auto(Folder.of(base));
  }

  public Project auto(Folder folder) {
    return auto(folder, properties(folder));
  }

  public Project auto(Folder folder, Properties properties) {
    var directory = Paths.name(folder.base(), Property.NAME.defaultValue);
    var name = Property.NAME.get(properties, directory);
    var version = Property.VERSION.get(properties);
    var library = Library.of(properties);
    var structure = structure(folder, library, properties);
    var deployment = deployment(properties);
    return new Project(name, Version.parse(version), structure, deployment);
  }

  public Properties properties(Folder folder) {
    var properties = folder.src(PROPERTIES);
    var file = System.getProperty(PROPERTIES, properties.toString());
    return Paths.load(new Properties(), folder.base().resolve(file));
  }

  public Deployment deployment(Properties properties) {
    var id = Property.DEPLOYMENT_REPOSITORY_ID.get(properties);
    var uri = Property.DEPLOYMENT_URL.get(properties);
    return (id == null || uri == null) ? null : new Deployment(id, URI.create(uri));
  }

  public Structure structure(Folder folder, Library library, Properties properties) {
    if (!Files.isDirectory(folder.base())) {
      throw new IllegalArgumentException("Not a directory: " + folder.base());
    }
    var src = folder.src();
    var moduleFilesInSrc = Paths.find(Set.of(src), Paths::isModuleFile);
    if (moduleFilesInSrc.isEmpty()) throw new IllegalStateException("No module declared: " + src);

    // Simple single realm? All must match: "src/{MODULE}/module-info.java"
    if (moduleFilesInSrc.stream().allMatch(path -> path.getNameCount() == 3)) {
      var realm = new Realm("realm", Set.of(), List.of(src), List.of(folder.lib()), Map.of());
      var units = new ArrayList<Unit>();
      for (var root : Paths.list(src, Files::isDirectory)) {
        log.debug("root = %s", root);
        var module = root.getFileName().toString();
        if (!SourceVersion.isName(module.replace(".", ""))) continue;
        if (Paths.isModuleFile(root.resolve("module-info.java"))) {
          var info = root.resolve("module-info.java");
          var descriptor = Modules.describe(Paths.readString(info));
          var pom = root.resolve("pom.xml");
          var sources = List.of(Source.of(root));
          var unit = new Unit(realm, descriptor, info, pom, sources, List.of(), List.of());
          units.add(unit);
        }
      }
      return new Structure(folder, library, List.of(realm), units);
    }

    // Default "main" and "test" realms...
    var main =
        new Realm(
            "main",
            Set.of(Realm.Modifier.DEPLOY),
            List.of(folder.src("{MODULE}/main/java")),
            List.of(folder.lib()),
            Map.of("javac", Property.REALM_MAIN_JAVAC_ARGS.list(properties, "\\|")));
    var test =
        new Realm(
            "test",
            Set.of(Realm.Modifier.TEST),
            List.of(folder.src("{MODULE}/test/java"), folder.src("{MODULE}/test/module")),
            List.of(folder.modules("main"), folder.lib()),
            Map.of("javac", Property.REALM_TEST_JAVAC_ARGS.list(properties, "\\|")));
    var realms = List.of(main, test);
    log.debug("realms = %s", realms);

    var registry = new TreeMap<String, List<String>>(); // local realm-based module name registry
    realms.forEach(realm -> registry.put(realm.name(), new ArrayList<>()));
    var units = new ArrayList<Unit>();
    for (var root : Paths.list(src, Files::isDirectory)) {
      log.debug("root = %s", root);
      var module = root.getFileName().toString();
      if (!SourceVersion.isName(module.replace(".", ""))) continue;
      log.debug("module = %s", module);
      var moduleFilesInRoot = Paths.find(Set.of(root), Paths::isModuleFile);
      if (moduleFilesInRoot.isEmpty()) continue;
      int mark = units.size();
      if (Files.isDirectory(root.resolve("main"))) {
        var resources = Paths.filterExisting(List.of(root.resolve("main/resources")));
        var unit = unit(root, main, resources, List.of());
        registry.get("main").add(module);
        units.add(unit);
      }
      if (Files.isDirectory(root.resolve("test"))) {
        var resources =
            Paths.filterExisting(
                List.of(root.resolve("test/resources"), root.resolve("main/resources")));
        var patches = new ArrayList<Path>();
        if (registry.get("main").contains(module)) {
          patches.add(root.resolve("main/java"));
        }
        var unit = unit(root, test, resources, patches);
        registry.get("test").add(module);
        units.add(unit);
      }
      if (mark == units.size()) {
        log.warning(
            "Ignoring %s -- it's tree layout is not supported: %s", root, moduleFilesInRoot);
      }
    }
    var names = units.stream().map(Unit::name).collect(Collectors.toSet());
    units.sort(Comparator.comparingLong(unit -> countProjectInternalRequires(unit, names)));
    log.debug("units = %s", units);
    return new Structure(folder, library, realms, units);
  }

  private long countProjectInternalRequires(Unit unit, Set<String> names) {
    return unit.descriptor().requires().stream()
        .filter(requires -> names.contains(requires.name()))
        .count();
  }

  private Path info(Path path) {
    for (var directory : List.of("java", "module")) {
      var info = path.resolve(directory).resolve("module-info.java");
      if (Paths.isJavaFile(info)) return info;
    }
    throw new IllegalArgumentException("Couldn't find module-info.java file in: " + path);
  }

  private Unit unit(Path root, Realm realm, List<Path> resources, List<Path> patches) {
    var module = root.getFileName().toString();
    var relative = root.resolve(realm.name()); // realm-relative
    var pom = relative.resolve("maven/pom.xml");
    log.debug("pom = %s", pom);
    log.debug("resources = %s", resources);
    log.debug("patches = %s", patches);
    // jigsaw
    if (Files.isDirectory(relative.resolve("java"))) { // no trailing "...-${N}"
      var info = info(relative);
      var descriptor = Modules.describe(Paths.readString(info));
      var sources = List.of(Source.of(relative.resolve("java")));
      log.debug("info = %s", info);
      log.debug("descriptor = %s", descriptor);
      log.debug("sources = %s", sources);
      return new Unit(realm, descriptor, info, pom, sources, resources, patches);
    }
    // multi-release
    if (!Paths.list(relative, "java-*").isEmpty()) {
      Path info = null;
      ModuleDescriptor descriptor = null;
      var sources = new ArrayList<Source>();
      for (int feature = 7; feature <= Runtime.version().feature(); feature++) {
        var sourced = relative.resolve("java-" + feature);
        if (Files.notExists(sourced)) continue; // feature
        log.debug("sourced = %s", sourced);
        sources.add(Source.of(sourced, feature));
        var infoPath = sourced.resolve("module-info.java");
        if (info == null && Paths.isJavaFile(infoPath)) { // select first
          info = infoPath;
          descriptor = Modules.describe(Paths.readString(info));
        }
      }
      log.debug("info = %s", info);
      log.debug("descriptor = %s", descriptor);
      log.debug("sources = %s", sources);
      return new Unit(realm, descriptor, info, pom, sources, resources, patches);
    }
    throw new IllegalArgumentException("Unknown unit layout: " + module + " <- " + root.toUri());
  }
}
