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
import de.sormuras.bach.project.Space;
import de.sormuras.bach.project.Unit;
import de.sormuras.bach.tool.Jar;
import java.lang.System.Logger.Level;
import java.util.ArrayList;

/** An abstract action with basic build support. */
abstract class BuildCodeSpace<T> implements Action {

  private final Bach bach;
  private final Space<T> space;

  BuildCodeSpace(Bach bach, Space<T> space) {
    this.bach = bach;
    this.space = space;
  }

  @Override
  public Bach bach() {
    return bach;
  }

  public final Space<T> space() {
    return space;
  }

  @Override
  public void execute() {
    if (space.units().isEmpty()) {
      log(Level.DEBUG, "No units in %s space - nothing to build", space.title());
      return;
    }

    log(Level.INFO, "\n");
    log(Level.INFO, "Build " + space.title() + " space");
    buildSpace();
  }

  public void buildSpace() {
    buildModules();
  }

  abstract void buildModules();

  public Jar computeJarCall(Unit unit) {
    var module = unit.name();
    var archive = project().toModuleArchive(space.name(), module);
    var classes = base().classes(space.name(), space.release().feature(), module);
    var resources = new ArrayList<>(unit.resources()); // TODO Include upstream resources if patched
    return Call.jar()
        .with("--create")
        .withArchiveFile(archive)
        .with(unit.descriptor().mainClass(), Jar::withMainClass)
        .with("-C", classes, ".")
        .with(resources, (call, resource) -> call.with("-C", resource, "."));
  }
}
