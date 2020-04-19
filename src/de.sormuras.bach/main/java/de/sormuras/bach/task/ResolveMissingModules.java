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

package de.sormuras.bach.task;

import de.sormuras.bach.Project;
import de.sormuras.bach.Task;
import de.sormuras.bach.project.Locator;
import de.sormuras.bach.util.ModulesResolver;
import de.sormuras.bach.util.Resources;
import java.lang.System.Logger.Level;
import java.lang.module.FindException;
import java.lang.module.ResolutionException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/** Compute missing modules and transport them to the library directory of this project. */
public /*static*/ class ResolveMissingModules extends Task {

  private final Project project;

  public ResolveMissingModules(Project project) {
    super("Resolve missing modules");
    this.project = project;
  }

  @Override
  public void execute(Execution execution) {
    var structure = project.structure();
    var lib = execution.getBach().getWorkspace().lib();

    class Transporter implements Consumer<Set<String>> {
      @Override
      public void accept(Set<String> modules) {
        execution.print(Level.INFO, "Copy modules: " + modules);
        var resources = new Resources(execution.getBach().getHttpClient());
        for (var module : modules) {
          var uri = structure.library().locator().apply(module);
          if (uri == null) throw new FindException("Module " + module + " not locatable");
          var attributes = Locator.parseFragment(uri.getFragment());
          var version = Optional.ofNullable(attributes.get("version"));
          var jar = module + version.map(v -> '@' + v).orElse("") + ".jar";
          try {
            execution.print(Level.DEBUG, jar + " << " + uri);
            resources.copy(uri, lib.resolve(jar));
          } catch (Exception e) {
            throw new ResolutionException("Copy failed for: " + uri, e);
          }
        }
      }
    }

    var declared = structure.toDeclaredModuleNames();
    var resolver = new ModulesResolver(new Path[] {lib}, declared, new Transporter());
    var requires = new TreeSet<String>();
    requires.addAll(structure.toRequiredModuleNames());
    requires.addAll(structure.library().requires());
    resolver.resolve(requires);
  }
}
