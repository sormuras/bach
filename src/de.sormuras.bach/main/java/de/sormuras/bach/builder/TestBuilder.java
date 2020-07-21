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

package de.sormuras.bach.builder;

import static java.lang.System.Logger.Level.*;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Call;
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.project.TestSources;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.TestModule;

public class TestBuilder extends AbstractTestBuilder<TestSources> {

  public TestBuilder(Bach bach) {
    super(bach, bach.project().sources().testSources());
  }

  @Override
  public void buildModules() {
    buildTestModules();
    buildTestReportsByExecutingTestModules();
  }

  public void buildTestModules() {
    bach().run(computeJavacForTestSources());
    Paths.createDirectories(base().modules(test().name()));
    bach().run(bach()::run, this::computeJarCall, test().units().map().values());
  }

  public void buildTestReportsByExecutingTestModules() {
    for (var unit : test().units().map().values())
      buildTestReportsByExecutingTestModule("test", unit);
  }

  public void buildTestReportsByExecutingTestModule(String realm, SourceUnit unit) {
    var module = unit.name();
    var modulePaths =
        Paths.retainExisting(
            project().toModuleArchive(realm, module), // test module
            base().modules(""), // main modules
            base().modules(realm), // other test modules
            base().libraries()); // external modules
    log(DEBUG, "Run tests in '%s' with module-path: %s", module, modulePaths);

    var testModule = new TestModule(module, modulePaths);
    if (testModule.findProvider().isPresent()) bach().run(testModule);

    var junit = computeJUnitCall(unit, modulePaths);
    if (junit.findProvider().isPresent()) bach().run(junit);
  }

  public Javac computeJavacForTestSources() {
    var release = Runtime.version().feature();
    var testUnits = test().units();
    var modulePath = Paths.joinExisting(base().modules(""), base().libraries());
    return Call.javac()
        .withModule(testUnits.toNames(","))
        .with("--module-version", project().version().toString() + "-test")
        .with(testUnits.toModuleSourcePaths(false), Javac::withModuleSourcePath)
        .with(
            testUnits.toModulePatches(main().units()).entrySet(),
            (javac, patch) -> javac.withPatchModule(patch.getKey(), patch.getValue()))
        .with(modulePath, Javac::withModulePath)
        .withEncoding("UTF-8")
        .with("-parameters")
        .withRecommendedWarnings()
        .with("-d", base().classes("test", release));
  }
}
