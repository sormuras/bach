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

package de.sormuras.bach.execution;

import de.sormuras.bach.api.Project;
import de.sormuras.bach.api.Realm;
import de.sormuras.bach.api.Unit;
import java.io.PrintWriter;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Provide test launch support. */
abstract /*static*/ class TestLauncher implements ToolProvider, GarbageCollect, Scribe {

  /** Test launcher running provided test tools. */
  static class ToolTester extends TestLauncher {

    ToolTester(Project project, Realm realm, Unit unit) {
      super("test(" + unit.name() + ")", project, realm, unit);
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      return tools()
          .filter(tool -> name().equals(tool.name()))
          .mapToInt(tool -> Math.abs(run(tool, out, err, args)))
          .sum();
    }
  }

  /** Test launcher running JUnit Platform. */
  static class JUnitTester extends TestLauncher {

    JUnitTester(Project project, Realm realm, Unit unit) {
      super("junit", project, realm, unit);
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      var junit = tools().filter(tool -> "junit".equals(tool.name())).findFirst();
      if (junit.isEmpty()) {
        out.println("Tool named 'junit' not found");
        return 0;
      }
      return run(junit.get(), out, err, args);
    }
  }

  /** Return module layer finding modules in the specified paths. */
  static ModuleLayer layer(List<Path> paths, String module) {
    var finder = ModuleFinder.of(paths.toArray(Path[]::new));
    var boot = ModuleLayer.boot();
    var roots = List.of(module);
    try {
      var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
      var loader = ClassLoader.getPlatformClassLoader();
      var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), loader);
      return controller.layer();
    } catch (FindException e) {
      var joiner = new StringJoiner(System.lineSeparator());
      joiner.add(e.getMessage());
      joiner.add("Module path:");
      paths.forEach(path -> joiner.add("\t" + path.toString()));
      joiner.add("Finder finds module(s):");
      finder.findAll().stream()
          .sorted(Comparator.comparing(ModuleReference::descriptor))
          .forEach(reference -> joiner.add("\t" + reference));
      joiner.add("");
      throw new RuntimeException(joiner.toString(), e);
    }
  }

  private final String name;
  private final Project project;
  private final Realm realm;
  private final Unit unit;

  TestLauncher(String name, Project project, Realm realm, Unit unit) {
    this.name = name;
    this.project = project;
    this.realm = realm;
    this.unit = unit;
  }

  @Override
  public String name() {
    return name;
  }

  Stream<ToolProvider> tools() {
    var modulePath = new ArrayList<Path>();
    modulePath.add(project.toModularJar(realm, unit)); // test module first
    modulePath.addAll(realm.modulePaths(project.paths())); // compiled requires next, like "main"
    modulePath.add(project.paths().modules(realm)); // same realm last, like "test"
    var layer = layer(modulePath, unit.name());
    var tools = ServiceLoader.load(layer, ToolProvider.class);
    return StreamSupport.stream(tools.spliterator(), false);
  }

  int run(ToolProvider tool, PrintWriter out, PrintWriter err, String... args) {
    var toolLoader = tool.getClass().getClassLoader();
    var currentThread = Thread.currentThread();
    var currentContextLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(toolLoader);
    try {
      return tool.run(out, err, args);
    } finally {
      currentThread.setContextClassLoader(currentContextLoader);
    }
  }

  @Override
  public Snippet toSnippet() {
    return Snippet.of("// TODO Launch " + $(name()) + " in class " + getClass().getSimpleName());
  }
}
