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

import de.sormuras.bach.call.GenericSourcesConsumer;
import de.sormuras.bach.call.Jar;
import de.sormuras.bach.call.Javac;
import de.sormuras.bach.call.Javadoc;
import de.sormuras.bach.call.Jlink;
import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.Paths;
import java.lang.module.FindException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** A builder for building {@link Project.Builder} objects by parsing a directory for modules. */
public /*static*/ class Walker {

  private Project.Base base = Project.Base.of();
  private List<Path> moduleInfoFiles = new ArrayList<>();
  private int walkDepthLimit = 9;
  private Tuner tuner = Tuner::defaults;

  public Walker setBase(Path directory) {
    return setBase(Project.Base.of(directory));
  }

  public Walker setBase(Project.Base base) {
    this.base = base;
    return this;
  }

  public Walker setModuleInfoFiles(List<Path> moduleInfoFiles) {
    this.moduleInfoFiles = moduleInfoFiles;
    return this;
  }

  public Walker setWalkDepthLimit(int walkDepthLimit) {
    this.walkDepthLimit = walkDepthLimit;
    return this;
  }

  public Walker setTuner(Tuner tuner) {
    this.tuner = tuner;
    return this;
  }

  public Project.Builder newBuilder() {
    var builder = Project.builder();
    builder.setBase(base);
    var directoryName = base.directory().toAbsolutePath().getFileName();
    if (directoryName != null) builder.title(directoryName.toString());
    builder.setRealms(computeRealms());
    return builder;
  }

  List<Project.Realm> computeRealms() {
    if (moduleInfoFiles.isEmpty()) walkDirectoryTreePopulatingModuleInfoFiles();
    try {
      return computeMainTestPreviewRealms();
    } catch (UnsupportedOperationException ignored) {
    }
    return computeUnnamedRealm();
  }

  List<Project.Realm> computeUnnamedRealm() {
    var unnamedRealm = new RealmBuilder("");
    unnamedRealm.moduleInfoFiles.addAll(moduleInfoFiles);
    unnamedRealm.flags.addAll(EnumSet.allOf(Flag.class));
    return List.of(unnamedRealm.build());
  }

  List<Project.Realm> computeMainTestPreviewRealms() {
    throw new UnsupportedOperationException();
  }

  void walkDirectoryTreePopulatingModuleInfoFiles() {
    var directory = base.directory();
    if (Paths.isRoot(directory)) throw new IllegalStateException("Root directory: " + directory);
    var paths = Paths.find(List.of(directory), walkDepthLimit, Paths::isModuleInfoJavaFile);
    moduleInfoFiles.addAll(paths);
  }

  /** A tool call arguments tuner used by {@link Walker}s. */
  @FunctionalInterface
  public interface Tuner {

    void tune(Call call, Map<String, String> context);

    static void defaults(Call call, @SuppressWarnings("unused") Map<String, String> context) {
      if (call instanceof GenericSourcesConsumer) {
        var consumer = (GenericSourcesConsumer<?>) call;
        consumer.setCharacterEncodingUsedBySourceFiles("UTF-8");
      }
      if (call instanceof Javac) {
        var javac = (Javac) call;
        javac.setGenerateMetadataForMethodParameters(true);
        javac.setTerminateCompilationIfWarningsOccur(true);
        javac.getAdditionalArguments().add("-X" + "lint");
      }
      if (call instanceof Javadoc) {
        var javadoc = (Javadoc) call;
        javadoc.getAdditionalArguments().add("-locale", "en");
      }
      if (call instanceof Jlink) {
        var jlink = (Jlink) call;
        jlink.getAdditionalArguments().add("--compress", "2");
        jlink.getAdditionalArguments().add("--no-header-files");
        jlink.getAdditionalArguments().add("--no-man-pages");
        jlink.getAdditionalArguments().add("--strip-debug");
      }
    }
  }

  enum Flag {
    CREATE_API_DOCUMENTATION,
    CREATE_CUSTOM_RUNTIME_IMAGE,
    LAUNCH_TESTS
  }

  /** A builder for building {@link Project.Realm} objects. */
  class RealmBuilder {

    final String name;
    final Set<Flag> flags = new HashSet<>();
    final List<Path> moduleInfoFiles = new ArrayList<>();
    final List<Project.Realm> upstreams = new ArrayList<>();

    class Units {
      Map<String, Project.Unit> map = new TreeMap<>();

      Set<String> moduleNames = new TreeSet<>();
      Set<String> moduleSourcePathPatterns = new TreeSet<>();
      Map<String, List<Path>> moduleSourcePathsPerModule = new TreeMap<>();

      Map<String, List<Path>> patches() {
        if (moduleNames.isEmpty() || upstreams.isEmpty()) return Map.of();
        var patches = new TreeMap<String, List<Path>>();
        for (var module : moduleNames) {
          for (var upstream : upstreams)
            // TODO other.paths()
            upstream.unit(module).ifPresent(other -> patches.put(module, List.of()));
        }
        return patches;
      }
    }

    RealmBuilder(String name) {
      this.name = Objects.requireNonNull(name, "name");
    }

    Project.Realm build() {
      var units = computeUnits();
      var javac = computeJavac(units);
      var tasks = computeTasks();
      return new Project.Realm(name, units.map, javac, tasks);
    }

    Units computeUnits() {
      var units = new Units();
      for (var info : moduleInfoFiles) {
        var descriptor = Modules.describe(info);
        var module = descriptor.name();
        units.moduleNames.add(module);
        try {
          units.moduleSourcePathPatterns.add(Modules.modulePatternForm(info, descriptor.name()));
        } catch (FindException e) {
          units.moduleSourcePathsPerModule.put(module, List.of(info.getParent()));
        }

        var classes = base.classes(name, module);
        var modules = base.modules(name);
        var jar = modules.resolve(module + ".jar");
        var context = Map.of("realm", name, "module", module);
        var jarCreate = new Jar();
        var jarCreateArgs = jarCreate.getAdditionalArguments();
        jarCreateArgs.add("--create").add("--file", jar);
        descriptor.mainClass().ifPresent(main -> jarCreateArgs.add("--main-class", main));
        jarCreateArgs.add("-C", classes, ".");
        tuner.tune(jarCreate, context);
        var jarDescribe = new Jar();
        jarDescribe.getAdditionalArguments().add("--describe-module").add("--file", jar);
        tuner.tune(jarDescribe, context);
        var task =
            Task.sequence(
                "Create modular JAR file " + jar.getFileName(),
                new Task.CreateDirectories(jar.getParent()),
                jarCreate.toTask(),
                jarDescribe.toTask());

        units.map.put(module, new Project.Unit(descriptor, List.of(task)));
      }
      return units;
    }

    Javac computeJavac(Units units) {
      var namesOfUpstreams =
          upstreams.stream().map(Project.Realm::name).collect(Collectors.toList());
      return new Javac()
          .setModules(units.moduleNames)
          .setPatternsWhereToFindSourceFiles(new ArrayList<>(units.moduleSourcePathPatterns))
          .setPathsWhereToFindSourceFiles(units.moduleSourcePathsPerModule)
          .setPathsWhereToFindMoreAssetsPerModule(units.patches())
          .setPathsWhereToFindApplicationModules(base.modulePaths(namesOfUpstreams))
          .setDestinationDirectory(base.classes(name));
    }

    List<Task> computeTasks() {
      // TODO javadoc
      // TODO jlink
      return List.of();
    }
  }
}
