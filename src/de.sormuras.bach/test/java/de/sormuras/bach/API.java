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

import de.sormuras.bach.project.structure.Directory;
import de.sormuras.bach.project.structure.Realm;
import de.sormuras.bach.project.structure.Unit;
import de.sormuras.bach.project.Structure;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;

/** Bach API type factories for testing purposes. */
public interface API {
  static Project emptyProject() {
    return new Project("empty", Version.parse("0"), emptyStructure());
  }

  static Structure emptyStructure() {
    return new Structure(List.of());
  }

  static Realm emptyRealm() {
    return new Realm("empty", 0, false, List.of());
  }

  static Unit emptyUnit() {
    return new Unit(emptyModuleDescriptor(), List.of());
  }

  static Directory emptyDirectory() {
    return new Directory(Path.of("empty"), 0);
  }

  static ModuleDescriptor emptyModuleDescriptor() {
    return ModuleDescriptor.newModule("empty").build();
  }

  @FunctionalInterface
  interface Executable {
    void execute(Task.Execution execution) throws Exception;
  }

  static Task taskOf(String name, Executable executable) {
    class Named extends Task {
      Named() {
        super(name);
      }

      @Override
      public void execute(Execution execution) throws Exception {
        executable.execute(execution);
      }
    }
    return new Named();
  }
}
