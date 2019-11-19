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

import de.sormuras.bach.util.Modules;
import de.sormuras.bach.util.Paths;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import javax.lang.model.SourceVersion;

public class ProjectBuilder {

  /** Create project instance auto-configured by scanning the current working directory. */
  public static Project build(Path base) {
    return build(Folder.of(base));
  }

  public static Project build(Folder folder) {
    var base = folder.base();
    if (!Files.isDirectory(base)) {
      throw new IllegalArgumentException("Not a directory: " + base);
    }
    var main =
        new Realm(
            "main",
            Set.of(),
            List.of(folder.base().resolve("src/{MODULE}/main/java")),
            List.of(folder.lib()));
    var test =
        new Realm(
            "test",
            Set.of(Realm.Modifier.TEST),
            List.of(
                folder.base().resolve("src/{MODULE}/test/java"),
                folder.base().resolve("src/{MODULE}/test/module")),
            List.of(folder.modules("main"), folder.lib()));
    var realms = List.of(main, test);

    var src = base.resolve("src"); // TODO System.getProperty(".bach/project.path.src", "src")
    var modules = new TreeMap<String, List<String>>(); // local realm-based module registry
    var units = new ArrayList<Unit>();
    for (var root : Paths.list(src, Files::isDirectory)) {
      var module = root.getFileName().toString();
      if (!SourceVersion.isName(module.replace(".", ""))) continue;
      realm:
      for (var realm : realms) {
        modules.putIfAbsent(realm.name(), new ArrayList<>());
        for (var zone : List.of("java", "module")) {
          var info = root.resolve(realm.name()).resolve(zone).resolve("module-info.java");
          if (Files.isRegularFile(info)) {
            var patches = new ArrayList<Path>();
            if (realm.name().equals("test") && modules.get("main").contains(module)) {
              patches.add(src.resolve(module).resolve("main/java"));
            }
            var descriptor = Modules.describe(Paths.readString(info));
            units.add(new Unit(realm, info, descriptor, patches));
            modules.get(realm.name()).add(module);
            continue realm; // first zone hit wins
          }
        }
      }
    }

    var name = Optional.ofNullable(base.toAbsolutePath().getFileName()).map(Path::toString);
    var version = Version.parse("0");
    var structure = new Structure(folder, Library.of(), realms, units);

    return new Project(name.orElse("project"), version, structure);
  }
}
