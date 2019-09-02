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

package it;

import de.sormuras.bach.Configuration;
import java.nio.file.Path;
import java.util.List;

class ConfigurationBean implements Configuration {

  private final Path homeDirectory;
  private Path workspaceDirectory;
  private List<Path> libraryPaths;
  private List<Path> sourceDirectories;

  ConfigurationBean(Path homeDirectory) {
    this.homeDirectory = homeDirectory;
    setWorkspaceDirectory(Configuration.super.getWorkspaceDirectory());
    setLibraryPaths(Configuration.super.getLibraryPaths());
    setSourceDirectories(Configuration.super.getSourceDirectories());
    Configuration.validate(this);
  }

  @Override
  public Path getHomeDirectory() {
    return homeDirectory;
  }

  @Override
  public Path getWorkspaceDirectory() {
    return workspaceDirectory;
  }

  ConfigurationBean setWorkspaceDirectory(Path workspaceDirectory) {
    this.workspaceDirectory = resolve(workspaceDirectory, "workspace directory");
    return this;
  }

  @Override
  public List<Path> getLibraryPaths() {
    return libraryPaths;
  }

  ConfigurationBean setLibraryPaths(List<Path> libraryPaths) {
    this.libraryPaths = resolve(libraryPaths, "library paths");
    return this;
  }

  @Override
  public List<Path> getSourceDirectories() {
    return sourceDirectories;
  }

  ConfigurationBean setSourceDirectories(List<Path> sourceDirectories) {
    this.sourceDirectories = resolve(sourceDirectories, "source directories");
    return this;
  }
}
