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

import de.sormuras.bach.model.Project;
import de.sormuras.bach.model.ProjectBuilder;
import java.lang.module.ModuleDescriptor.Version;
import java.util.function.Consumer;

/** Bach - Java Shell Builder. */
public class Bach {

  /** Version of Bach. */
  public static Version VERSION = Version.parse("11.0-ea");

  /** Main entry-point. */
  public static void main(String... args) {
    Main.main(args);
  }

  /** Build default project potentially modified by the passed project builder consumer. */
  public Object build(Consumer<ProjectBuilder> projectBuilderConsumer) {
    return build(project(projectBuilderConsumer));
  }

  /** Build the specified project. */
  public Object build(Project project) {
    return project;
  }

  /** Create new default project potentially modified by the passed project builder consumer. */
  Project project(Consumer<ProjectBuilder> projectBuilderConsumer) {
    // var projectBuilder = new ProjectScanner(paths).scan();
    var projectBuilder = new ProjectBuilder();
    projectBuilderConsumer.accept(projectBuilder);
    return projectBuilder.build();
  }
}
