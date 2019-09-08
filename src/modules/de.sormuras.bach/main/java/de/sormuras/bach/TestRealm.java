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

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/*BODY*/
/*STATIC*/ class TestRealm extends Realm {

  private final Realm main;

  TestRealm(String name, Configuration configuration, Realm main) {
    super(name, configuration);
    this.main = main;
  }

  void addModulePatches(Command javac, Collection<String> modules) {
    var mainModules = main.getDeclaredModules();
    for (var module : modules) {
      if (mainModules.contains(module)) {
        var patch = main.getDeclaredModuleInfo(module).sources;
        javac.add("--patch-module", module + "=" + patch);
      }
    }
  }

  List<Path> getModulePaths() {
    var paths = new LinkedHashSet<Path>();
    paths.addAll(super.getModulePaths()); // "test" realm
    paths.addAll(main.getModulePaths()); // "main" realm
    return Util.findExistingDirectories(paths);
  }

  @Override
  List<Path> getRuntimeModulePaths(Path... initialPaths) {
    var paths = new LinkedHashSet<>(List.of(initialPaths));
    paths.add(main.getDestination().resolve("modules")); // main modules
    paths.addAll(getModulePaths()); // test modules + library paths
    return Util.findExisting(paths);
  }
}
