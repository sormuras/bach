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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** A call to {@code javadoc}, the Java API documentation generating tool. */
public /*static*/ class Javadoc extends AbstractTool {

  private Set<String> documentModules;
  private Collection<String> patternsWhereToFindSourceFiles;
  private Map<String, Collection<Path>> pathsWhereToFindSourceFiles;
  private Map<String, Collection<Path>> pathsWhereToFindMoreAssetsPerModule;
  private Collection<Path> pathsWhereToFindApplicationModules;
  private String characterEncodingUsedBySourceFiles;
  private int compileForVirtualMachineVersion;
  private boolean enablePreviewLanguageFeatures;
  private boolean outputMessagesAboutWhatTheCompilerIsDoing;
  private Path destinationDirectory;

  public Javadoc() {
    super("javadoc");
  }

  @Override
  protected void arguments(Arguments arguments) {
    var module = getDocumentModules();
    if (assigned(module)) arguments.add("--module", String.join(",", new TreeSet<>(module)));

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

    if (isOutputMessagesAboutWhatJavadocIsDoing()) arguments.add("-verbose");

    var destination = getDestinationDirectory();
    if (assigned(destination)) arguments.add("-d", destination);
  }

  public Set<String> getDocumentModules() {
    return documentModules;
  }

  public Javadoc setDocumentModules(Set<String> documentModules) {
    this.documentModules = documentModules;
    return this;
  }

  public Collection<String> getPatternsWhereToFindSourceFiles() {
    return patternsWhereToFindSourceFiles;
  }

  public Javadoc setPatternsWhereToFindSourceFiles(Collection<String> patternsWhereToFindSourceFiles) {
    this.patternsWhereToFindSourceFiles = patternsWhereToFindSourceFiles;
    return this;
  }

  public Map<String, Collection<Path>> getPathsWhereToFindSourceFiles() {
    return pathsWhereToFindSourceFiles;
  }

  public Javadoc setPathsWhereToFindSourceFiles(Map<String, Collection<Path>> pathsWhereToFindSourceFiles) {
    this.pathsWhereToFindSourceFiles = pathsWhereToFindSourceFiles;
    return this;
  }

  public Map<String, Collection<Path>> getPathsWhereToFindMoreAssetsPerModule() {
    return pathsWhereToFindMoreAssetsPerModule;
  }

  public Javadoc setPathsWhereToFindMoreAssetsPerModule(Map<String, Collection<Path>> pathsWhereToFindMoreAssetsPerModule) {
    this.pathsWhereToFindMoreAssetsPerModule = pathsWhereToFindMoreAssetsPerModule;
    return this;
  }

  public Collection<Path> getPathsWhereToFindApplicationModules() {
    return pathsWhereToFindApplicationModules;
  }

  public Javadoc setPathsWhereToFindApplicationModules(Collection<Path> pathsWhereToFindApplicationModules) {
    this.pathsWhereToFindApplicationModules = pathsWhereToFindApplicationModules;
    return this;
  }

  public String getCharacterEncodingUsedBySourceFiles() {
    return characterEncodingUsedBySourceFiles;
  }

  public Javadoc setCharacterEncodingUsedBySourceFiles(String characterEncodingUsedBySourceFiles) {
    this.characterEncodingUsedBySourceFiles = characterEncodingUsedBySourceFiles;
    return this;
  }

  public int getCompileForVirtualMachineVersion() {
    return compileForVirtualMachineVersion;
  }

  public Javadoc setCompileForVirtualMachineVersion(int compileForVirtualMachineVersion) {
    this.compileForVirtualMachineVersion = compileForVirtualMachineVersion;
    return this;
  }

  public boolean isEnablePreviewLanguageFeatures() {
    return enablePreviewLanguageFeatures;
  }

  public Javadoc setEnablePreviewLanguageFeatures(boolean enablePreviewLanguageFeatures) {
    this.enablePreviewLanguageFeatures = enablePreviewLanguageFeatures;
    return this;
  }

  public boolean isOutputMessagesAboutWhatJavadocIsDoing() {
    return outputMessagesAboutWhatTheCompilerIsDoing;
  }

  public Javadoc setOutputMessagesAboutWhatJavadocIsDoing(boolean outputMessagesAboutWhatTheCompilerIsDoing) {
    this.outputMessagesAboutWhatTheCompilerIsDoing = outputMessagesAboutWhatTheCompilerIsDoing;
    return this;
  }

  public Path getDestinationDirectory() {
    return destinationDirectory;
  }

  public Javadoc setDestinationDirectory(Path destinationDirectory) {
    this.destinationDirectory = destinationDirectory;
    return this;
  }
}
