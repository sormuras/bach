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

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/*BODY*/
/** Launch JUnit Platform. */
/*STATIC*/ class Tester {

  private final Bach bach;
  private final Project.Realm test;

  Tester(Bach bach, Project.Realm test) {
    this.bach = bach;
    this.test = test;
  }

  void test() {
    bach.log("Launching all test modules in realm: %s", test.name);
    test(test.names());
  }

  void test(Iterable<String> modules) {
    bach.log("Launching all tests in realm " + test);
    for (var module : modules) {
      bach.log("%n%n%n%s%n%n%n", module);
      var unit = test.unit(module);
      if (unit.isEmpty()) {
        bach.warn("No test module unit available for: %s", module);
        continue;
      }
      test(unit.get());
    }
  }

  private void test(Project.ModuleUnit unit) {
    var target = bach.project.target(test);
    var modulePath = bach.project.modulePaths(target, target.modularJar(unit));
    var layer = layer(modulePath, unit.name());

    var errors = new StringBuilder();
    try {
      errors.append(new ToolProviderTester(layer, unit).test());
      errors.append(new JUnitConsoleTester(layer, unit).test());
    } finally {
      if (Util.isWindows()) {
        System.gc();
        Util.sleep(1234);
      }
    }
    if (errors.toString().replace('0', ' ').isBlank()) {
      return;
    }
    throw new AssertionError("Test run failed!");
  }

  private ModuleLayer layer(List<Path> modulePath, String module) {
    bach.log("Module path:");
    for (var element : modulePath) {
      bach.log("  -> %s", element);
    }
    var finder = ModuleFinder.of(modulePath.toArray(Path[]::new));
    bach.log("Finder finds module(s):");
    finder.findAll().stream()
        .sorted(Comparator.comparing(ModuleReference::descriptor))
        .forEach(reference -> bach.log("  -> %s", reference));
    var roots = List.of(module);
    bach.log("Root module(s):");
    for (var root : roots) {
      bach.log("  -> %s", root);
    }
    var boot = ModuleLayer.boot();
    var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
    var loader = ClassLoader.getPlatformClassLoader();
    var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), loader);
    return controller.layer();
  }

  private int run(ToolProvider tool, String... args) {
    var toolLoader = tool.getClass().getClassLoader();
    var currentThread = Thread.currentThread();
    var currentContextLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(toolLoader);
    var parent = toolLoader;
    while (parent != null) {
      parent.setDefaultAssertionStatus(true);
      parent = parent.getParent();
    }
    try {
      bach.log("Running %s %s", tool.name(), String.join(" ", args));
      return tool.run(bach.out, bach.err, args);
    } finally {
      currentThread.setContextClassLoader(currentContextLoader);
    }
  }

  class ToolProviderTester {

    private final ModuleLayer layer;
    private final Project.ModuleUnit unit;

    ToolProviderTester(ModuleLayer layer, Project.ModuleUnit unit) {
      this.layer = layer;
      this.unit = unit;
    }

    int test() {
      var key = "test(" + unit.name() + ")";
      var serviceLoader = ServiceLoader.load(layer, ToolProvider.class);
      var tools =
              StreamSupport.stream(serviceLoader.spliterator(), false)
                      .filter(provider -> provider.name().equals(key))
                      .collect(Collectors.toList());
      if (tools.isEmpty()) {
        bach.log("No tool provider named '%s' found in: %s", key, layer);
        return 0;
      }
      int sum = 0;
      for (var tool : tools) {
        sum += run(tool);
      }
      return sum;
    }
  }

  class JUnitConsoleTester {

    private final ModuleLayer layer;
    private final Project.ModuleUnit unit;

    JUnitConsoleTester(ModuleLayer layer, Project.ModuleUnit unit) {
      this.layer = layer;
      this.unit = unit;
    }

    int test() {
      var serviceLoader = ServiceLoader.load(layer, ToolProvider.class);
      var junit =
          StreamSupport.stream(serviceLoader.spliterator(), false)
              .filter(provider -> provider.name().equals("junit"))
              .findFirst();
      if (junit.isEmpty()) {
        bach.warn("No tool provider named 'junit' for %s found in: %s", unit.name(), layer);
        return 0;
      }
      return run(junit.get(), "--select-module", unit.name());
    }
  }
}
