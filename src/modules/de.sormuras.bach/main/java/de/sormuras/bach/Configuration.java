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

import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/*BODY*/
public interface Configuration {

  default String getProjectName() {
    return getHomeDirectory().toAbsolutePath().getFileName().toString();
  }

  default Version getProjectVersion() {
    return Version.parse("0");
  }

  default Path getHomeDirectory() {
    return Path.of("");
  }

  default Path getWorkspaceDirectory() {
    return Path.of("bin");
  }

  default Path getLibraryDirectory() {
    return getLibraryPaths().get(0);
  }

  default List<Path> getLibraryPaths() {
    return List.of(Path.of("lib"));
  }

  default List<Path> getSourceDirectories() {
    return List.of(Path.of("src"));
  }

  /** {@code if (module.equals("foo.bar.baz")) return URI.create("https://<path>/baz-1.3.jar")} */
  default URI getModuleUri(String module) {
    throw new UnmappedModuleException(module);
  }

  /** {@code module.startsWith("foo.bar") -> URI.create("https://dl.bintray.com/foo-bar/maven")} */
  default URI getModuleMavenRepository(String module) {
    return URI.create("https://repo1.maven.org/maven2");
  }

  /** {@code if (module.equals("foo.bar.baz")) return "org.foo.bar:foo-baz"} */
  default String getModuleMavenGroupAndArtifact(String module) {
    throw new UnmappedModuleException(module);
  }

  default String getModuleVersion(String module) {
    throw new UnmappedModuleException(module);
  }

  static Configuration of() {
    return of(new Default());
  }

  static Configuration of(Path home) {
    return of(home, new Default().getWorkspaceDirectory());
  }

  static Configuration of(Path home, Path work) {
    return new Fixture(home, work, new Default());
  }

  static Configuration of(Configuration configuration) {
    return new Fixture(
        configuration.getHomeDirectory(), configuration.getWorkspaceDirectory(), configuration);
  }

  static List<String> toStrings(Configuration configuration) {
    var home = configuration.getHomeDirectory();
    return List.of(
        String.format("home = '%s' -> %s", home, home.toUri()),
        String.format("workspace = '%s'", configuration.getWorkspaceDirectory()),
        String.format("library paths = %s", configuration.getLibraryPaths()),
        String.format("source directories = %s", configuration.getSourceDirectories()));
  }

  class Default implements Configuration {}

  class Fixture implements Configuration {
    private final Configuration that;
    private final Path homeDirectory;
    private final Path workspaceDirectory;
    private final Path libraryDirectory;
    private final List<Path> libraryPaths;
    private final List<Path> sourceDirectories;
    private final String projectName;
    private final Version projectVersion;

    Fixture(Path homeDirectory, Path workspaceDirectory, Configuration that) {
      this.that = Util.requireNonNull(that, "that underlying configuration");
      // basic
      this.homeDirectory = Util.requireNonNull(homeDirectory, "home directory");
      this.workspaceDirectory = resolve(workspaceDirectory, "workspace directory");
      this.libraryDirectory = resolve(that.getLibraryDirectory(), "library directory");
      this.libraryPaths = resolve(that.getLibraryPaths(), "library paths");
      this.sourceDirectories = resolve(that.getSourceDirectories(), "source directories");
      // project
      this.projectName = Util.requireNonNull(that.getProjectName(), "project name");
      this.projectVersion = Util.requireNonNull(that.getProjectVersion(), "project version");
    }

    private Path resolve(Path path, String name) {
      return Util.requireNonNull(path, name).isAbsolute() ? path : homeDirectory.resolve(path);
    }

    private List<Path> resolve(List<Path> paths, String name) {
      return List.of(
          Util.requireNonNull(paths, name).stream()
              .map(path -> resolve(path, "element of " + name))
              .toArray(Path[]::new));
    }

    @Override
    public Path getHomeDirectory() {
      return homeDirectory;
    }

    @Override
    public Path getWorkspaceDirectory() {
      return workspaceDirectory;
    }

    @Override
    public Path getLibraryDirectory() {
      return libraryDirectory;
    }

    @Override
    public List<Path> getLibraryPaths() {
      return libraryPaths;
    }

    @Override
    public List<Path> getSourceDirectories() {
      return sourceDirectories;
    }

    @Override
    public String getProjectName() {
      return projectName;
    }

    @Override
    public Version getProjectVersion() {
      return projectVersion;
    }

    @Override
    public URI getModuleUri(String module) {
      return that.getModuleUri(module);
    }

    @Override
    public URI getModuleMavenRepository(String module) {
      return that.getModuleMavenRepository(module);
    }

    @Override
    public String getModuleMavenGroupAndArtifact(String module) {
      return that.getModuleMavenGroupAndArtifact(module);
    }

    @Override
    public String getModuleVersion(String module) {
      return that.getModuleVersion(module);
    }
  }

  class UnmappedModuleException extends RuntimeException {
    UnmappedModuleException(String module) {
      super("Module " + module + "is not mapped");
    }
  }
}
