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

package de.sormuras.bach.action;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Call;
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.project.CodeSpace;
import de.sormuras.bach.project.CodeUnit;
import de.sormuras.bach.tool.JUnit;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.TestModule;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;

/** An abstract action with test-realm specific build support. */
abstract class BuildTestCodeSpace<R> extends BuildCodeSpace<R> {

  BuildTestCodeSpace(Bach bach, CodeSpace<R> realm) {
    super(bach, realm);
  }

  @Override
  public void buildSpace() {
    super.buildSpace();
    buildReportsByExecutingModules();
  }

  @Override
  void buildModules() {
    bach().run(computeJavacCall());
    Paths.createDirectories(base().modules(space().name()));
    bach().run(bach()::run, this::computeJarCall, space().units().map().values());
  }

  public void buildReportsByExecutingModules() {
    space().units().toUnits().forEach(this::buildReportsByExecutingModule);
  }

  public void buildReportsByExecutingModule(CodeUnit unit) {
    var module = unit.name();
    var modulePaths = Paths.retainExisting(computeModulePathsForRuntime(unit));

    log(Level.DEBUG, "Run tests in '%s' with module-path: %s", module, modulePaths);

    var testModule = new TestModule(module, modulePaths);
    if (testModule.findProvider().isPresent()) bach().run(testModule);

    var junitCall = computeJUnitCall(unit, modulePaths);
    if (junitCall.findProvider().isPresent()) {
      bach().run(junitCall);
    }
  }

  public Javac computeJavacCall() {
    var classes = base().classes(space().name(), space().release().feature());
    var units = space().units();
    var modulePath = Paths.joinExisting(computeModulePathsForCompileTime());
    return Call.javac()
        .withModule(units.toNames(","))
        .with("--module-version", project().version().toString() + "-" + space().name())
        .with(units.toModuleSourcePaths(false), Javac::withModuleSourcePath)
        .with(
            units.toModulePatches(main().units()).entrySet(),
            (javac, patch) -> javac.withPatchModule(patch.getKey(), patch.getValue()))
        .with(modulePath, Javac::withModulePath)
        .withEncoding("UTF-8")
        .with("-parameters")
        .withRecommendedWarnings()
        .with("-d", classes);
  }

  public abstract Path[] computeModulePathsForCompileTime();

  public abstract Path[] computeModulePathsForRuntime(CodeUnit unit);

  public JUnit computeJUnitCall(CodeUnit unit, List<Path> modulePaths) {
    var module = unit.name();
    return new JUnit(module, modulePaths, List.of())
        .with("--select-module", module)
        .with("--disable-ansi-colors")
        .with("--reports-dir", base().reports("junit-" + space().name(), module));
  }
}
