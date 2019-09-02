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

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

/*BODY*/
public interface Configuration {

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

  default Path resolve(Path path, String name) {
    return Configuration.resolve(getHomeDirectory(), path, name);
  }

  default List<Path> resolve(List<Path> paths, String name) {
    return Configuration.resolve(getHomeDirectory(), paths, name);
  }

  static Configuration of() {
    return of(Path.of(""));
  }

  static Configuration of(Path home) {
    validateDirectory(Util.requireNonNull(home, "home directory"));
    var ccc = compileCustomConfiguration(home);
    return new DefaultConfiguration(
        home,
        resolve(home, ccc.getWorkspaceDirectory(), "workspace directory"),
        resolve(home, ccc.getLibraryPaths(), "library paths"),
        resolve(home, ccc.getSourceDirectories(), "source directories"));
  }

  static Path resolve(Path home, Path path, String name) {
    return Util.requireNonNull(path, name).isAbsolute() ? path : home.resolve(path);
  }

  static List<Path> resolve(Path home, List<Path> paths, String name) {
    return List.of(
        Util.requireNonNull(paths, name).stream()
            .map(path -> resolve(home, path, "element of " + name))
            .toArray(Path[]::new));
  }

  private static Configuration compileCustomConfiguration(Path home) {
    class ConfigurationInvocationHandler implements Configuration, InvocationHandler {

      private final Object that;

      private ConfigurationInvocationHandler(Object that) {
        this.that = that;
      }

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
          return that.getClass().getMethod(method.getName()).invoke(that);
        } catch (NoSuchMethodException e) {
          return this.getClass().getMethod(method.getName()).invoke(this);
        }
      }
    }

    var dot = home.resolve(".bach");
    if (Files.isDirectory(dot)) {
      var out = new PrintWriter(System.out, true);
      var err = new PrintWriter(System.err, true);
      var javac = ToolProvider.findFirst("javac").orElseThrow();
      var bin = dot.resolve("bin");
      var name = "Configuration";
      var configurationJava = dot.resolve(name + ".java");
      if (Files.exists(configurationJava)) {
        javac.run(out, err, "-d", bin.toString(), configurationJava.toString());
      }
      try {
        var parent = Configuration.class.getClassLoader();
        var loader = URLClassLoader.newInstance(new URL[] {bin.toUri().toURL()}, parent);
        var configuration = loader.loadClass(name).getConstructor().newInstance();
        var interfaces = new Class[] {Configuration.class};
        var handler = new ConfigurationInvocationHandler(configuration);
        return (Configuration) Proxy.newProxyInstance(loader, interfaces, handler);
      } catch (ClassNotFoundException e) {
        // ignore "missing" custom configuration class
      } catch (Exception e) {
        throw new Error("Loading custom configuration failed: " + configurationJava.toUri(), e);
      }
    }
    return new Configuration() {};
  }

  class DefaultConfiguration implements Configuration {

    private final Path homeDirectory;
    private final Path workspaceDirectory;
    private final List<Path> libraryPaths;
    private final List<Path> sourceDirectories;

    private DefaultConfiguration(
        Path homeDirectory,
        Path workspaceDirectory,
        List<Path> libraryPaths,
        List<Path> sourceDirectories) {
      this.homeDirectory = homeDirectory;
      this.workspaceDirectory = workspaceDirectory;
      this.libraryPaths = Util.requireNonEmpty(libraryPaths, "library paths");
      this.sourceDirectories = Util.requireNonEmpty(sourceDirectories, "source directories");
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
    public List<Path> getLibraryPaths() {
      return libraryPaths;
    }

    @Override
    public List<Path> getSourceDirectories() {
      return sourceDirectories;
    }

    @Override
    public String toString() {
      return "Configuration [" + String.join(", ", toStrings(this)) + "]";
    }
  }

  class ValidationError extends AssertionError {
    private ValidationError(String expected, Object hint) {
      super(String.format("expected that %s: %s", expected, hint));
    }
  }

  static List<String> toStrings(Configuration configuration) {
    var home = configuration.getHomeDirectory();
    return List.of(
        String.format("home = '%s' -> %s", home, home.toUri()),
        String.format("workspace = '%s'", configuration.getWorkspaceDirectory()),
        String.format("library paths = %s", configuration.getLibraryPaths()),
        String.format("source directories = %s", configuration.getSourceDirectories()));
  }

  static void validate(Configuration configuration) {
    var home = configuration.getHomeDirectory();
    validateDirectory(home);
    if (Util.list(home, Files::isDirectory).size() == 0)
      throw new ValidationError("home contains a directory", home.toUri());
    var work = configuration.getWorkspaceDirectory();
    if (Files.exists(work)) {
      validateDirectory(work);
      if (!work.toFile().canWrite()) throw new ValidationError("bin is writable: %s", work.toUri());
    } else {
      var parentOfBin = work.toAbsolutePath().getParent();
      if (parentOfBin != null && !parentOfBin.toFile().canWrite())
        throw new ValidationError("parent of work is writable", parentOfBin.toUri());
    }
    validateDirectoryIfExists(configuration.getLibraryDirectory());
    configuration.getSourceDirectories().forEach(Configuration::validateDirectory);
  }

  static void validateDirectoryIfExists(Path path) {
    if (Files.exists(path)) validateDirectory(path);
  }

  static void validateDirectory(Path path) {
    if (!Files.isDirectory(path)) throw new ValidationError("path is a directory", path.toUri());
  }
}
