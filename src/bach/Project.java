/*
 * Bach - Java Shell Builder
 * Copyright (C) 2018 Christian Stein
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

import java.nio.file.*;
import java.util.*;

/** Project build support. */
class Project {

  static ProjectBuilder builder() {
    return new ProjectBuilder();
  }

  private String name = Paths.get(".").toAbsolutePath().normalize().getFileName().toString();
  private String version = "1.0.0-SNAPSHOT";
  private Path target = Paths.get("target", "bach");
  private Map<String, ModuleGroup> moduleGroups = new TreeMap<>();

  private Project() {}

  String name() {
    return name;
  }

  String version() {
    return version;
  }

  Path target() {
    return target;
  }

  ModuleGroup moduleGroup(String name) {
    return moduleGroups.get(name);
  }

  static class ProjectBuilder {

    private Project project = new Project();

    Project build() {
      var result = project;
      project = new Project();
      return result;
    }

    ProjectBuilder name(String name) {
      project.name = name;
      return this;
    }

    ProjectBuilder version(String version) {
      project.version = version;
      return this;
    }

    ProjectBuilder target(Path target) {
      project.target = target;
      return this;
    }

    ModuleGroupBuilder newModuleGroup(String name) {
      if (project.moduleGroups.containsKey(name)) {
        throw new IllegalArgumentException(name + " already defined");
      }
      return new ModuleGroupBuilder(this, name);
    }
  }

  static class ModuleGroup {

    private final String name;
    private Path destination;
    private List<Path> modulePath;
    private List<Path> moduleSourcePath;
    private Map<String, List<Path>> patchModule = Map.of();

    private ModuleGroup(String name) {
      this.name = name;
    }

    String name() {
      return name;
    }

    Path destination() {
      return destination;
    }

    List<Path> modulePath() {
      return modulePath;
    }

    List<Path> moduleSourcePath() {
      return moduleSourcePath;
    }

    Map<String, List<Path>> patchModule() {
      return patchModule;
    }
  }

  static class ModuleGroupBuilder {

    private final ProjectBuilder projectBuilder;
    private final ModuleGroup group;

    private ModuleGroupBuilder(ProjectBuilder projectBuilder, String name) {
      this.projectBuilder = projectBuilder;
      this.group = new ModuleGroup(name);
      this.group.destination = projectBuilder.project.target.resolve(Paths.get(name, "modules"));
      this.group.modulePath = List.of();
      this.group.moduleSourcePath = List.of(Paths.get("src", name, "java"));
    }

    ProjectBuilder end() {
      projectBuilder.project.moduleGroups.put(group.name, group);
      return projectBuilder;
    }

    ModuleGroupBuilder destination(Path destination) {
      group.destination = destination;
      return this;
    }

    ModuleGroupBuilder modulePath(List<Path> modulePath) {
      group.modulePath = modulePath;
      return this;
    }

    ModuleGroupBuilder moduleSourcePath(List<Path> moduleSourcePath) {
      group.moduleSourcePath = moduleSourcePath;
      return this;
    }

    ModuleGroupBuilder patchModule(Map<String, List<Path>> patchModule) {
      group.patchModule = patchModule;
      return this;
    }
  }
}
