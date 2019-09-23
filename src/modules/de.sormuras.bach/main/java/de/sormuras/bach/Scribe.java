/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

import java.util.Collection;
import java.util.TreeSet;

/*BODY*/
/** Create API documentation. */
public /*STATIC*/ class Scribe {

  private final Bach bach;
  private final Project project;
  private final Project.Realm realm;
  private final Project.Target target;

  public Scribe(Bach bach, Project project, Project.Realm realm) {
    this.bach = bach;
    this.project = project;
    this.realm = realm;
    this.target = project.target(realm);
  }

  public void document() {
    document(new TreeSet<>(realm.units.keySet()));
  }

  public void document(Collection<String> modules) {
    bach.log("Compiling %s realm's documentation: %s", realm.name, modules);
    var destination = target.directory.resolve("javadoc");
    bach.run(
        new Command("javadoc")
            .add("-d", destination)
            .add("-encoding", "UTF-8")
            .addIff(!bach.verbose(), "-quiet")
            .add("-Xdoclint:-missing")
            .add("--module-path", project.library.modulePaths)
            .add("--module-source-path", realm.moduleSourcePath)
            .add("--module", String.join(",", modules)));

    var nameDashVersion = project.name + '-' + project.version;
    bach.run(
        new Command("jar")
            .add("--create")
            .add("--file", target.directory.resolve(nameDashVersion + "-javadoc.jar"))
            .addIff(bach.verbose(), "--verbose")
            .add("--no-manifest")
            .add("-C", destination)
            .add("."));
  }
}
