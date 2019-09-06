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
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/*BODY*/
public /*STATIC*/ class Configuration {

  private final Path home;
  private final Properties properties;

  public Configuration() {
    this(Path.of(Property.PROJECT_BASE.get()));
  }

  public Configuration(Path home) {
    this.home = home;
    var file = ".bach/.properties";
    var USER = Util.load(new Properties(), Path.of(System.getProperty("user.home")).resolve(file));
    this.properties = Util.load(USER, home.resolve(System.getProperty("properties", file)));
  }

  private String get(Property property) {
    return get(property, property.get());
  }

  private String get(Property property, String defaultValue) {
    return properties.getProperty(property.getKey(), defaultValue);
  }

  List<String> lines(Property property) {
    return get(property).lines().collect(Collectors.toList());
  }

  public boolean debug() {
    return get(Property.DEBUG).equalsIgnoreCase("true");
  }

  public String getProjectName() {
    var name = Property.PROJECT_NAME;
    var dir = getHomeDirectory().toAbsolutePath().getFileName();
    return get(name, dir != null ? dir.toString() : name.getDefaultValue());
  }

  public Version getProjectVersion() {
    return Version.parse(get(Property.PROJECT_VERSION));
  }

  public Path getHomeDirectory() {
    return home;
  }

  public Path getWorkspaceDirectory() {
    return getHomeDirectory().resolve(get(Property.TARGET_DIRECTORY));
  }

  public Path getLibraryDirectory() {
    return getLibraryPaths().get(0);
  }

  public List<Path> getLibraryPaths() {
    var lib = getHomeDirectory().resolve(get(Property.LIBRARY_DIRECTORY));
    return List.of(lib);
  }

  public List<Path> getSourceDirectories() {
    var src = getHomeDirectory().resolve(get(Property.SOURCE_DIRECTORY));
    return List.of(src);
  }

  /** {@code if (module.equals("foo.bar.baz")) return URI.create("https://<path>/baz-1.3.jar")} */
  public URI getModuleUri(String module) {
    throw new UnmappedModuleException(module);
  }

  /** {@code module.startsWith("foo.bar") -> URI.create("https://dl.bintray.com/foo-bar/maven")} */
  public URI getModuleMavenRepository(@SuppressWarnings("unused") String module) {
    return URI.create(get(Property.MAVEN_REPOSITORY));
  }

  /** {@code if (module.equals("foo.bar.baz")) return "org.foo.bar:foo-baz"} */
  public String getModuleMavenGroupAndArtifact(String module) {
    throw new UnmappedModuleException(module);
  }

  public String getModuleVersion(String module) {
    throw new UnmappedModuleException(module);
  }

  void print(PrintWriter writer) {
    var home = getHomeDirectory();
    writer.printf("  home = '%s' -> %s%n", home, home.toUri());
    writer.printf("  workspace = '%s'%n", getWorkspaceDirectory());
    writer.printf("  library paths = %s%n", getLibraryPaths());
    writer.printf("  source directories = %s%n", getSourceDirectories());
    writer.printf("  project name = %s%n", getProjectName());
    writer.printf("  project version = %s%n", getProjectVersion());
  }

  static class UnmappedModuleException extends RuntimeException {
    private static final long serialVersionUID = 0;

    UnmappedModuleException(String module) {
      super("Module " + module + " is not mapped");
    }
  }
}
