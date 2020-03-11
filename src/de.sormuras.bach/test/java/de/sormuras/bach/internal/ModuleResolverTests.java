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

import de.sormuras.bach.api.Locator;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.Tree;

class ModuleResolverTests {

  static class Downloader implements BiConsumer<Set<String>, Path> {

    final Locator locator = Locator.dynamicCentral(Map.of());
    final Resources resources = new Resources(null, HttpClient.newHttpClient());

    @Override
    public void accept(Set<String> modules, Path lib) {
      System.out.println(modules);
      for (var module : modules) {
        var uri = locator.locate(module);
        if (uri.isEmpty()) continue;
        var version = locator.version(module).map(v -> "-" + v).orElse("");
        try {
          resources.copy(uri.get(), lib.resolve(module + version + ".jar"));
        } catch (Exception e) {
          System.err.println(e.getMessage());
        }
      }
    }
  }

  @Test
  void resolveJUnitJupiter(@TempDir Path temp) throws Exception {
    var downloader = new Downloader();
    var resolver = new ModuleResolver(temp, Set.of(), downloader);
    resolver.resolve(Set.of("org.junit.jupiter"));

    var files = new ArrayList<String>();
    Tree.walk(temp, files::add);
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
}
