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
import de.sormuras.bach.Workspace;
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.project.Unit;
import de.sormuras.bach.tool.JavaArchiveTool;
import de.sormuras.bach.tool.Option;
import de.sormuras.bach.tool.Tool;
import java.util.ArrayList;
import java.util.function.Supplier;

/** Supplies a task that compiles all realms of the specified project. */
public /*static*/ class BuildTaskFactory implements Supplier<Task> {

  private final Workspace workspace;
  private final Project project;
  private final boolean verbose;

  public BuildTaskFactory(Workspace workspace, Project project, boolean verbose) {
    this.workspace = workspace;
    this.project = project;
    this.verbose = verbose;
  }

  @Override
  public Task get() {
    return Task.sequence(
        "Build project " + project.toNameAndVersion(),
        printVersionInformationOfFoundationTools(),
        new ValidateWorkspace(),
        new PrintProject(project),
        new ValidateProject(project),
        new CreateDirectories(workspace.workspace()),
        compileAllRealms(),
        // javac/jar main realm | javadoc
        // jlink    | javac/jar test realm
        // jpackage | junit test realm
        new PrintModules(project));
  }

  protected Task printVersionInformationOfFoundationTools() {
    return verbose
        ? Task.parallel(
            "Print version of various foundation tools",
            Task.run(Tool.of("javac", "--version")),
            Task.run("jar", "--version"),
            Task.run("javadoc", "--version"))
        : Task.sequence("Print version of javac", Task.run("javac", "--version"));
  }

  protected Task compileAllRealms() {
    var realms = project.structure().realms();
    var tasks = realms.stream().map(this::compileRealm);
    return Task.sequence("Compile all realms", tasks.toArray(Task[]::new));
  }

  protected Task compileRealm(Realm realm) {
    // project.tuner().tune(javac, project, realm);
    return Task.sequence(
        "Compile " + realm.name() + " realm", Task.run(realm.javac()), createArchives(realm)
        // , createCustomRuntimeImage(realm)
        );
  }

  protected Task createArchives(Realm realm) {
    var jars = new ArrayList<Task>();
    for (var unit : realm.units()) {
      jars.add(createArchive(realm, unit));
      // jars.add(packageUnitSources(realm, unit));
    }
    return Task.sequence(
        "Package " + realm.name() + " modules and sources",
        new CreateDirectories(workspace.modules(realm.name())),
        // new CreateDirectories(workspace.sources(realm.name())),
        Task.parallel("Jar each " + realm.name() + " module", jars.toArray(Task[]::new)));
  }

  protected Task createArchive(Realm realm, Unit unit) {
    var file = workspace.module(realm.name(), unit.name(), project.toModuleVersion(unit));
    var options = new ArrayList<Option>();
    options.add(new JavaArchiveTool.PerformOperation(JavaArchiveTool.Operation.CREATE));
    options.add(new JavaArchiveTool.ArchiveFile(file));
    // if (verbose) options.add(new ObjectArrayOption<>("--verbose"));
    // module.mainClass().ifPresent(name -> options.add(new KeyValueOption<>("--main-class",
    // name)));
    var root = workspace.classes(realm.name(), realm.release()).resolve(unit.name());
    options.add(new JavaArchiveTool.ChangeDirectory(root));
    return Task.run(Tool.jar(options));
  }
}
