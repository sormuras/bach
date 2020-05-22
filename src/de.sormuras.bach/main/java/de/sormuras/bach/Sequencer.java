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
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
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
  private final Tuner tuner;

  public Sequencer(Project project, Tuner tuner) {
    this.project = project;
    this.tuner = tuner;
  }

  public Task newBuildSequence() {
    var tasks = new ArrayList<Task>();
    tasks.add(new Task.ResolveMissingThirdPartyModules());
    for (var realm : project.realms()) {
      // javac + jar = compile
      tasks.add(newJavacTask(realm));
      tasks.add(newJarTask(realm));
      // javadoc
      if (realm.flags().contains(Project.Realm.Flag.CREATE_API_DOCUMENTATION))
        tasks.add(newJavadocTask(realm));
      // jlink
      if (realm.flags().contains(Project.Realm.Flag.CREATE_CUSTOM_RUNTIME_IMAGE))
        tasks.add(newJLinkTask(realm));
      // test(${MODULE}) | junit = test
      if (realm.flags().contains(Project.Realm.Flag.LAUNCH_TESTS)) tasks.add(newTestsTask(realm));
    }
    return Task.sequence("Build Sequence", tasks);
  }

  Task newJavacTask(Project.Realm realm) {
    var classes = project.base().classes(realm.name());
    var arguments = Helper.newModuleArguments(project, realm).put("-d", classes);
    tuner.tune(arguments, project, Tuner.context("javac", realm));
    return new Task.RunTool(
        "Compile sources of " + realm.toLabelName() + " realm",
        ToolProvider.findFirst("javac").orElseThrow(),
        arguments.toStringArray());
  }

  Task newJarTask(Project.Realm realm) {
    var tasks = new ArrayList<Task>();
    tasks.add(new Task.CreateDirectories(project.base().modules(realm.name())));
    tasks.add(new Task.CreateDirectories(project.base().sources(realm.name())));
    for (var unit : realm.units().values()) tasks.add(newJarTask(realm, unit));
    return Task.sequence("Create JAR files of " + realm.toLabelName() + " realm", tasks);
  }

  Task newJarTask(Project.Realm realm, Project.Unit unit) {
    var tasks = new ArrayList<Task>();
    var module = unit.toName();
    var tool = ToolProvider.findFirst("jar").orElseThrow();
    var base = project.base();
    {
      var file = Helper.jar(project, realm, module);
      var classes = base.classes(realm.name(), module);
      var arguments = new Arguments().put("--create").put("--file", file);
      unit.descriptor().mainClass().ifPresent(main -> arguments.put("--main-class", main));
      arguments.add("-C", classes, ".");
      unit.resources().forEach(resource -> arguments.add("-C", resource, "."));
      tuner.tune(arguments, project, Tuner.context("jar", realm, module));
      var args = arguments.toStringArray();
      tasks.add(new Task.RunTool("Package classes of module " + module, tool, args));
    }
    {
      var version = project.info().version();
      var file = base.sources(realm.name()).resolve(module + "@" + version + "-sources.jar");
      var arguments = new Arguments().put("--create").put("--file", file).put("--no-manifest");
      var sources = new ArrayDeque<>(unit.sources());
      arguments.add("-C", sources.removeFirst().path(), "."); // API-defining "base" source
      for (var source : sources) {
        if (source.release() >= 9) arguments.add("--release", source.release());
        arguments.add("-C", source.path(), ".");
      }
      // unit.resources().forEach(resource -> arguments.add("-C", resource, "."));
      tuner.tune(arguments, project, Tuner.context("jar", realm, module));
      var args = arguments.toStringArray();
      tasks.add(new Task.RunTool("Package sources of module " + module, tool, args));
    }
    return Task.sequence("Create JAR files of " + realm.toLabelName() + " realm", tasks);
  }

  Task newJavadocTask(Project.Realm realm) {
    var arguments = Helper.newModuleArguments(project, realm).put("-d", project.base().api());
    tuner.tune(arguments, project, Tuner.context("javadoc", realm));
    return new Task.RunTool(
        "Generate API documentation for " + realm.toLabelName() + " realm",
        ToolProvider.findFirst("javadoc").orElseThrow(),
        arguments.toStringArray());
  }

  Task newJLinkTask(Project.Realm realm) {
    var base = project.base();
    var modulePaths = new ArrayList<Path>();
    modulePaths.add(base.modules(realm.name()));
    modulePaths.addAll(Helper.modulePaths(project, realm));
    var automaticModules =
        ModuleFinder.of(modulePaths.toArray(Path[]::new)).findAll().stream()
            .map(ModuleReference::descriptor)
            .filter(ModuleDescriptor::isAutomatic)
            .collect(Collectors.toList());
    if (!automaticModules.isEmpty()) return Task.sequence("Automatic module: " + automaticModules);
    var units = realm.units();
    var mainModule = Modules.findMainModule(units.values().stream().map(Project.Unit::descriptor));
    var arguments =
        new Arguments()
            .put("--add-modules", String.join(",", units.keySet()))
            .put("--module-path", modulePaths)
            .put("--output", base.image());
    if (mainModule.isPresent()) {
      var module = mainModule.get();
      var launcher = Path.of(module.replace('.', '/')).getFileName().toString();
      arguments.put("--launcher", launcher + '=' + module);
    }
    tuner.tune(arguments, project, Tuner.context("jlink", realm));
    return Task.sequence(
        "Create custom runtime image",
        new Task.DeleteDirectories(base.image()),
        new Task.RunTool(
            "jlink", ToolProvider.findFirst("jlink").orElseThrow(), arguments.toStringArray()));
  }

  Task newTestsTask(Project.Realm realm) {
    var base = project.base();
    var tasks = new ArrayList<Task>();
    for (var unit : realm.units().values()) {
      var module = unit.toName();
      var jar = Helper.jar(project, realm, module);
      var modulePaths = new ArrayList<Path>();
      modulePaths.add(jar);
      modulePaths.addAll(Helper.modulePaths(project, realm));
      modulePaths.add(base.modules(realm.name()));
      var arguments = new Arguments().put("--select-module", module);
      tuner.tune(arguments, project, Tuner.context("junit", realm, module));
      tasks.add(new Task.RunTestModule(module, modulePaths, arguments.toStringArray()));
    }
    return Task.sequence("Launch all tests located in " + realm.toLabelName() + " realm", tasks);
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

    static Arguments newModuleArguments(Project project, Project.Realm realm) {
      var arguments = new Arguments().put("--module", String.join(",", realm.units().keySet()));
      var modulePaths = Helper.modulePaths(project, realm);
      if (!modulePaths.isEmpty()) arguments.put("--module-path", modulePaths);
      Helper.putModuleSourcePaths(arguments, realm);
      Helper.putModulePatches(arguments, project, realm);
      if (realm.flags().contains(Project.Realm.Flag.ENABLE_PREVIEW_LANGUAGE_FEATURES)) {
        arguments.put("--enable-preview");
        arguments.put("--release", Runtime.version().feature());
      }
      return arguments;
    }

    static List<Path> modulePaths(Project project, Project.Realm realm) {
      var base = project.base();
      var lib = base.lib();
      var paths = new ArrayList<Path>();
      for (var upstream : realm.upstreams()) paths.add(base.modules(upstream));
      if (Files.isDirectory(lib) || !project.toExternalModuleNames().isEmpty()) paths.add(lib);
      return List.copyOf(paths);
    }

    static Path jar(Project project, Project.Realm realm, String module) {
      var modules = project.base().modules(realm.name());
      return modules.resolve(module + "@" + project.info().version() + ".jar");
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

  /** An arguments tuner invoked by a {@link Sequencer} instance. */
  @FunctionalInterface
  public interface Tuner {

    static Map<String, String> context(String tool, Project.Realm realm) {
      return Map.of("tool", tool, "realm", realm.name());
    }

    static Map<String, String> context(String tool, Project.Realm realm, String module) {
      return Map.of("tool", tool, "realm", realm.name(), "module", module);
    }

    void tune(Arguments arguments, Project project, Map<String, String> context);

    static void defaults(Arguments arguments, Project project, Map<String, String> context) {
      switch (context.get("tool")) {
        case "javac":
          arguments.put("-encoding", "UTF-8");
          arguments.put("-parameters");
          arguments.put("-Werror");
          arguments.put("-X" + "lint");
          break;
        case "javadoc":
          arguments.put("-encoding", "UTF-8");
          arguments.put("-locale", "en");
          break;
        case "jlink":
          arguments.put("--compress", "2");
          arguments.put("--no-header-files");
          arguments.put("--no-man-pages");
          arguments.put("--strip-debug");
          break;
        case "junit":
          var module = context.get("module");
          var target = project.base().workspace("junit-reports", module);
          arguments.put("--disable-ansi-colors");
          arguments.put("--reports-dir", target);
          break;
      }
    }
  }
}
