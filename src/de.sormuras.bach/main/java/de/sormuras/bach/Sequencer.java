/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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

import de.sormuras.bach.internal.Modules;
import java.io.File;
import java.lang.module.FindException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** A task factory building the default build sequence. */
public /*static*/ class Sequencer {

  private final Project project;

  public Sequencer(Project project) {
    this.project = project;
  }

  public Task newBuildSequence() {
    var tasks = new ArrayList<Task>();
    tasks.add(new Task.ResolveMissingThirdPartyModules());
    for (var realm : project.realms()) {
      tasks.add(newJavacTask(realm));
      for (var unit : realm.units().values()) tasks.add(newJarTask(realm, unit));

      // TODO tasks.addAll(realm.tasks());
    }
    return Task.sequence("Build Sequence", tasks);
  }

  Task newJavacTask(Project.Realm realm) {
    var arguments =
        new Arguments()
            .put("--module", String.join(",", realm.units().keySet()))
            .put("-d", project.base().classes(realm.name()));

    var modulePaths = Helper.modulePaths(project, realm);
    if (!modulePaths.isEmpty()) arguments.put("--module-path", modulePaths);

    Helper.putModuleSourcePaths(arguments, realm);
    Helper.putModulePatches(arguments, project, realm);

    if (realm.flags().contains(Project.Realm.Flag.ENABLE_PREVIEW_LANGUAGE_FEATURES)) {
      arguments.put("--enable-preview");
      arguments.put("--release", Runtime.version().feature());
    }

    return new Task.RunTool(
        "Compile sources of " + realm.toLabelName() + " realm",
        ToolProvider.findFirst("javac").orElseThrow(),
        arguments.toStringArray());
  }

  Task newJarTask(Project.Realm realm, Project.Unit unit) {
    var base = project.base();
    var module = unit.toName();

    var classes = base.classes(realm.name(), module);
    var modules = base.modules(realm.name());
    var jar = modules.resolve(module + "@" + project.info().version() + ".jar");

    var arguments = new Arguments().put("--create").put("--file", jar);
    unit.descriptor().mainClass().ifPresent(main -> arguments.put("--main-class", main));
    arguments.put("-C", classes, ".");

    return Task.sequence(
        "Create modular JAR file " + jar.getFileName(),
        new Task.CreateDirectories(jar.getParent()),
        new Task.RunTool(
            "Package classes of module " + module,
            ToolProvider.findFirst("jar").orElseThrow(),
            arguments.toStringArray()));
  }

  /** A mutable argument collection builder. */
  public static class Arguments {
    private final Map<String, List<Object>> namedOptions = new LinkedHashMap<>();
    private final List<Object> additionalArguments = new ArrayList<>();

    public Arguments add(Object... arguments) {
      this.additionalArguments.addAll(List.of(arguments));
      return this;
    }

    public Arguments put(String option, Object... values) {
      namedOptions.put(option, List.of(values));
      return this;
    }

    public Arguments put(String option, Collection<Path> paths) {
      return put(option, "", paths);
    }

    public Arguments put(String option, String prefix, Collection<Path> paths) {
      var path = paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
      namedOptions.put(option, List.of(prefix + path));
      return this;
    }

    public String[] toStringArray() {
      var list = new ArrayList<String>();
      for (var entry : namedOptions.entrySet()) {
        list.add(entry.getKey());
        for (var value : entry.getValue()) list.add(value.toString());
      }
      for (var additional : additionalArguments) list.add(additional.toString());
      return list.toArray(String[]::new);
    }
  }

  /** A static helper. */
  public interface Helper {

    static List<Path> modulePaths(Project project, Project.Realm realm) {
      var base = project.base();
      var lib = base.lib();
      var paths = new ArrayList<Path>();
      for (var upstream : realm.upstreams()) paths.add(base.modules(upstream));
      if (Files.isDirectory(lib) || !project.toExternalModuleNames().isEmpty()) paths.add(lib);
      return List.copyOf(paths);
    }

    /** Compute module-relevant source path for the given unit. */
    static List<Path> relevantSourcePaths(Project.Unit unit) {
      var sources = unit.sources();
      var p0 = sources.get(0).path();
      if (sources.size() == 1 || Files.exists(p0.resolve("module-info.java"))) return List.of(p0);
      for (var source : sources) {
        var pN = source.path();
        if (Files.exists(pN.resolve("module-info.java"))) return List.of(p0, pN);
      }
      throw new IllegalStateException("No module-info.java found in: " + sources);
    }

    static void putModuleSourcePaths(Arguments arguments, Project.Realm realm) {
      var patterns = new TreeSet<String>(); // "src:etc/*/java"
      var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
      for (var unit : realm.units().values()) {
        var sourcePaths = Helper.relevantSourcePaths(unit);
        try {
          for (var path : sourcePaths) patterns.add(Modules.modulePatternForm(path, unit.toName()));
        } catch (FindException e) {
          specific.put(unit.toName(), sourcePaths);
        }
      }
      if (!patterns.isEmpty())
        arguments.put("--module-source-path", String.join(File.pathSeparator, patterns));
      if (specific.isEmpty()) return;
      for (var entry : specific.entrySet())
        arguments.put("--module-source-path", entry.getKey() + "=", entry.getValue());
    }

    static Map<String, List<Path>> patches(
        Collection<Project.Unit> units, List<Project.Realm> upstreams) {
      if (units.isEmpty() || upstreams.isEmpty()) return Map.of();
      var patches = new TreeMap<String, List<Path>>();
      for (var unit : units) {
        var module = unit.toName();
        for (var upstream : upstreams)
          upstream.units().values().stream()
              .filter(up -> up.toName().equals(module))
              .findAny()
              .ifPresent(up -> patches.put(module, List.of(up.sources().get(0).path())));
      }
      return patches;
    }

    static void putModulePatches(Arguments arguments, Project project, Project.Realm realm) {
      var upstreams = new ArrayList<>(project.realms());
      upstreams.removeIf(candidate -> !realm.upstreams().contains(candidate.name()));
      var patches = patches(realm.units().values(), upstreams);
      if (patches.isEmpty()) return;
      for (var patch : patches.entrySet())
        arguments.put("--patch-module", patch.getKey() + "=", patch.getValue());
    }
  }
}
