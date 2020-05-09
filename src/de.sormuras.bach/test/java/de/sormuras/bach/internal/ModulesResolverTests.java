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

package de.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
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
              "org.apiguardian.api.jar",
              "org.junit.jupiter.api.jar",
              "org.junit.jupiter.engine.jar",
              "org.junit.jupiter.jar",
              "org.junit.jupiter.params.jar",
              "org.junit.platform.commons.jar",
              "org.junit.platform.engine.jar",
              "org.opentest4j.jar"),
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
              "org.apiguardian.api.jar",
              "org.junit.platform.commons.jar",
              "org.junit.platform.console.jar",
              "org.junit.platform.engine.jar",
              "org.junit.platform.launcher.jar",
              "org.junit.platform.reporting.jar",
              "org.opentest4j.jar"),
          files);
    } catch (Throwable throwable) {
      files.forEach(System.err::println);
      throw throwable;
    }
  }

  private static class Transporter extends TreeMap<String, URI> implements Consumer<Set<String>> {

    private static URI central(String group, String artifact, String version) {
      var host = "https://repo.maven.apache.org/maven2";
      var file = artifact + '-' + version + ".jar";
      return URI.create(String.join("/", host, group.replace('.', '/'), artifact, version, file));
    }

    private final Path directory;
    private final Resources resources;

    private Transporter(Path directory) {
      this.directory = directory;
      this.resources = new Resources(HttpClient.newHttpClient());
      var jupiter = "org.junit.jupiter";
      put(jupiter, central(jupiter, "junit-jupiter", "5.6.2"));
      put(jupiter + ".api", central(jupiter, "junit-jupiter-api", "5.6.2"));
      put(jupiter + ".engine", central(jupiter, "junit-jupiter-engine", "5.6.2"));
      put(jupiter + ".params", central(jupiter, "junit-jupiter-params", "5.6.2"));

      var platform = "org.junit.platform";
      put(platform + ".commons", central(platform, "junit-platform-commons", "1.6.2"));
      put(platform + ".console", central(platform, "junit-platform-console", "1.6.2"));
      put(platform + ".engine", central(platform, "junit-platform-engine", "1.6.2"));
      put(platform + ".launcher", central(platform, "junit-platform-launcher", "1.6.2"));
      put(platform + ".reporting", central(platform, "junit-platform-reporting", "1.6.2"));
      put(platform + ".testkit", central(platform, "junit-platform-testkit", "1.6.2"));

      put("org.apiguardian.api", central("org.apiguardian", "apiguardian-api", "1.1.0"));
      put("org.opentest4j", central("org.opentest4j", "opentest4j", "1.2.0"));
    }

    @Override
    public void accept(Set<String> modules) {
      for (var module : modules) {
        var uri = get(module);
        if (uri == null) continue;
        try {
          var file = resources.copy(uri, directory.resolve(module + ".jar"));
          assertTrue(Files.isReadable(file));
        } catch (Exception e) {
          System.err.println(e.getMessage());
        }
      }
    }
  }
}
