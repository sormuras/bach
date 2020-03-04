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
import de.sormuras.bach.api.Tool;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

  /** Create new tool-running task for the given tool name and arguments. */
  public static Task run(Tool tool) {
    return run(tool.name(), tool.arguments().toArray(String[]::new));
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
    var paths = project.paths();
    var modules = paths.modules(realm);
    var sources = paths.sources(realm);
    var title = realm.title();
    return sequence(
        "Package " + title + " modules and sources",
        createDirectories(modules),
        createDirectories(sources),
        parallel("Jar each " + title + " module", jars.toArray(Task[]::new)));
  }

  protected Task compileApiDocumentation() {
    return sequence("Compile API documentation");
  }

  protected Task launchAllTests() {
    return sequence("Launch all tests");
  }
}
