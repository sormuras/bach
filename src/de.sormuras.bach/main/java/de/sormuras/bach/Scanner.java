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
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.project.Base;
import de.sormuras.bach.project.MainSources;
import de.sormuras.bach.project.ModuleSource;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.SourceDirectory;
import de.sormuras.bach.tool.Call;
import de.sormuras.bach.tool.Jar;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import java.io.File;
import java.lang.System.Logger.Level;
import java.lang.module.FindException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/** A scanner creates a {@link Project} object by parsing a directory for Java modules. */
public class Scanner {

  /** A layout defines a directory pattern for organizing {@code module-info.java} files. */
  public enum Layout {
    /** Detect layout on-the-fly. */
    AUTOMATIC,
    /** A single realm. */
    DEFAULT,
    /** By Three They Come: {@code main}, {@code test}, and {@code test-preview}. */
    MAIN_TEST_PREVIEW
  }

  public static List<Path> findModuleInfoJavaFiles(Base base, Path offset, int limit) {
    var directory = base.directory().resolve(offset);
    if (Paths.isRoot(directory)) throw new IllegalStateException("Root directory: " + directory);
    var units = Paths.find(List.of(directory), limit, Paths::isModuleInfoJavaFile);
    if (units.isEmpty()) throw new IllegalStateException("No module-info.java: " + directory);
    return List.copyOf(units);
  }

  private final Logbook logbook;
  private final Base base;
  private final Layout layout;
  private final List<Path> infoFiles;

  public Scanner(Logbook logbook, Base base, Layout layout, List<Path> infoFiles) {
    this.logbook = logbook;
    this.base = base;
    this.layout = layout;
    this.infoFiles = infoFiles;
  }

  public Project scan() {
    logbook.print(Level.DEBUG, "Scan directory %s for modules...", base.directory().toUri());
    var directoryName = base.directory().toAbsolutePath().getFileName();
    var projectName = directoryName != null ? directoryName.toString() : "unnamed";
    var project = Project.of(projectName, "1-ea").with(base);
    return scan(project);
  }

  Project scan(Project project) {
    var release = Runtime.version().feature();
    var version = project.basics().version();
    var main = MainSources.of();
    var moduleNames = new ArrayList<String>();
    for (var info : infoFiles) {
      var unit = ModuleSource.of(info);
      var module = unit.name();
      moduleNames.add(module);
      var file = module + '@' + version + ".jar";
      var jar =
          Jar.of(base.modules("").resolve(file))
              // .with("--verbose")
              .withChangeDirectoryAndIncludeFiles(base.classes("", release, module), ".");
      project = project.with(main = main.with(unit.with(jar)));
    }
    // generate javac call
    var javac =
        Javac.of()
            .with("-d", base.classes("", release))
            .with("--module", String.join(",", moduleNames))
            .with("--module-version", version);
    javac = withModuleSourcePaths(javac, project.main().units().values());
    project = project.with(main = main.with(javac));
    // generate javadoc call
    var javadoc =
        Javadoc.of()
            .with("-d", base.workspace("documentation", "api"))
            .with("--module", String.join(",", moduleNames));
    javadoc = withModuleSourcePaths(javadoc, project.main().units().values());
    project = project.with(main.with(javadoc));
    return project;
  }

  /** Compute module-relevant source path for the given unit. */
  static List<Path> relevantSourcePaths(List<SourceDirectory> sources) {
    var s0 = sources.get(0);
    if (s0.isModuleInfoJavaPresent()) return List.of(s0.path());
    for (var source : sources)
      if (source.isModuleInfoJavaPresent()) return List.of(s0.path(), source.path());
    throw new IllegalStateException("No module-info.java found in: " + sources);
  }

  static <C extends Call<C>> C withModuleSourcePaths(C call, Iterable<ModuleSource> units) {
    var patterns = new TreeSet<String>(); // "src:etc/*/java"
    var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
    for (var unit : units) {
      var sourcePaths = relevantSourcePaths(unit.sources());
      try {
        for (var path : sourcePaths) patterns.add(Modules.modulePatternForm(path, unit.name()));
      } catch (FindException e) {
        specific.put(unit.name(), sourcePaths);
      }
    }
    if (patterns.isEmpty() && specific.isEmpty()) throw new IllegalStateException("");
    if (!patterns.isEmpty())
      call = call.with("--module-source-path", String.join(File.pathSeparator, patterns));
    for (var entry : specific.entrySet())
      call = call.with("--module-source-path", entry.getKey() + "=", entry.getValue());
    return call;
  }
}
