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

package de.sormuras.bach.api;

import de.sormuras.bach.api.Realm.Flag;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Bach API helpers. */
public class Projects {

  public static Project docProjectJigsawQuickStart() {
    var name = "jigsaw.quick.start";
    var base = Path.of("doc", "project", name);
    var module = "com.greetings";
    var source = Source.of(base.resolve(module));
    var unit =
        new Unit(
            source.path().resolve("module-info.java"),
            ModuleDescriptor.newModule(module).mainClass(module + ".Main").build(),
            base,
            List.of(source),
            List.of());
    var realm = new Realm("", 0, List.of(unit), List.of(), Realm.Flag.values());
    return
        Project.builder()
            .base(base)
            .name(name)
            .units(List.of(unit))
            .realms(List.of(realm))
            .build();
  }

  public static Project newProject(String name, String version) {
    return Project.builder().name(name).version(version).build();
  }

  public static Project newProjectWithAllBellsAndWhistles() {
    var main =
        realm(
            "main",
            0,
            List.of("alpha", "beta", "gamma", "delta", "omega"),
            List.of(),
            Flag.CREATE_JAVADOC);
    var test =
        realm(
            "test",
            0,
            List.of("test", "beta"),
            List.of(main),
            Flag.LAUNCH_TESTS,
            Flag.ENABLE_PREVIEW);
    var units =
        Stream.of(main.units(), test.units()).flatMap(List::stream).collect(Collectors.toList());
    return Project.builder()
        .base("")
        .name("bells.and.whistles")
        .version("0.1-ea+3")
        .units(units)
        .realms(List.of(main, test))
        .tuner(new Tuner())
        .requires(Set.of("foo", "bar"))
        .locators(
            List.of(
                Locator.direct(Map.of("bar", Maven.central("com.bar", "bar", "1"))),
                Locator.mavenCentral(Map.of("foo", "org.foo:foo:2")),
                Locator.dynamicCentral(Map.of("junit", "3.7"))
        ))
        .build();
  }

  static Realm realm(String name) {
    return realm(name, 0, List.of(), List.of());
  }

  static Realm realm(
      String name, int feature, List<String> modules, List<Realm> requires, Flag... flags) {
    var units = modules.stream().map(module -> unit(module, name)).collect(Collectors.toList());
    return new Realm(name, feature, units, requires, flags);
  }

  static Unit unit(String module, String realm) {
    return new Unit(
        Path.of("src", module, realm, "java", "module-info.java"),
        ModuleDescriptor.newModule(module).build(),
        Path.of("src", "{MODULE}", realm, "java"),
        List.of(Source.of(Path.of("src", module, realm, "java"))),
        List.of(Path.of("src", module, realm, "resources")));
  }
}
