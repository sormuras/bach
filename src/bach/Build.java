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

// default package

import de.sormuras.bach.Bach;
import de.sormuras.bach.Log;
import de.sormuras.bach.project.Folder;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.ProjectBuilder;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;

/** Program building module {@code de.sormuras.bach} itself. */
public class Build {
  public static void main(String... args) {
    var log = Log.ofSystem(true);
    var builder = new ProjectBuilder(log);
    var folder = Folder.of(Path.of(""));
    var properties = builder.properties(folder);

    var name = ProjectBuilder.Property.NAME.get(properties);
    var version = ProjectBuilder.Property.VERSION.get(properties);
    var structure = builder.structure(folder, properties);
    var deployment = builder.deployment(properties);

    var project = new Project(name, Version.parse(version), structure, deployment);
    Bach.build(log, project);
  }
}
