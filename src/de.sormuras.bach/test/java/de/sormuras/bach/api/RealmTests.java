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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RealmTests {

  @Test
  void syntheticMainAndTestRealms() {
    var paths = Paths.of(Path.of(""));
    var a =
        new Unit(
            Path.of("src/a/main/java/module-info.java"),
            ModuleDescriptor.newModule("a").build(),
            List.of(Source.of(Path.of("src/a/main/java"))),
            List.of(Path.of("src/a/main/resources")));

    var main =
        new Realm(
            "main", Runtime.version().feature(), List.of(a), List.of(), Realm.Flag.CREATE_JAVADOC);

    assertEquals("main", main.name());
    assertEquals(Runtime.version().feature(), main.feature());
    assertSame(a, main.unit("a").orElseThrow());
    assertTrue(main.requires().isEmpty());
    assertTrue(main.flags().contains(Realm.Flag.CREATE_JAVADOC));
    assertFalse(main.flags().contains(Realm.Flag.ENABLE_PREVIEW));
    assertFalse(main.flags().contains(Realm.Flag.LAUNCH_TESTS));

    assertEquals("main", main.title());
    assertEquals(Runtime.version().feature(), main.release().orElseThrow());
    assertLinesMatch(List.of("a"), main.moduleNames());
    assertEquals(List.of(Path.of("src/{MODULE}/main/java")), main.moduleSourcePaths());
    assertEquals(List.of(Path.of("lib")), main.modulePaths(paths));
    assertTrue(main.patches((realm, unit) -> List.of(Path.of("?"))).isEmpty());

    var t =
        new Unit(
            Path.of("src/t/test/java/module-info.java"),
            ModuleDescriptor.newModule("t", Set.of(ModuleDescriptor.Modifier.OPEN))
                .requires("a")
                .requires("org.junit.jupiter")
                .build(),
            List.of(Source.of(Path.of("src/a/main/java"))),
            List.of(Path.of("src/a/main/resources")));

    var a2 = // in-module test of module a
        new Unit(
            Path.of("src/a/test/java/module-info.java"),
            ModuleDescriptor.newModule("a", Set.of(ModuleDescriptor.Modifier.OPEN))
                .requires("org.junit.jupiter")
                .build(),
            List.of(Source.of(Path.of("src/a/test/java"))),
            List.of(Path.of("src/a/test/resources")));

    var test =
        new Realm(
            "test",
            Runtime.version().feature(),
            List.of(t, a2),
            List.of(main),
            Realm.Flag.ENABLE_PREVIEW,
            Realm.Flag.LAUNCH_TESTS);

    assertEquals("test", test.name());
    assertEquals(Runtime.version().feature(), test.feature());
    assertSame(t, test.unit("t").orElseThrow());
    assertSame(a2, test.unit("a").orElseThrow());
    assertFalse(test.requires().isEmpty());
    assertSame(main, test.requires().get(0));
    assertFalse(test.flags().contains(Realm.Flag.CREATE_JAVADOC));
    assertTrue(test.flags().contains(Realm.Flag.ENABLE_PREVIEW));
    assertTrue(test.flags().contains(Realm.Flag.LAUNCH_TESTS));

    assertEquals("test", test.title());
    assertEquals(Runtime.version().feature(), test.release().orElseThrow());
    assertLinesMatch(List.of("t", "a"), test.moduleNames());
    assertEquals(List.of(Path.of("src/{MODULE}/test/java")), test.moduleSourcePaths());
    assertEquals(List.of(paths.modules(main), Path.of("lib")), test.modulePaths(paths));
    assertEquals(
        Map.of("a", List.of(Path.of("src/a/main/java"))),
        test.patches((realm, unit) -> List.of(unit.info().getParent())));
  }
}
