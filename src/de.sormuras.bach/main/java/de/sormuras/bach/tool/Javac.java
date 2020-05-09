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

package de.sormuras.bach.tool;

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** A call to {@code javac}, the Java compiler. */
public /*static*/ class Javac extends AbstractTool {

  private Set<String> compileModulesCheckingTimestamps;
  private Version versionOfModulesThatAreBeingCompiled;
  private Map<String, Collection<Path>> pathsWhereToFindSourceFiles;
  private Collection<String> patternsWhereToFindSourceFiles;
  private Collection<Path> pathsWhereToFindApplicationModules;
  private Path destinationDirectory;

  public Javac() {
    super("javac");
  }

  @Override
  protected void arguments(Arguments arguments) {
    var module = getCompileModulesCheckingTimestamps();
    if (assigned(module)) arguments.add("--module", String.join(",", new TreeSet<>(module)));

    var version = getVersionOfModulesThatAreBeingCompiled();
    if (assigned(version)) arguments.add("--module-version", version);

    var specific = getPathsWhereToFindSourceFiles();
    if (assigned(specific))
      for (var entry : specific.entrySet())
        arguments.add("--module-source-path", entry.getKey() + '=' + join(entry.getValue()));

    var patterns = getPatternsWhereToFindSourceFiles();
    if (assigned(patterns)) arguments.add("--module-source-path", joinPaths(patterns));

    var modulePath = getPathsWhereToFindApplicationModules();
    if (assigned(modulePath)) arguments.add("--module-path", join(modulePath));

    var destination = getDestinationDirectory();
    if (assigned(destination)) arguments.add("-d", destination);
  }

  public Set<String> getCompileModulesCheckingTimestamps() {
    return compileModulesCheckingTimestamps;
  }

  public Javac setCompileModulesCheckingTimestamps(Set<String> moduleNames) {
    this.compileModulesCheckingTimestamps = moduleNames;
    return this;
  }

  public Version getVersionOfModulesThatAreBeingCompiled() {
    return versionOfModulesThatAreBeingCompiled;
  }

  public Javac setVersionOfModulesThatAreBeingCompiled(Version versionOfModulesThatAreBeingCompiled) {
    this.versionOfModulesThatAreBeingCompiled = versionOfModulesThatAreBeingCompiled;
    return this;
  }

  public Map<String, Collection<Path>> getPathsWhereToFindSourceFiles() {
    return pathsWhereToFindSourceFiles;
  }

  public Javac setPathsWhereToFindSourceFiles(Map<String, Collection<Path>> map) {
    this.pathsWhereToFindSourceFiles = map;
    return this;
  }

  public Collection<String> getPatternsWhereToFindSourceFiles() {
    return patternsWhereToFindSourceFiles;
  }

  public Javac setPatternsWhereToFindSourceFiles(Collection<String> patterns) {
    this.patternsWhereToFindSourceFiles = patterns;
    return this;
  }

  public Collection<Path> getPathsWhereToFindApplicationModules() {
    return pathsWhereToFindApplicationModules;
  }

  public Javac setPathsWhereToFindApplicationModules(Collection<Path> pathsWhereToFindApplicationModules) {
    this.pathsWhereToFindApplicationModules = pathsWhereToFindApplicationModules;
    return this;
  }

  public Path getDestinationDirectory() {
    return destinationDirectory;
  }

  public Javac setDestinationDirectory(Path destinationDirectory) {
    this.destinationDirectory = destinationDirectory;
    return this;
  }
}
