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
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.tool.JUnit;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.TestModule;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;

/** An abstract action with test-realm specific build support. */
abstract class BuildTestRealmAction<R> extends BuildRealmAction<R> {

  BuildTestRealmAction(Bach bach, Realm<R> realm) {
    super(bach, realm);
  }

  @Override
  public void buildRealm() {
    super.buildRealm();
    buildReportsByExecutingModules();
  }

  @Override
  void buildModules() {
    bach().run(computeJavacCall());
    Paths.createDirectories(base().modules(realm().name()));
    bach().run(bach()::run, this::computeJarCall, realm().units().map().values());
  }

  public void buildReportsByExecutingModules() {
    realm().units().toUnits().forEach(this::buildReportsByExecutingModule);
  }

  public void buildReportsByExecutingModule(SourceUnit unit) {
    var module = unit.name();
    var modulePaths = Paths.retainExisting(computeModulePathsForRuntime(unit));

    log(Level.DEBUG, "Run tests in '%s' with module-path: %s", module, modulePaths);

    var testModule = new TestModule(module, modulePaths);
    if (testModule.findProvider().isPresent()) bach().run(testModule);

    var junit = computeJUnitCall(unit, modulePaths);
    if (junit.findProvider().isPresent()) bach().run(junit);
  }

  public Javac computeJavacCall() {
    var classes = base().classes(realm().name(), realm().release().feature());
    var units = realm().units();
    var modulePath = Paths.joinExisting(computeModulePathsForCompileTime());
    return Call.javac()
        .withModule(units.toNames(","))
        .with("--module-version", project().version().toString() + "-" + realm().name())
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

  public abstract Path[] computeModulePathsForRuntime(SourceUnit unit);

  public JUnit computeJUnitCall(SourceUnit unit, List<Path> modulePaths) {
    var module = unit.name();
    return new JUnit(module, modulePaths, List.of())
        .with("--select-module", module)
        .with("--disable-ansi-colors")
        .with("--reports-dir", base().reports("junit-" + realm().name(), module));
  }
}
