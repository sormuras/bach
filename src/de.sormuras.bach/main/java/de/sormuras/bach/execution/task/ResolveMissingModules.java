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

package de.sormuras.bach.execution.task;

import de.sormuras.bach.api.Locator;
import de.sormuras.bach.api.Project;
import de.sormuras.bach.execution.ExecutionContext;
import de.sormuras.bach.execution.ExecutionResult;
import de.sormuras.bach.execution.Task;
import de.sormuras.bach.internal.ModuleResolver;
import de.sormuras.bach.internal.Resources;
import java.lang.module.FindException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

/** Determine and load missing library modules. */
public /*static*/ class ResolveMissingModules extends Task {

  public ResolveMissingModules() {
    super("Resolve missing modules", false, List.of());
  }

  @Override
  public ExecutionResult execute(ExecutionContext execution) {
    var downloader = new Downloader(execution.summary().project());
    var project = execution.summary().project();
    var lib = project.paths().lib();
    var declared = project.toDeclaredModuleNames();
    var requires = new TreeSet<String>();
    requires.addAll(project.toRequiredModuleNames());
    requires.addAll(project.library().requires());
    try {
      var resolver = new ModuleResolver(lib, declared, downloader);
      resolver.resolve(requires);
      return execution.ok();
    } catch (Exception e) {
      return execution.failed(e);
    }
  }

  private static class Downloader implements BiConsumer<Set<String>, Path> {

    final Project project;
    Resources resources;

    Downloader(Project project) {
      this.project = project;
    }

    Locator.Location locate(String module) {
      var locators = project.library().locators();
      for (var locator : locators) {
        var located = locator.locate(module);
        if (located.isPresent()) return located.get();
      }
      throw new FindException("Module " + module + " not locatable via: " + locators);
    }

    @Override
    public void accept(Set<String> modules, Path directory) {
      if (resources == null) {
        // TODO var http = project.library().supplier().get();
        var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        resources = new Resources(null, http);
      }
      for (var module : modules) {
        var location = locate(module);
        var uri = location.uri();
        var version = location.toVersionString();
        try {
          resources.copy(uri, directory.resolve(module + version + ".jar"));
        } catch (Exception e) {
          System.err.println(e.getMessage());
        }
      }
    }
  }
}
