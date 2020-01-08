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

package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Call;
import de.sormuras.bach.Log;
import de.sormuras.bach.project.Folder;
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.project.Unit;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.StreamSupport;

class Tester {

  private final Bach bach;
  private final Realm realm;
  private final Log log;
  private final Folder folder;

  Tester(Bach bach, Realm realm) {
    this.bach = bach;
    this.realm = realm;
    this.log = bach.getLog();
    this.folder = bach.getProject().folder();
  }

  void test(Iterable<Unit> units) {
    log.debug("Launching all tests in realm " + realm);
    for (var unit : units) {
      log.debug("Testing %s...", unit);
      test(unit);
    }
  }

  private void test(Unit unit) {
    var modulePath = new ArrayList<Path>();
    modulePath.add(bach.getProject().modularJar(unit)); // test module first
    modulePath.addAll(realm.modulePaths()); // compile dependencies next, like "main"...
    modulePath.add(folder.modules(realm.name())); // same realm last, like "test"...
    var layer = layer(modulePath, unit.name());

    var errors = new StringBuilder();
    errors.append(run(layer, "test(" + unit.name() + ")"));
    errors.append(
        run(
            layer,
            "junit",
            "--select-module",
            unit.name(),
            "--reports-dir",
            folder.realm(realm.name(), "junit-reports").toString()));
    if (errors.toString().replace('0', ' ').isBlank()) {
      return;
    }
    throw new AssertionError("Test run failed! // " + errors);
  }

  private ModuleLayer layer(List<Path> modulePath, String module) {
    var finder = ModuleFinder.of(modulePath.toArray(Path[]::new));
    var roots = List.of(module);
    if (bach.isVerbose()) {
      log.debug("Module path:");
      for (var element : modulePath) {
        log.debug("  -> %s", element);
      }
      log.debug("Finder finds module(s):");
      finder.findAll().stream()
          .sorted(Comparator.comparing(ModuleReference::descriptor))
          .forEach(reference -> log.debug("  -> %s", reference));
      log.debug("Root module(s):");
      for (var root : roots) {
        log.debug("  -> %s", root);
      }
    }
    var boot = ModuleLayer.boot();
    var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
    var loader = ClassLoader.getPlatformClassLoader();
    var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), loader);
    return controller.layer();
  }

  private int run(ModuleLayer layer, String name, String... args) {
    var serviceLoader = ServiceLoader.load(layer, ToolProvider.class);
    return StreamSupport.stream(serviceLoader.spliterator(), false)
        .filter(provider -> provider.name().equals(name))
        .mapToInt(tool -> Math.abs(run(tool, args)))
        .sum();
  }

  private int run(ToolProvider tool, String... args) {
    var toolLoader = tool.getClass().getClassLoader();
    var currentThread = Thread.currentThread();
    var currentContextLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(toolLoader);
    try {
      var parent = toolLoader;
      while (parent != null) {
        parent.setDefaultAssertionStatus(true);
        parent = parent.getParent();
      }
      return bach.run(tool, new Call(tool.name(), args));
    } finally {
      currentThread.setContextClassLoader(currentContextLoader);
    }
  }
}
