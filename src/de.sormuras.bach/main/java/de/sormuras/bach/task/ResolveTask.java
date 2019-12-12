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

package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Task;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.Unit;
import de.sormuras.bach.util.Modules;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ResolveTask implements Task {

  @Override
  public void execute(Bach bach) throws Exception {
    var project = bach.getProject();
    var log = bach.getLog();
    var lib = project.folder().lib();
    var library = project.structure().library();

    var requires = library.requires();
    if (!requires.isEmpty()) {
      library.resolveRequires(lib);
    }

    var systemModulesSurvey = Modules.Survey.of(ModuleFinder.ofSystem());
    var missing = findMissingModules(bach, systemModulesSurvey);
    if (missing.isEmpty()) {
      log.debug("All required modules are locatable.");
      return;
    }

    log.info("Resolving missing modules...");
    var repeat = library.modifiers().contains(Library.Modifier.RESOLVE_RECURSIVELY);
    do {
      var intersection = new TreeSet<>(missing.keySet());
      library.resolveModules(lib, missing);
      missing.clear();
      var libraryModulesSurvey = Modules.Survey.of(ModuleFinder.of(lib));
      libraryModulesSurvey.putAllRequiresTo(missing);
      libraryModulesSurvey.declaredModules().forEach(missing::remove);
      systemModulesSurvey.declaredModules().forEach(missing::remove);
      intersection.retainAll(missing.keySet());
      if (!intersection.isEmpty())
        throw new IllegalStateException("Unresolved module(s): " + intersection);
    } while (repeat && !missing.isEmpty());
    // log.info("Loaded %d 3rd-party module(s): %s", loaded.size(), lib);
  }

  Map<String, Set<Version>> findMissingModules(Bach bach, Modules.Survey systemModulesSurvey) {
    var project = bach.getProject();
    var log = bach.getLog();
    var lib = project.folder().lib();
    var library = project.structure().library();
    var units = project.structure().units().stream().map(Unit::info).collect(Collectors.toList());
    var projectModulesSurvey = Modules.Survey.of(units);
    var libraryModulesSurvey = Modules.Survey.of(ModuleFinder.of(lib));

    log.debug("Project modules survey of %s unit(s) -> %s", units.size(), units);
    log.debug("  declared -> " + projectModulesSurvey.declaredModules());
    log.debug("  requires -> " + projectModulesSurvey.requiredModules());
    log.debug("Library modules survey of -> %s", lib.toUri());
    log.debug("  declared -> " + libraryModulesSurvey.declaredModules());
    log.debug("  requires -> " + libraryModulesSurvey.requiredModules());
    log.debug("System contains %d modules.", systemModulesSurvey.declaredModules().size());

    var missing = new TreeMap<String, Set<Version>>();
    projectModulesSurvey.putAllRequiresTo(missing);
    libraryModulesSurvey.putAllRequiresTo(missing);
    if (library.modifiers().contains(Library.Modifier.ADD_MISSING_JUNIT_TEST_ENGINES))
      Library.addJUnitTestEngines(missing);
    if (library.modifiers().contains(Library.Modifier.ADD_MISSING_JUNIT_PLATFORM_CONSOLE))
      Library.addJUnitPlatformConsole(missing);
    projectModulesSurvey.declaredModules().forEach(missing::remove);
    libraryModulesSurvey.declaredModules().forEach(missing::remove);
    systemModulesSurvey.declaredModules().forEach(missing::remove);
    return missing;
  }
}
