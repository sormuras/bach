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
  private Collection<String> patternsWhereToFindSourceFiles;
  private Map<String, Collection<Path>> pathsWhereToFindSourceFiles;
  private Map<String, Collection<Path>> pathsWhereToFindMoreAssetsPerModule;
  private Collection<Path> pathsWhereToFindApplicationModules;
  private String characterEncodingUsedBySourceFiles;
  private int compileForVirtualMachineVersion;
  private boolean enablePreviewLanguageFeatures;
  private boolean generateMetadataForMethodParameters;
  private boolean outputMessagesAboutWhatTheCompilerIsDoing;
  private boolean outputSourceLocationsOfDeprecatedUsages;
  private boolean terminateCompilationIfWarningsOccur;
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

    var patterns = getPatternsWhereToFindSourceFiles();
    if (assigned(patterns)) arguments.add("--module-source-path", joinPaths(patterns));

    var specific = getPathsWhereToFindSourceFiles();
    if (assigned(specific))
      for (var entry : specific.entrySet())
        arguments.add("--module-source-path", entry.getKey() + '=' + join(entry.getValue()));

    var patches = getPathsWhereToFindMoreAssetsPerModule();
    if (assigned(patches))
      for (var patch : patches.entrySet())
        arguments.add("--patch-module", patch.getKey() + '=' + join(patch.getValue()));

    var modulePath = getPathsWhereToFindApplicationModules();
    if (assigned(modulePath)) arguments.add("--module-path", join(modulePath));

    var encoding = getCharacterEncodingUsedBySourceFiles();
    if (assigned(encoding)) arguments.add("-encoding", encoding);

    var release = getCompileForVirtualMachineVersion();
    if (assigned(release)) arguments.add("--release", release);

    if (isEnablePreviewLanguageFeatures()) arguments.add("--enable-preview");

    if (isGenerateMetadataForMethodParameters()) arguments.add("-parameters");

    if (isOutputSourceLocationsOfDeprecatedUsages()) arguments.add("-deprecation");

    if (isOutputMessagesAboutWhatTheCompilerIsDoing()) arguments.add("-verbose");

    if (isTerminateCompilationIfWarningsOccur()) arguments.add("-Werror");

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

  public Javac setVersionOfModulesThatAreBeingCompiled(
      Version versionOfModulesThatAreBeingCompiled) {
    this.versionOfModulesThatAreBeingCompiled = versionOfModulesThatAreBeingCompiled;
    return this;
  }

  public Collection<String> getPatternsWhereToFindSourceFiles() {
    return patternsWhereToFindSourceFiles;
  }

  public Javac setPatternsWhereToFindSourceFiles(Collection<String> patterns) {
    this.patternsWhereToFindSourceFiles = patterns;
    return this;
  }

  public Map<String, Collection<Path>> getPathsWhereToFindSourceFiles() {
    return pathsWhereToFindSourceFiles;
  }

  public Javac setPathsWhereToFindSourceFiles(Map<String, Collection<Path>> map) {
    this.pathsWhereToFindSourceFiles = map;
    return this;
  }

  public Map<String, Collection<Path>> getPathsWhereToFindMoreAssetsPerModule() {
    return pathsWhereToFindMoreAssetsPerModule;
  }

  public Javac setPathsWhereToFindMoreAssetsPerModule(Map<String, Collection<Path>> map) {
    this.pathsWhereToFindMoreAssetsPerModule = map;
    return this;
  }

  public Collection<Path> getPathsWhereToFindApplicationModules() {
    return pathsWhereToFindApplicationModules;
  }

  public Javac setPathsWhereToFindApplicationModules(
      Collection<Path> pathsWhereToFindApplicationModules) {
    this.pathsWhereToFindApplicationModules = pathsWhereToFindApplicationModules;
    return this;
  }

  public String getCharacterEncodingUsedBySourceFiles() {
    return characterEncodingUsedBySourceFiles;
  }

  public Javac setCharacterEncodingUsedBySourceFiles(String encoding) {
    this.characterEncodingUsedBySourceFiles = encoding;
    return this;
  }

  public int getCompileForVirtualMachineVersion() {
    return compileForVirtualMachineVersion;
  }

  public Javac setCompileForVirtualMachineVersion(int release) {
    this.compileForVirtualMachineVersion = release;
    return this;
  }

  public boolean isEnablePreviewLanguageFeatures() {
    return enablePreviewLanguageFeatures;
  }

  public Javac setEnablePreviewLanguageFeatures(boolean preview) {
    this.enablePreviewLanguageFeatures = preview;
    return this;
  }

  public boolean isGenerateMetadataForMethodParameters() {
    return generateMetadataForMethodParameters;
  }

  public Javac setGenerateMetadataForMethodParameters(boolean parameters) {
    this.generateMetadataForMethodParameters = parameters;
    return this;
  }

  public boolean isOutputMessagesAboutWhatTheCompilerIsDoing() {
    return outputMessagesAboutWhatTheCompilerIsDoing;
  }

  public Javac setOutputMessagesAboutWhatTheCompilerIsDoing(boolean verbose) {
    this.outputMessagesAboutWhatTheCompilerIsDoing = verbose;
    return this;
  }

  public boolean isOutputSourceLocationsOfDeprecatedUsages() {
    return outputSourceLocationsOfDeprecatedUsages;
  }

  public Javac setOutputSourceLocationsOfDeprecatedUsages(boolean deprecation) {
    this.outputSourceLocationsOfDeprecatedUsages = deprecation;
    return this;
  }

  public boolean isTerminateCompilationIfWarningsOccur() {
    return terminateCompilationIfWarningsOccur;
  }

  public Javac setTerminateCompilationIfWarningsOccur(boolean error) {
    this.terminateCompilationIfWarningsOccur = error;
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
