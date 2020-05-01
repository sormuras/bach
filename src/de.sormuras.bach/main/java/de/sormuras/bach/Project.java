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

import de.sormuras.bach.util.Modules;
import de.sormuras.bach.util.Paths;
import java.io.File;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeSet;

/** A project descriptor. */
public /*static*/ final class Project {

  public static Builder newProject(String title, String version) {
    return new Builder().title(title).version(Version.parse(version));
  }

  private final Base base;
  private final Info info;

  public Project(Base base, Info info) {
    this.base = base;
    this.info = info;
  }

  public Base base() {
    return base;
  }

  public Info info() {
    return info;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Project.class.getSimpleName() + "[", "]")
        .add("base=" + base)
        .add("info=" + info)
        .toString();
  }

  public String toTitleAndVersion() {
    return info.title() + ' ' + info.version();
  }

  /** A base directory with a set of derived directories, files, locations, and other assets. */
  public static final class Base {

    /** Create a base instance for the current working directory. */
    public static Base of() {
      return of(Path.of(""));
    }

    /** Create a base instance for the specified directory. */
    public static Base of(Path directory) {
      return new Base(directory, directory.resolve(".bach/workspace"));
    }

    private final Path directory;
    private final Path workspace;

    Base(Path directory, Path workspace) {
      this.directory = directory;
      this.workspace = workspace;
    }

    public Path directory() {
      return directory;
    }

    public Path workspace() {
      return workspace;
    }

    public Path path(String first, String... more) {
      return directory.resolve(Path.of(first, more));
    }

    public Path workspace(String first, String... more) {
      return workspace.resolve(Path.of(first, more));
    }

    public Path api() {
      return workspace("api");
    }

    public Path classes(String realm) {
      return workspace("classes", realm);
    }

    public Path classes(String realm, String module) {
      return workspace("classes", realm, module);
    }

    public Path image() {
      return workspace("image");
    }

    public Path modules(String realm) {
      return workspace("modules", realm);
    }
  }

  /** A basic information holder. */
  public static final class Info {

    private final String title;
    private final Version version;

    public Info(String title, Version version) {
      this.title = title;
      this.version = version;
    }

    public String title() {
      return title;
    }

    public Version version() {
      return version;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Info.class.getSimpleName() + "[", "]")
          .add("title='" + title + "'")
          .add("version=" + version)
          .toString();
    }
  }

  /** A builder for building {@link Project} objects. */
  public static class Builder {

    private Base base = Base.of();
    private String title = "Project Title";
    private Version version = Version.parse("1-ea");

    public Project build() {
      var info = new Info(title, version);
      return new Project(base, info);
    }

    public Builder base(Base base) {
      this.base = base;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder version(Version version) {
      this.version = version;
      return this;
    }
  }

  /** A directory walker using {@code module-info.java} units to build {@link Builder} objects. */
  public interface Walker {

    /** A single-realm directory tree walker creating a single unnamed realm for all modules. */
    class DefaultRealmWalker {

      private final Path base;

      public DefaultRealmWalker(Path base) {
        this.base = base;
      }

      public Builder walk() {
        var moduleInfoFiles = Paths.find(List.of(base), Paths::isModuleInfoJavaFile);
        if (moduleInfoFiles.isEmpty()) throw new IllegalStateException("No module found: " + base);
        var moduleNames = new TreeSet<String>();
        var moduleSourcePathPatterns = new TreeSet<String>();
        for (var info : moduleInfoFiles) {
          var descriptor = Modules.describe(info);
          moduleNames.add(descriptor.name());
          moduleSourcePathPatterns.add(Modules.modulePatternForm(info, descriptor.name()));
        }
        var moduleSourcePath = String.join(File.pathSeparator, moduleSourcePathPatterns);
        return new Builder().base(Base.of(base));
      }
    }
  }
}
