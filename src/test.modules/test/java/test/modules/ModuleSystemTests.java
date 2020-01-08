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

import static java.lang.module.ModuleDescriptor.Requires.Modifier.MANDATED;
import static java.lang.module.ModuleDescriptor.newModule;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import de.sormuras.bach.Bach;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class ModuleSystemTests {

  @Test
  void testModuleIsNamedAndTheNameIsTestModules() {
    assumeTrue(getClass().getModule().isNamed(), "not running on the module path");
    assertEquals("test.modules", getClass().getModule().getName());
  }

  @Test
  void bachModule() {
    var actual = Bach.class.getModule().getDescriptor();
    assumeTrue(actual != null, "not running on the module path");
    // exploded and jarred module fixtures
    var expected =
        newModule("de.sormuras.bach")
            .exports("de.sormuras.bach")
            .exports("de.sormuras.bach.project")
            .packages(Set.of("de.sormuras.bach.task", "de.sormuras.bach.util"))
            .uses(ToolProvider.class.getName())
        // .provides(ToolProvider.class.getName(), List.of("de.sormuras.bach.BachToolProvider"))
        ;
    // requires may contain compiled version
    var requiresWithVersion =
        actual.requires().stream()
            .filter(requires -> requires.name().equals("java.base"))
            .findFirst()
            .orElseThrow()
            .compiledVersion();
    if (requiresWithVersion.isPresent()) {
      var version = requiresWithVersion.orElseThrow();
      expected
          .requires(Set.of(MANDATED), "java.base", version)
          .requires(Set.of(), "java.compiler", version)
          .requires(Set.of(), "java.net.http", version)
          .requires(Set.of(), "jdk.compiler", version)
          .requires(Set.of(), "jdk.jartool", version)
          .requires(Set.of(), "jdk.javadoc", version)
          .requires(Set.of(), "jdk.jdeps", version)
          .requires(Set.of(), "jdk.jlink", version);
    } else {
      expected
          .requires(Set.of(MANDATED), "java.base")
          .requires(Set.of(), "java.compiler")
          .requires(Set.of(), "java.net.http")
          .requires(Set.of(), "jdk.compiler")
          .requires(Set.of(), "jdk.jartool")
          .requires(Set.of(), "jdk.javadoc")
          .requires(Set.of(), "jdk.jdeps")
          .requires(Set.of(), "jdk.jlink");
    }
    // only the jarred module provides the following attributes
    actual.version().ifPresent(expected::version); // reflexive
    actual.mainClass().ifPresent(__ -> expected.mainClass("de.sormuras.bach.Bach"));
    assertEquals(expected.build(), actual);
  }

  @TestFactory
  Stream<DynamicTest> findSystemModuleViaNameUsingCustomLayer() {
    var allSystemModuleNames =
        ModuleFinder.ofSystem().findAll().stream()
            .map(ModuleReference::descriptor)
            .map(ModuleDescriptor::name)
            .sorted()
            .collect(Collectors.toList());

    assertTrue(allSystemModuleNames.contains("java.base"));
    assertTrue(allSystemModuleNames.contains("java.compiler"));
    // ...
    assertTrue(allSystemModuleNames.contains("java.xml.crypto"));
    assertTrue(allSystemModuleNames.contains("jdk.accessibility"));
    // ...
    assertTrue(allSystemModuleNames.contains("jdk.zipfs"));
    assertTrue(allSystemModuleNames.size() > 50);

    var before = ModuleFinder.of();
    var after = ModuleFinder.of();
    var roots = List.of("java.base");
    var boot = ModuleLayer.boot();
    var configuration = boot.configuration().resolveAndBind(before, after, roots);
    var loader = ClassLoader.getPlatformClassLoader();
    var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), loader);
    var layer = controller.layer();
    return allSystemModuleNames.stream()
        .map(module -> dynamicTest(module, () -> assertTrue(layer.findModule(module).isPresent())));
  }
}
