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

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;

/** Bach's own build program. */
class Build {

  public static void main(String... args) {
    var project = project();
    var bach = new Bach();
    bach.print("Build Bach.java using %s", bach);
    bach.print("%s", project);
    // bach.build(project).assertSuccessful();
  }

  private static Object project() {
    return List.of(mainRealm(), testRealm(), testPreviewRealm());
  }

  private static Bach.Realm mainRealm() {
    return new Bach.Realm(
        "main",
        11,
        false,
        List.of(
            new Bach.Unit(
                ModuleDescriptor.newModule("de.sormuras.bach").build(),
                List.of(new Bach.Folder(Path.of("src/de.sormuras.bach/main/java"), 0)) //
                ) //
            ) //
        );
  }

  private static Bach.Realm testRealm() {
    return new Bach.Realm(
        "test",
        11,
        false,
        List.of(
            //
            new Bach.Unit(
                ModuleDescriptor.newOpenModule("de.sormuras.bach").build(),
                List.of(
                    new Bach.Folder(Path.of("src/de.sormuras.bach/test/java"), 0),
                    new Bach.Folder(Path.of("src/de.sormuras.bach/test/java-module"), 0))),
            //
            new Bach.Unit(
                ModuleDescriptor.newOpenModule("test.base").build(),
                List.of(new Bach.Folder(Path.of("src/test.base/test/java"), 0))),
            //
            new Bach.Unit(
                ModuleDescriptor.newOpenModule("test.modules").build(),
                List.of(new Bach.Folder(Path.of("src/test.modules/test/java"), 0)))
            //
            ));
  }

  private static Bach.Realm testPreviewRealm() {
    var descriptor = ModuleDescriptor.newOpenModule("test.preview").build();
    var release = Runtime.version().feature();
    var folders = List.of(new Bach.Folder(Path.of("src/test.preview/test-preview/java"), release));
    var unit = new Bach.Unit(descriptor, folders);
    return new Bach.Realm("test-preview", release, true, List.of(unit));
  }
}
