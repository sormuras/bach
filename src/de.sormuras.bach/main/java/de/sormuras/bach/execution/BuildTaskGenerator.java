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

package de.sormuras.bach.execution;

import de.sormuras.bach.api.Project;
import de.sormuras.bach.api.Realm;
import de.sormuras.bach.api.Source;
import de.sormuras.bach.api.Tool;
import de.sormuras.bach.api.Unit;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.spi.ToolProvider;

/** Generate default build task for a given project. */
public /*static*/ class BuildTaskGenerator implements Supplier<Task> {

  public static Task parallel(String title, Task... tasks) {
    return new Task(title, true, List.of(tasks));
  }

  public static Task sequence(String title, Task... tasks) {
    return new Task(title, false, List.of(tasks));
  }

  /** Create new tool-running task for the given tool name and argument strings. */
  public static Task run(Tool tool) {
    return run(tool.name(), tool.toStrings());
  }

  /** Create new tool-running task for the given tool and its options. */
  public static Task run(String name, String... args) {
    var provider = ToolProvider.findFirst(name).orElseThrow();
    return new Tasks.RunToolProvider(provider, args);
  }

  private final Project project;
  private final boolean verbose;

  public BuildTaskGenerator(Project project, boolean verbose) {
    this.project = project;
    this.verbose = verbose;
  }

  public Project project() {
    return project;
  }

  public boolean verbose() {
    return verbose;
  }

  @Override
  public Task get() {
    return sequence(
        "Build " + project().toNameAndVersion(),
        createDirectories(project.paths().out()),
        printVersionInformationOfFoundationTools(),
        resolveMissingModules(),
        parallel(
            "Compile realms and generate API documentation",
            compileApiDocumentation(),
            compileAllRealms()),
        launchAllTests());
  }

  protected Task createDirectories(Path path) {
    return new Tasks.CreateDirectories(path);
  }

  protected Task printVersionInformationOfFoundationTools() {
    return verbose()
        ? parallel(
            "Print version of various foundation tools",
            run("javac", "--version"),
            run("javadoc", "--version"),
            run("jar", "--version"))
        : sequence("Print version of javac", run("javac", "--version"));
  }

  protected Task resolveMissingModules() {
    return sequence("Resolve missing modules");
  }

  protected Task compileAllRealms() {
    var realms = project.structure().realms();
    if (realms.isEmpty()) return sequence("Cannot compile modules: 0 realms declared");
    var tasks = realms.stream().map(this::compileRealm);
    return sequence("Compile all realms", tasks.toArray(Task[]::new));
  }

  protected Task compileRealm(Realm realm) {
    if (realm.units().isEmpty()) return sequence("No units in " + realm.title() + " realm?!");
    var paths = project.paths();
    var enablePreview = realm.flags().contains(Realm.Flag.ENABLE_PREVIEW);
    var release = enablePreview ? OptionalInt.of(Runtime.version().feature()) : realm.release();
    var patches = realm.patches((other, unit) -> List.of(project.toModularJar(other, unit)));
    var javac =
        Tool.javac()
            .setCompileModulesCheckingTimestamps(realm.moduleNames())
            .setVersionOfModulesThatAreBeingCompiled(project.version())
            .setPathsWhereToFindSourceFilesForModules(realm.moduleSourcePaths())
            .setPathsWhereToFindApplicationModules(realm.modulePaths(paths))
            .setPathsWhereToFindMoreAssetsPerModule(patches)
            .setEnablePreviewLanguageFeatures(enablePreview)
            .setCompileForVirtualMachineVersion(release.orElse(0))
            .setCharacterEncodingUsedBySourceFiles("UTF-8")
            .setOutputMessagesAboutWhatTheCompilerIsDoing(false)
            .setGenerateMetadataForMethodParameters(true)
            .setOutputSourceLocationsOfDeprecatedUsages(true)
            .setTerminateCompilationIfWarningsOccur(true)
            .setDestinationDirectory(paths.classes(realm));
    project.tuner().tune(javac, project, realm);
    return sequence("Compile " + realm.title() + " realm", run(javac), packageRealm(realm));
  }

  protected Task packageRealm(Realm realm) {
    var jars = new ArrayList<Task>();
    for (var unit : realm.units()) {
      jars.add(packageUnitModule(realm, unit));
      jars.add(packageUnitSources(realm, unit));
    }
    return sequence(
        "Package " + realm.title() + " modules and sources",
        createDirectories(project.paths().modules(realm)),
        createDirectories(project.paths().sources(realm)),
        parallel("Jar each " + realm.title() + " module", jars.toArray(Task[]::new)));
  }

  protected Task packageUnitModule(Realm realm, Unit unit) {
    var paths = project.paths();
    var module = unit.name();
    var classes = paths.classes(realm).resolve(module);
    var jar =
        Tool.of("jar")
            .add("--create")
            .add("--file", project.toModularJar(realm, unit))
            .add(verbose, "--verbose")
            .add("-C", classes, ".")
            .forEach(
                realm.requires(),
                (args, other) -> {
                  var patched = other.unit(module).isPresent();
                  var path = paths.classes(other).resolve(module);
                  args.add(patched, "-C", path, ".");
                })
            .forEach(unit.resources(), (any, path) -> any.add("-C", path, "."));
    project.tuner().tune(jar, project, realm, unit);
    return run(jar);
  }

  protected Task packageUnitSources(Realm realm, Unit unit) {
    var sources = project.paths().sources(realm);
    var jar =
        Tool.of("jar")
            .add("--create")
            .add("--file", sources.resolve(project.toJarName(unit, "sources")))
            .add(verbose, "--verbose")
            .add("--no-manifest")
            .forEach(unit.sources(Source::path), (any, path) -> any.add("-C", path, "."))
            .forEach(unit.resources(), (any, path) -> any.add("-C", path, "."));
    project.tuner().tune(jar, project, realm, unit);
    return run(jar);
  }

  protected Task compileApiDocumentation() {
    var realms = project.structure().realms();
    if (realms.isEmpty()) return sequence("Cannot generate API documentation: 0 realms");
    var realm =
        realms.stream()
            .filter(candidate -> candidate.flags().contains(Realm.Flag.CREATE_JAVADOC))
            .findFirst();
    if (realm.isEmpty()) return sequence("No realm wants javadoc: " + realms);
    return compileApiDocumentation(realm.get());
  }

  protected Task compileApiDocumentation(Realm realm) {
    var modules = realm.moduleNames();
    if (modules.isEmpty()) return sequence("Cannot generate API documentation: 0 modules");
    var name = project.name();
    var version = Optional.ofNullable(project.version());
    var file = name + version.map(v -> "-" + v).orElse("");
    var moduleSourcePath = realm.moduleSourcePaths();
    var modulePath = realm.modulePaths(project.paths());
    var javadoc = project.paths().javadoc();
    return sequence(
        "Generate API documentation and jar generated site",
        createDirectories(javadoc),
        run(
            Tool.of("javadoc")
                .add("--module", String.join(",", modules))
                .add("--module-source-path", Tool.join(moduleSourcePath))
                .add(!modulePath.isEmpty(), "--module-path", Tool.join(modulePath))
                .add("-d", javadoc)
                .add(!verbose, "-quiet")
                .add("-Xdoclint:-missing")),
        run(
            Tool.of("jar")
                .add("--create")
                .add("--file", javadoc.getParent().resolve(file + "-javadoc.jar"))
                .add(verbose, "--verbose")
                .add("--no-manifest")
                .add("-C", javadoc)
                .add(".")));
  }

  protected Task launchAllTests() {
    return sequence("Launch all tests");
  }
}
