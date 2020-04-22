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

package de.sormuras.bach.util;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.project.Locator;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import test.base.Tree;

class ModulesResolverTests {

  @Test
  void resolveWithoutTransportFails(@TempDir Path temp) {
    var resolver = new ModulesResolver(new Path[] {temp}, Set.of(), __ -> {});
    assertThrows(IllegalStateException.class, () -> resolver.resolve(Set.of("org.junit.jupiter")));
  }

  @Test
  @DisabledIfSystemProperty(named = "offline", matches = "true")
  void resolveJUnitJupiter(@TempDir Path temp) {
    var transporter = new Transporter(temp);
    var resolver = new ModulesResolver(new Path[] {temp}, Set.of(), transporter);
    resolver.resolve(Set.of("org.junit.jupiter"));

    var files = Tree.walk(temp);
    try {
      assertLinesMatch(
          List.of(
              "org.apiguardian.api@.+.jar",
              "org.junit.jupiter.api@.+.jar",
              "org.junit.jupiter.engine@.+.jar",
              "org.junit.jupiter.params@.+.jar",
              "org.junit.jupiter@.+.jar",
              "org.junit.platform.commons@.+.jar",
              "org.junit.platform.engine@.+.jar",
              "org.opentest4j@.+.jar"),
          files);
    } catch (Throwable throwable) {
      files.forEach(System.err::println);
      throw throwable;
    }
  }

  @Test
  @DisabledIfSystemProperty(named = "offline", matches = "true")
  void resolveJUnitPlatformConsole(@TempDir Path temp) {
    var transporter = new Transporter(temp);
    var resolver = new ModulesResolver(new Path[] {temp}, Set.of(), transporter);
    resolver.resolve(Set.of("org.junit.platform.console"));

    var files = Tree.walk(temp);
    try {
      assertLinesMatch(
          List.of(
              "org.apiguardian.api@.+.jar",
              "org.junit.platform.commons@.+.jar",
              "org.junit.platform.console@.+.jar",
              "org.junit.platform.engine@.+.jar",
              "org.junit.platform.launcher@.+.jar",
              "org.junit.platform.reporting@.+.jar",
              "org.opentest4j@.+.jar"),
          files);
    } catch (Throwable throwable) {
      files.forEach(System.err::println);
      throw throwable;
    }
  }

  private static class Transporter implements Consumer<Set<String>> {

    private final Path directory;
    private final Function<String, URI> locator;
    private final Resources resources;

    private Transporter(Path directory) {
      this.directory = directory;
      this.locator = Locator.of();
      this.resources = new Resources(HttpClient.newHttpClient());
    }

    @Override
    public void accept(Set<String> modules) {
      for (var module : modules) {
        var uri = locator.apply(module);
        if (uri == null) continue;
        var attributes = Locator.parseFragment(uri.getFragment());
        var version = Optional.ofNullable(attributes.get("version"));
        var jar = module + version.map(v -> '@' + v).orElse("") + ".jar";
        try {
          var file = resources.copy(uri, directory.resolve(jar));
          Paths.assertFileAttributes(file, attributes);
        } catch (Exception e) {
          System.err.println(e.getMessage());
        }
      }
    }
  }
}
