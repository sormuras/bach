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

package de.sormuras.bach;

import de.sormuras.bach.project.Information;
import de.sormuras.bach.project.Directory;
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.project.Unit;
import de.sormuras.bach.project.Structure;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;

/** Bach API type factories for testing purposes. */
public interface API {
  static Project emptyProject() {
    return new Project("empty", Version.parse("0"), Information.of(), emptyStructure());
  }

  static Information emptyInformation() {
    return Information.of();
  }

  static Structure emptyStructure() {
    return new Structure(List.of(), null);
  }

  static Realm emptyRealm() {
    return newRealm("empty");
  }

  static Unit emptyUnit() {
    return new Unit(newModuleDescriptor("empty"), List.of());
  }

  static Directory emptyDirectory() {
    return new Directory(Path.of("empty"), Directory.Type.UNKNOWN, 0);
  }

  static ModuleDescriptor newModuleDescriptor(String name, String... requires) {
    var descriptor = ModuleDescriptor.newModule(name);
    for (var required : requires) descriptor.requires(required);
    return descriptor.build();
  }

  static Realm newRealm(String name, Unit... units) {
    return new Realm(name, 0, false, List.of(units), null);
  }

  static Unit newUnit(String name, Directory... directories) {
    return new Unit(newModuleDescriptor(name), List.of(directories));
  }

  @FunctionalInterface
  interface Executable {
    void execute(Task.Execution execution) throws Exception;
  }

  static Task taskOf(String name, Executable executable) {
    class NamedTask extends Task {
      NamedTask() {
        super(name);
      }

      @Override
      public void execute(Execution execution) throws Exception {
        executable.execute(execution);
      }
    }
    return new NamedTask();
  }
}
