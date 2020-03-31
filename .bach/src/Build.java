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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Bach's own build program. */
class Build {
  public static void main(String... args) {
    var bach = new Bach();
    bach.print("Build Bach.java using %s", bach);
    var project = newProjectDescriptor();
    bach.print("%s", project);
    // bach.build(project).assertSuccessful();
  }

  private static Object newProjectDescriptor() {
    var folders = new ArrayList<Bach.Folder>();
    folders.addAll(mainFolders());
    folders.addAll(testFolders());
    return folders;
  }

  private static List<Bach.Folder> mainFolders() {
    return List.of(new Bach.Folder(Path.of("src/de.sormuras.bach/main/java"), 11));
  }

  private static List<Bach.Folder> testFolders() {
    return List.of(
        new Bach.Folder(Path.of("src/de.sormuras.bach/test/java"), 14),
        new Bach.Folder(Path.of("src/de.sormuras.bach/test/java-module"), 9),
        new Bach.Folder(Path.of("src/test.base/test/java"), 14),
        new Bach.Folder(Path.of("src/test.module/test/java"), 14),
        new Bach.Folder(Path.of("src/test.preview/test-preview/java"), 14));
  }
}
