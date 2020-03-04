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

package test.modules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.Bach;
import de.sormuras.bach.api.Paths;
import de.sormuras.bach.api.Project;
import de.sormuras.bach.api.Realm;
import de.sormuras.bach.api.Source;
import de.sormuras.bach.api.Unit;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.Log;
import test.base.Tree;

class DocProjectJigsawQuickStartTests {

  @Test
  void build(@TempDir Path temp) {
    var name = "jigsaw.quick.start";
    var base = Path.of("doc", "project", name);

    assertTrue(Files.isDirectory(base), "Directory not found: " + base);

    var module = "com.greetings";
    var source = Source.of(base.resolve(module));
    var unit =
        new Unit(
            source.path().resolve("module-info.java"),
            ModuleDescriptor.newModule(module).mainClass(module + ".Main").build(),
            base,
            List.of(source),
            List.of());
    var realm = new Realm("", 0, List.of(unit), List.of());
    var paths = new Paths(base, temp.resolve("out"), temp.resolve("lib"));
    var project =
        Project.builder()
            .paths(paths)
            .name(name)
            .units(List.of(unit))
            .realms(List.of(realm))
            .build();
    var log = new Log();
    var bach = new Bach(log, true);
    var summary = bach.build(project);

    summary.assertSuccessful();

    Tree.walk(temp, System.out::println);
    summary.toMarkdown().forEach(System.out::println);
  }
}
