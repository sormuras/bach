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

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Build modular Java project.
 */
public class Bach {

  public static String VERSION = "2.0-ea";

  /**
   * Main entry-point.
   */
  public static void main(String... args) {
    System.out.println("Bach " + VERSION);
  }

  /**
   * Project model.
   */
  public static /*record*/ class Project {
    final String name;
    final Version version;

    public Project(String name, Version version) {
      this.name = name;
      this.version = version;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Project project = (Project) o;
      return name.equals(project.name) &&
             version.equals(project.version);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, version);
    }
  }

  /**
   * Mutable project builder.
   */
  public static class ProjectBuilder {

    static ProjectBuilder of(Path path) {
      var builder = new ProjectBuilder();
      builder.name = Optional.ofNullable(path.toAbsolutePath().getFileName()).map(Path::toString).orElse("project");
      builder.version = "0";
      return builder;
    }

    String name = "project";
    String version = "0";

    Project build() {
      return new Project(name, Version.parse(version));
    }
  }

  /**
   * Java source generator.
   */
  public static class SourceGenerator {

    String $(Object object) {
      return object == null ? "null" : "\"" + object + "\"";
    }

    public List<String> toSource(Project project) {
      var lines = new ArrayList<String>();
      lines.add(String.format("new Project(%s, Version.parse(%s));", $(project.name), $(project.version)));
      return lines;
    }
  }
}
