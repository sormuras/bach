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

import de.sormuras.bach.Tool;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Mutable options collection for {@code javac}. */
public /*static*/ class JavaCompiler extends Tool {

  private List<String> compileModulesCheckingTimestamps;
  private Version versionOfModulesThatAreBeingCompiled;
  private List<Path> pathsWhereToFindSourceFilesForModules;
  private List<Path> pathsWhereToFindApplicationModules;
  private Map<String, List<Path>> pathsWhereToFindMoreAssetsPerModule;
  private String characterEncodingUsedBySourceFiles;
  private int compileForVirtualMachineVersion;
  private boolean enablePreviewLanguageFeatures;
  private boolean generateMetadataForMethodParameters;
  private boolean outputMessagesAboutWhatTheCompilerIsDoing;
  private boolean outputSourceLocationsOfDeprecatedUsages;
  private boolean terminateCompilationIfWarningsOccur;
  private Path destinationDirectory;

  public JavaCompiler(Object... arguments) {
    super("javac", arguments);
  }

  @Override
  public List<String> args() {
    var tool = new Tool("<local>");
    super.args().forEach(tool::add);

    var module = getCompileModulesCheckingTimestamps();
    if (isAssigned(module)) tool.add("--module", String.join(",", module));

    var moduleVersion = getVersionOfModulesThatAreBeingCompiled();
    if (isAssigned(moduleVersion)) tool.add("--module-version", moduleVersion);

    var moduleSourcePath = getPathsWhereToFindSourceFilesForModules();
    if (isAssigned(moduleSourcePath)) tool.add("--module-source-path", join(moduleSourcePath));

    var modulePath = getPathsWhereToFindApplicationModules();
    if (isAssigned(modulePath)) tool.add("--module-path", join(modulePath));

    var modulePatches = getPathsWhereToFindMoreAssetsPerModule();
    if (isAssigned(modulePatches))
      for (var patch : modulePatches.entrySet())
        tool.add("--patch-module", patch.getKey() + '=' + join(patch.getValue()));

    var release = getCompileForVirtualMachineVersion();
    if (isAssigned(release)) tool.add("--release", release);

    if (isEnablePreviewLanguageFeatures()) tool.add("--enable-preview");

    if (isGenerateMetadataForMethodParameters()) tool.add("-parameters");

    if (isOutputSourceLocationsOfDeprecatedUsages()) tool.add("-deprecation");

    if (isOutputMessagesAboutWhatTheCompilerIsDoing()) tool.add("-verbose");

    if (isTerminateCompilationIfWarningsOccur()) tool.add("-Werror");

    var encoding = getCharacterEncodingUsedBySourceFiles();
    if (isAssigned(encoding)) tool.add("-encoding", encoding);

    var destination = getDestinationDirectory();
    if (isAssigned(destination)) tool.add("-d", destination);

    return tool.args();
  }

  public JavaCompiler setCompileModulesCheckingTimestamps(List<String> modules) {
    this.compileModulesCheckingTimestamps = modules;
    return this;
  }

  public List<String> getCompileModulesCheckingTimestamps() {
    return compileModulesCheckingTimestamps;
  }

  public JavaCompiler setPathsWhereToFindSourceFilesForModules(List<Path> moduleSourcePath) {
    this.pathsWhereToFindSourceFilesForModules = moduleSourcePath;
    return this;
  }

  public List<Path> getPathsWhereToFindSourceFilesForModules() {
    return pathsWhereToFindSourceFilesForModules;
  }

  public JavaCompiler setPathsWhereToFindApplicationModules(List<Path> modulePath) {
    this.pathsWhereToFindApplicationModules = modulePath;
    return this;
  }

  public List<Path> getPathsWhereToFindApplicationModules() {
    return pathsWhereToFindApplicationModules;
  }

  public JavaCompiler setDestinationDirectory(Path destinationDirectory) {
    this.destinationDirectory = destinationDirectory;
    return this;
  }

  public Path getDestinationDirectory() {
    return destinationDirectory;
  }

  public JavaCompiler setPathsWhereToFindMoreAssetsPerModule(Map<String, List<Path>> patches) {
    this.pathsWhereToFindMoreAssetsPerModule = patches;
    return this;
  }

  public Map<String, List<Path>> getPathsWhereToFindMoreAssetsPerModule() {
    return pathsWhereToFindMoreAssetsPerModule;
  }

  public JavaCompiler setCharacterEncodingUsedBySourceFiles(String encoding) {
    this.characterEncodingUsedBySourceFiles = encoding;
    return this;
  }

  public String getCharacterEncodingUsedBySourceFiles() {
    return characterEncodingUsedBySourceFiles;
  }

  public JavaCompiler setCompileForVirtualMachineVersion(int release) {
    this.compileForVirtualMachineVersion = release;
    return this;
  }

  public int getCompileForVirtualMachineVersion() {
    return compileForVirtualMachineVersion;
  }

  public JavaCompiler setEnablePreviewLanguageFeatures(boolean enablePreview) {
    this.enablePreviewLanguageFeatures = enablePreview;
    return this;
  }

  public boolean isEnablePreviewLanguageFeatures() {
    return enablePreviewLanguageFeatures;
  }

  public JavaCompiler setGenerateMetadataForMethodParameters(boolean parameters) {
    this.generateMetadataForMethodParameters = parameters;
    return this;
  }

  public boolean isGenerateMetadataForMethodParameters() {
    return generateMetadataForMethodParameters;
  }

  public JavaCompiler setOutputSourceLocationsOfDeprecatedUsages(boolean deprecation) {
    this.outputSourceLocationsOfDeprecatedUsages = deprecation;
    return this;
  }

  public boolean isOutputSourceLocationsOfDeprecatedUsages() {
    return outputSourceLocationsOfDeprecatedUsages;
  }

  public JavaCompiler setOutputMessagesAboutWhatTheCompilerIsDoing(boolean verbose) {
    this.outputMessagesAboutWhatTheCompilerIsDoing = verbose;
    return this;
  }

  public boolean isOutputMessagesAboutWhatTheCompilerIsDoing() {
    return outputMessagesAboutWhatTheCompilerIsDoing;
  }

  public JavaCompiler setTerminateCompilationIfWarningsOccur(boolean warningsAreErrors) {
    this.terminateCompilationIfWarningsOccur = warningsAreErrors;
    return this;
  }

  public boolean isTerminateCompilationIfWarningsOccur() {
    return terminateCompilationIfWarningsOccur;
  }

  public JavaCompiler setVersionOfModulesThatAreBeingCompiled(Version moduleVersion) {
    this.versionOfModulesThatAreBeingCompiled = moduleVersion;
    return this;
  }

  public Version getVersionOfModulesThatAreBeingCompiled() {
    return versionOfModulesThatAreBeingCompiled;
  }
}
