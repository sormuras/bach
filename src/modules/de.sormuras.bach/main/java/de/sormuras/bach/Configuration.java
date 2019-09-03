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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

/*BODY*/
public interface Configuration {

  Path DEFAULT_HOME_DIRECTORY = Path.of("");
  Path DEFAULT_WORKSPACE_DIRECTORY = Path.of("bin");

  default String getProjectName() {
    return getHomeDirectory().toAbsolutePath().getFileName().toString();
  }

  default Version getProjectVersion() {
    return Version.parse("0");
  }

  default Path getHomeDirectory() {
    return DEFAULT_HOME_DIRECTORY;
  }

  default Path getWorkspaceDirectory() {
    return DEFAULT_WORKSPACE_DIRECTORY;
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
    return of(Default.of(DEFAULT_HOME_DIRECTORY, DEFAULT_WORKSPACE_DIRECTORY));
  }

  static Configuration of(Path home) {
    return of(home, DEFAULT_WORKSPACE_DIRECTORY);
  }

  static Configuration of(Path home, Path work) {
    return new Fixture(home, work, Default.of(home, work));
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
        String.format("source directories = %s", configuration.getSourceDirectories()),
        String.format("project name = %s", configuration.getProjectName()),
        String.format("project version = %s", configuration.getProjectVersion()));
  }

  class Default implements Configuration, InvocationHandler {

    static Configuration of(Path home, Path work) {
      var dot = home.resolve(".bach");
      if (Files.isDirectory(dot)) {
        var bin = work.resolve(".bach");
        var name = "Configuration";
        var configurationJava = dot.resolve(name + ".java");
        if (Files.exists(configurationJava)) {
          var javac = ToolProvider.findFirst("javac").orElseThrow();
          javac.run(System.out, System.err, "-d", bin.toString(), configurationJava.toString());
        }
        try {
          var parent = Configuration.class.getClassLoader();
          var loader = URLClassLoader.newInstance(new URL[] {bin.toUri().toURL()}, parent);
          var configuration = loader.loadClass(name).getConstructor().newInstance();
          // System.out.println("Using custom configuration: " + configuration);
          if (configuration instanceof Configuration) {
            return (Configuration) configuration;
          }
          var interfaces = new Class[] {Configuration.class};
          var handler = new Default(home, work, configuration);
          return (Configuration) Proxy.newProxyInstance(loader, interfaces, handler);
        } catch (ClassNotFoundException e) {
          // ignore "missing" custom configuration class
        } catch (Exception e) {
          throw new Error("Loading custom configuration failed: " + configurationJava.toUri(), e);
        }
      }
      return new Default(home, work);
    }

    private final Path home;
    private final Path work;
    private final Object that;

    private Default(Path home, Path work) {
      this(home, work, new Object());
    }

    private Default(Path home, Path work, Object that) {
      this.that = that;
      this.home = home;
      this.work = work;
    }

    @Override
    public Path getHomeDirectory() {
      return home;
    }

    @Override
    public Path getWorkspaceDirectory() {
      return work;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      var name = method.getName();
      var types = method.getParameterTypes();
      try {
        try {
          return that.getClass().getMethod(name, types).invoke(that, args);
        } catch (NoSuchMethodException ignore) {
          return this.getClass().getMethod(name, types).invoke(this, args);
        }
      } catch (InvocationTargetException exception) {
        var cause = exception.getCause();
        if (cause != null) {
          throw cause;
        }
        throw exception;
      }
    }
  }

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
      super("Module " + module + " is not mapped");
    }
  }
}
